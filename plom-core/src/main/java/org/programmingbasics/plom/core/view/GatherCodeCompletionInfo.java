package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.CodeSuggestExpressionTyper;

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
      // If we're entering a block of a for loop, we need to find
      // any variables defined in the expression part first
      token.visit(new Token.TokenVisitor1<Void, CodeCompletionContext>() {
        @Override public Void visitSimpleToken(SimpleToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitParameterToken(ParameterToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitParameterOneBlockToken(Token.ParameterOneBlockToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitWideToken(WideToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitOneBlockToken(OneBlockToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, CodeCompletionContext param1) 
        {
          if (token.getType() != Symbol.COMPOUND_FOR) return null;
          if (pos.getOffset(level + 1) != CodeRenderer.EXPRBLOCK_POS_BLOCK) return null;
          parseWholeLine(token.expression, Symbol.ForExpression, context);
          return null; 
        }
      }, context); 
      // Now enter into the token for the current code position
      token.visit(new RecurseIntoCompoundToken<Void, CodeCompletionContext, RuntimeException>() {
        @Override Void handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
            CodePosition pos, int level, CodeCompletionContext context)
        {
          if (originalToken.getType() == Symbol.COMPOUND_FOR)
            fromLine(exprContainer, Symbol.ForExpression, context, pos, level);
          else
            fromLine(exprContainer, Symbol.Expression, context, pos, level);
          return null;
        }
        @Override Void handleStatementContainer(TokenWithSymbol originalToken,
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
        AstNode declareIdentifier = node.children.get(1);
        AstNode varType = node.children.get(2);
        handleVariableDeclaration(context, declareIdentifier, varType);
        return true;
      })
    .add(Rule.ForExpression_DotDeclareIdentifier_VarType_In_Expression, (triggers, node, context, param) -> {
      if (node.children.size() < 4)
        return true;
      AstNode declareIdentifier = node.children.get(0);
      AstNode varType = node.children.get(1);
      handleVariableDeclaration(context, declareIdentifier, varType);
      return true;
      });
  }
  static void handleVariableDeclaration(CodeCompletionContext context,
      AstNode declareIdentifier, AstNode varType)
  {
    if (declareIdentifier == null || !declareIdentifier.matchesRule(Rule.DotDeclareIdentifier_DotVariable))
      return;
    String name = ((Token.ParameterToken)declareIdentifier.children.get(0).token).getLookupName();
    if (varType == null)
      return;
    Type type = CodeSuggestExpressionTyper.gatherTypeInfoNoFail(varType, context);

    if (type == null) type = context.coreTypes().getVoidType();
//        Value val = context.coreTypes().getNullValue();
//        if (node.children.get(3) == null || !node.children.get(3).matchesRule(Rule.VarAssignment))
//          return true;
    context.currentScope().addVariable(name, type, context.coreTypes().getNullValue());
    return;
  }
}
