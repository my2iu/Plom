package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.AstNode.RecursiveWalkerVisitor;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;

public class GatherCodeCompletionInfo
{
  public static void fromStatements(StatementContainer statements, CodeCompletionContext context, CodePosition pos, int level)
  {
    int end = pos.getOffset(level);
    // Parse through each line up to the current position
    for (int n = 0; n < end; n++)
    {
      if (n >= statements.statements.size()) break;
      TokenContainer line = statements.statements.get(n);
      // Parse the whole line and extract type and variable info, but
      // do not recurse into sub-blocks
      parseWholeLine(line, Symbol.StatementOrEmpty, context);
    }
    if (end < statements.statements.size() && pos.hasOffset(level + 1))
    {
      TokenContainer line = statements.statements.get(end);
      fromLine(line, Symbol.StatementOrEmpty, context, pos, level + 1);
    }
    return;
  }
  
  static void parseWholeLine(TokenContainer line, Symbol baseContext, CodeCompletionContext context)
  {
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    parser.setRecurseIntoTokens(false);
    try {
      AstNode parsed = parser.parseToEnd(baseContext);
      parsed.recursiveVisit(statementHandlers, context, null);
    } 
    catch (ParseException e)
    {
      // Ignore errors
    }
    
  }
  
  public static void fromLine(TokenContainer line, Symbol baseContext, CodeCompletionContext context, CodePosition pos, int level)
  {
    if (!pos.hasOffset(level + 1))
    {
      // We're at the last level--for expressions and stuff, so we can
      // dig deeper to get the last type used in the expression
      ParseToAst parser = new ParseToAst(line.tokens.subList(0, pos.getOffset(level)), Symbol.EndStatement, null);
      parser.setErrorOnPrematureEnd(false);
      parser.setRecurseIntoTokens(false);
      try {
        AstNode parsed = parser.parseToEnd(baseContext);
        if (parsed != null)
          parsed.recursiveVisit(lastTypeHandlers, context, null);
      } 
      catch (ParseException e)
      {
        // Ignore errors
        e.printStackTrace();
      }
    }
    else
    {
      // We're inside another token that might contain statements, so we'll
      // need to recurse into there.
      Token token = line.tokens.get(pos.getOffset(level));
      token.visit(new RecurseIntoCompoundToken<Void, CodeCompletionContext>() {
        @Override Void handleExpression(Token originalToken, TokenContainer exprContainer,
            CodePosition pos, int level, CodeCompletionContext context)
        {
          fromLine(exprContainer, Symbol.Expression, context, pos, level);
          return null;
        }
        @Override Void handleStatementContainer(Token originalToken,
            StatementContainer blockContainer, CodePosition pos, int level,
            CodeCompletionContext context)
        {
          fromStatements(blockContainer, context, pos, level);
          return null;
        }
      }, pos, level + 1, context);
    }
    
//    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
//    {
//      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, null);
//    }
//    if (pos.getOffset(level) < line.tokens.size())
//      return line.tokens.get(pos.getOffset(level));
//    return null;
  }
  
  // When parsing type information, we need a structure for stashing
  // that type info in order to return it
  static class GatheredTypeInfo
  {
    Type type;
  }
  static AstNode.VisitorTriggers<GatheredTypeInfo, CodeCompletionContext, RuntimeException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredTypeInfo, CodeCompletionContext, RuntimeException>()
      .add(Rule.DotType_DotVariable, (triggers, node, typesToReturn, context) -> {
        // Just pass through
        node.recursiveVisitChildren(triggers, typesToReturn, context);
        return true;
      })
      .add(Rule.DotVariable, (triggers, node, typesToReturn, context) -> {
        Type t = new Type(((Token.ParameterToken)node.token).getLookupName());
        typesToReturn.type = t;
        return true;
      });

