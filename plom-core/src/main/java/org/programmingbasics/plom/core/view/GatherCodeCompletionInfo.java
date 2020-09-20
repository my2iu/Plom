package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.CodeSuggestExpressionTyper;
import org.programmingbasics.plom.core.suggestions.CodeSuggestExpressionTyper.GatheredTypeInfo;

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
          parsed.recursiveVisit(CodeSuggestExpressionTyper.lastTypeHandlers, context, null);
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
        node.children.get(2).recursiveVisit(CodeSuggestExpressionTyper.typeParsingHandlers, typeInfo, context);
        Type type = typeInfo.type;
        if (type == null) type = context.coreTypes().getVoidType();
        Value val = context.coreTypes().getNullValue();
        if (node.children.get(3) == null || !node.children.get(3).matchesRule(Rule.VarAssignment))
          return true;
        context.currentScope().addVariable(name, type, context.coreTypes().getNullValue());
        return true;
      });
  }
}
