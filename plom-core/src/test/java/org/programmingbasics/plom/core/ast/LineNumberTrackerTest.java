package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.GetToken;

import junit.framework.TestCase;

public class LineNumberTrackerTest extends TestCase
{
  private Token getToken(StatementContainer code, int...pos)
  {
    return GetToken.inStatements(code, CodePosition.fromOffsets(pos), 0);
  }

  private int getLine(LineNumberTracker lineTracker, Token tok)
  {
    return lineTracker.tokenLine.get(tok).intValue();
  }
  
  private int getLine(LineNumberTracker lineTracker, StatementContainer code, int ...pos)
  {
    return getLine(lineTracker, getToken(code, pos));
  }
  
  @Test
  public void testIf()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                    new Token.SimpleToken("and", Symbol.And),
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("=", Symbol.Gt),
                    new Token.SimpleToken("5", Symbol.Number)
                    ),
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable)
                        )
                    ))
            )
        );

    LineNumberTracker nestingCounter = new LineNumberTracker(); 
    nestingCounter.calculateLineNumbersForStatements(code, 1);

    Assert.assertEquals(1, getLine(nestingCounter, code.statements.get(0).tokens.get(0)));
    Assert.assertEquals(1, getLine(nestingCounter, ((Token.OneExpressionOneBlockToken)code.statements.get(0).tokens.get(0)).expression.tokens.get(0)));
    Assert.assertEquals(2, getLine(nestingCounter, ((Token.OneExpressionOneBlockToken)code.statements.get(0).tokens.get(0)).block.statements.get(0).tokens.get(0)));
  }
}
