package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
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
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return findPredictiveParseContextForLine(line, Symbol.FullStatement, pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = Symbol.FullStatement;
    return toReturn;
  }
  
  static ParseContextForCursor findPredictiveParseContextForLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenPredictiveParseContext(), pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = baseContext;
    toReturn.tokens.addAll(line.tokens.subList(0, pos.getOffset(level)));
    return toReturn;
  }
  
  static class TokenPredictiveParseContext implements Token.TokenVisitor2<ParseContextForCursor, CodePosition, Integer>
  {
    @Override
    public ParseContextForCursor visitSimpleToken(SimpleToken token,
        CodePosition pos, Integer level)
    {
      throw new IllegalArgumentException();
    }
    ParseContextForCursor handleWideToken(
        TokenContainer exprContainer, StatementContainer blockContainer,
        CodePosition pos, int level)
    {
      if (exprContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
      {
        return findPredictiveParseContextForLine(exprContainer, Symbol.ExpressionOnly, pos, level + 1);
      }
      else if (blockContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
      {
        return findPredictiveParseContextForStatements(blockContainer, pos, level + 1);
      }
      throw new IllegalArgumentException();
      
    }
    @Override
    public ParseContextForCursor visitWideToken(WideToken token,
        CodePosition pos, Integer level)
    {
      return handleWideToken(null, null, pos, level);
    }
    @Override
    public ParseContextForCursor visitOneBlockToken(OneBlockToken token,
        CodePosition pos, Integer level)
    {
      return handleWideToken(null, token.block, pos, level);
    }
    @Override
    public ParseContextForCursor visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, CodePosition pos, Integer level)
    {
      return handleWideToken(token.expression, token.block, pos, level);
    }
  }

}
