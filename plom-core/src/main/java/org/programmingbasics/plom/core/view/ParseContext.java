package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class ParseContext
{
  
  // For figuring out which tokens should be used for predicting what
  // the next token should be at the cursor position 
  public static class ParseContextForCursor
  {
    public Symbol baseContext;
    public List<Token> tokens = new ArrayList<>();
  }

  public static ParseContextForCursor findPredictiveParseContextForStatements(StatementContainer statements, CodePosition pos, int level)
  {
    return findPredictiveParseContextForStatements(Symbol.FullStatement, statements, pos, level);
  }
  
  public static ParseContextForCursor findPredictiveParseContextForStatements(Symbol baseContext, StatementContainer statements, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return findPredictiveParseContextForLine(line, baseContext, pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = baseContext;
    return toReturn;
  }
  
  static ParseContextForCursor findPredictiveParseContextForLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenPredictiveParseContext(), pos, level + 1, null);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = baseContext;
    toReturn.tokens.addAll(line.tokens.subList(0, pos.getOffset(level)));
    return toReturn;
  }
  
  static class TokenPredictiveParseContext extends RecurseIntoCompoundToken<ParseContextForCursor, Void, RuntimeException>
  {
    @Override
    ParseContextForCursor handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, Void param)
    {
      if (originalToken.getType() == Symbol.COMPOUND_FOR)
        return findPredictiveParseContextForLine(exprContainer, Symbol.ForExpressionOnly, pos, level);
      else if (originalToken.getType() == Symbol.FunctionLiteral)
        return findPredictiveParseContextForLine(exprContainer, Symbol.FunctionLiteralExpressionOnly, pos, level);
      else
        return findPredictiveParseContextForLine(exprContainer, Symbol.ExpressionOnly, pos, level);
    }
    @Override
    ParseContextForCursor handleStatementContainer(TokenWithSymbol originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, Void param)
    {
      return findPredictiveParseContextForStatements(blockContainer, pos, level);
    }
    @Override
    public ParseContextForCursor visitParameterToken(ParameterToken token,
        CodePosition pos, Integer level, Void param) throws RuntimeException
    {
      if (pos.getOffset(level) != CodePosition.PARAMTOK_POS_EXPRS)
        return super.visitParameterToken(token, pos, level, param);

      switch (token.getType())
      {
      case AtType:
      case FunctionTypeName:
        return findPredictiveParseContextForLine(token.parameters.get(pos.getOffset(level + 1)), Symbol.ParameterFieldOptionalNameOnly, pos, level+2);
      default:
        return super.visitParameterToken(token, pos, level, param);
      }
    }
  }
}
