package org.programmingbasics.plom.core.ast;

import java.util.IdentityHashMap;
import java.util.Map;

import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;

/**
 * Assigns line numbers to some code so that better error 
 * messages can be shown 
 */
public class LineNumberTracker
{
  public Map<Token, Integer> tokenLine = new IdentityHashMap<>();
  public Map<TokenContainer, Integer> expressionLine = new IdentityHashMap<>();
  
  int calculateLineNumbersForStatements(StatementContainer code, int startLine)
  {
    int lineNo = startLine;
    for (TokenContainer line: code.statements)
    {
      lineNo = calculateLineNumbersForLine(line, lineNo) + 1;
    }
    return lineNo;
  }
  
  int calculateLineNumbersForLine(TokenContainer line, int startLine)
  {
    int lineNo = startLine;
    expressionLine.put(line, lineNo);
    for (Token tok: line.tokens)
    {
      tokenLine.put(tok, lineNo);
      lineNo = tok.visit(new TokenLineNumberAssigner(this), lineNo);
    }
    return lineNo;
  }
  
  static class TokenLineNumberAssigner implements Token.TokenVisitor1<Integer, Integer>
  {
    LineNumberTracker lineTracker;
    TokenLineNumberAssigner(LineNumberTracker nestingCounter)
    {
      this.lineTracker = nestingCounter;
    }
    
    @Override public Integer visitSimpleToken(SimpleToken token, Integer startLine)
    {
      return startLine;
    }

    @Override public Integer visitParameterToken(ParameterToken token, Integer startLine)
    {
      for (TokenContainer expr: token.parameters)
      {
        lineTracker.calculateLineNumbersForLine(expr, startLine);
      }
      return startLine;
    }

    Integer visitWideToken(WideToken token, TokenContainer exprContainer,
        StatementContainer blockContainer, int lineNo)
    {
      if (exprContainer != null)
        lineTracker.calculateLineNumbersForLine(exprContainer, lineNo);
      if (blockContainer != null)
        lineNo = lineTracker.calculateLineNumbersForStatements(blockContainer, lineNo + 1);
      return lineNo;
    }
    
    @Override public Integer visitWideToken(WideToken token, Integer startLine)
    {
      return visitWideToken(token, startLine);
    }

    @Override public Integer visitOneBlockToken(OneBlockToken token, Integer startLine)
    {
      return visitWideToken(token, null, token.block, startLine);
    }

    @Override public Integer visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, Integer startLine)
    {
      return visitWideToken(token, token.expression, token.block, startLine);
    }
  }

}
