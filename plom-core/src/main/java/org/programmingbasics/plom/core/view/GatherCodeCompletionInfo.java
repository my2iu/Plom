package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
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
    if (end < statements.statements.size() && pos.hasOffset(level + 1));
    {
      TokenContainer line = statements.statements.get(end - 1);
      fromLine(line, Symbol.StatementOrEmpty, context, pos, level + 1);
    }
    return;
  }
  
  static void parseWholeLine(TokenContainer line, Symbol baseContext, CodeCompletionContext context)
  {
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
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
}
