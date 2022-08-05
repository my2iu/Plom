package org.programmingbasics.plom.core.view;

import java.util.IdentityHashMap;
import java.util.Map;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

/**
 * Counts how deeply nested tokens are
 */
public class CodeNestingCounter
{
  public Map<Token, Integer> tokenNesting = new IdentityHashMap<>();
  public Map<TokenContainer, Integer> expressionNesting = new IdentityHashMap<>();
  
  public void calculateNestingForStatements(StatementContainer code)
  {
    for (TokenContainer line: code.statements)
    {
      calculateNestingForLine(line);
    }
  }
  
  int calculateNestingForLine(TokenContainer line)
  {
    int maxNesting = 0;
    for (Token tok: line.tokens)
    {
      int nesting = tok.visit(new TokenNestingCounter(this));
      tokenNesting.put(tok, nesting);
      maxNesting = Math.max(maxNesting, nesting);
    }
    expressionNesting.put(line, maxNesting);
    return maxNesting;
  }
  
  static class TokenNestingCounter implements Token.TokenVisitor<Integer>
  {
    CodeNestingCounter nestingCounter;
    TokenNestingCounter(CodeNestingCounter nestingCounter)
    {
      this.nestingCounter = nestingCounter;
    }
    
    @Override public Integer visitSimpleToken(SimpleToken token)
    {
      return 1;
    }

    private int visitTokenWithParameters(Token.TokenWithParameters token)
    {
      int nesting = 0;
      if (!token.getParameters().isEmpty())
        nesting = 1;
      for (TokenContainer expr: token.getParameters())
      {
        nesting = Math.max(nesting, nestingCounter.calculateNestingForLine(expr));
      }
      return nesting + 1;
    }
    
    @Override public Integer visitParameterToken(ParameterToken token)
    {
      int nesting = visitTokenWithParameters(token);
      return nesting;
    }

    @Override public Integer visitParameterOneBlockToken(ParameterOneBlockToken token)
    {
      int nesting = visitTokenWithParameters(token);
      if (token.block != null)
        nestingCounter.calculateNestingForStatements(token.block);
      return nesting;
    }
    
    Integer visitWideToken(WideToken token, TokenContainer exprContainer,
        StatementContainer blockContainer)
    {
      if (exprContainer != null)
        nestingCounter.calculateNestingForLine(exprContainer);
      if (blockContainer != null)
        nestingCounter.calculateNestingForStatements(blockContainer);
      return 1;
    }
    
    @Override public Integer visitWideToken(WideToken token)
    {
      return visitWideToken(token, null, null);
    }

    @Override public Integer visitOneBlockToken(OneBlockToken token)
    {
      return visitWideToken(token, null, token.block);
    }

    @Override public Integer visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token)
    {
      return visitWideToken(token, token.expression, token.block);
    }
  }

}