  static AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException>();
  static {
    statementHandlers
      .add(Rule.VarStatement_Var_DotDeclareIdentifier_VarType_VarAssignment, (triggers, node, context, param) -> {
        if (node.children.size() < 4)
          return true;
        if (node.children.get(1) == null || !node.children.get(1).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
          return true;
        String name = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
        GatheredTypeInfo typeInfo = new GatheredTypeInfo();
        if (node.children.get(2) == null)
          return true;
        node.children.get(2).recursiveVisit(typeParsingHandlers, typeInfo, context);
        Type type = typeInfo.type;
        if (type == null) type = Type.VOID;
        Value val = Value.NULL;
        if (node.children.get(3) == null || !node.children.get(3).matchesRule(Rule.VarAssignment))
          return true;
        context.currentScope().addVariable(name, type, Value.NULL);
        return true;
      });
  }
  
  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> clearLastUsedType = (triggers, node, context, param) -> {
    context.clearLastTypeUsed();
    return true;
  };
  static interface BinaryTypeHandler
  {
    public Type apply(Type left, Type right);
  }
//  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> createBinaryTypeHandler(BinaryTypeHandler handler) {
//    return (triggers, node, context, param) -> {
//      if (node.children.get(0) == null)
//        return true;
//      context.clearLastTypeUsed();
//      if (node.children.get(1) == null)
//        return true;
//      node.children.get(1).recursiveVisit(triggers, context, param);
//      Type left = context.popType();
//      Type right = context.popType();
//      if (right == null)
//        context.pushType(null);
//      context.pushType(handler.apply(left, right));
//      context.setLastTypeUsed(right);
//      if (node.children.get(2) != null)
//        node.children.get(2).recursiveVisit(triggers, context, param);
//      return true;
//    }; 
//  }
  
  static AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException> lastTypeHandlers = new AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException>();
  static {
    lastTypeHandlers
      .add(Rule.DotVariable, (triggers, node, context, param) -> {
        Type t = context.currentScope().lookupType(((Token.ParameterToken)node.token).getLookupName());
        context.pushType(t);
        context.setLastTypeUsed(t);
        return true;
      })
      .add(Rule.Number, (triggers, node, context, param) -> {
        context.pushType(Type.NUMBER);
        context.setLastTypeUsed(Type.NUMBER);
        return true;
      })
      .add(Rule.String, (triggers, node, context, param) -> {
        context.pushType(Type.STRING);
        context.setLastTypeUsed(Type.STRING);
        return true;
      })
      .add(Rule.Assignment, clearLastUsedType)
      .add(Rule.Plus, clearLastUsedType)
      .add(Rule.Minus, clearLastUsedType)
      .add(Rule.Multiply, clearLastUsedType)
      .add(Rule.Divide, clearLastUsedType)
      .add(Rule.Eq, clearLastUsedType)
      .add(Rule.Ne, clearLastUsedType)
      .add(Rule.Gt, clearLastUsedType)
      .add(Rule.Ge, clearLastUsedType)
      .add(Rule.Lt, clearLastUsedType)
      .add(Rule.Le, clearLastUsedType)
      .add(Rule.And, clearLastUsedType)
      .add(Rule.Or, clearLastUsedType)
//      .add(Rule.AssignmentExpressionMore_Assignment_Expression, 
//          createBinaryTypeHandler((left, right) -> {
//            return right;
//          })
//      )
//      .add(Rule.AdditiveExpressionMore_Plus_MultiplicativeExpression_AdditiveExpressionMore,
//          createBinaryTypeHandler((left, right) -> {
//            if (Type.NUMBER.equals(left) && Type.NUMBER.equals(right))
//              return Type.NUMBER;
//            else if (Type.STRING.equals(left) && Type.STRING.equals(right))
//              return Type.STRING;
//            else
//              return Type.VOID;
//          })
//      )
//      .add(Rule.AdditiveExpressionMore_Minus_MultiplicativeExpression_AdditiveExpressionMore,
//          createBinaryTypeHandler((left, right) -> {
//            if (Type.NUMBER.equals(left) && Type.NUMBER.equals(right))
//              return Type.NUMBER;
//            else
//              return Type.VOID;
//          })
//      )
//      .add(Rule.MultiplicativeExpressionMore_Multiply_MemberExpression_MultiplicativeExpressionMore,
//          createBinaryTypeHandler((left, right) -> {
//            if (Type.NUMBER.equals(left) && Type.NUMBER.equals(right))
//              return Type.NUMBER;
//            else
//              return Type.VOID;
//          })
//      )
//      .add(Rule.MultiplicativeExpressionMore_Divide_MemberExpression_MultiplicativeExpressionMore,
//          createBinaryTypeHandler((left, right) -> {
//            if (Type.NUMBER.equals(left) && Type.NUMBER.equals(right))
//              return Type.NUMBER;
//            else
//              return Type.VOID;
//          })
//      )

      ;
  }

}