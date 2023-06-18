package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class LineForPositionTest extends TestCase
{
  private int getLine(LineNumberTracker lineTracker, StatementContainer code, int ...pos)
  {
    return LineForPosition.inCode(code, CodePosition.fromOffsets(pos), lineTracker);
  }
  
  @Test
  public void testEmptyContainer()
  {
    StatementContainer code = 
        new StatementContainer(
          );
    LineNumberTracker lineTracker = new LineNumberTracker();
    lineTracker.calculateLineNumbersForStatements(code, 1);
    Assert.assertEquals(1, getLine(lineTracker, code, 0));
    Assert.assertEquals(1, getLine(lineTracker, code, 0, 0));
  }

  @Test
  public void testWideTokens()
  {
    StatementContainer code = 
        new StatementContainer(
            new TokenContainer(
                ),
            new TokenContainer(
                new Token.WideToken("hello", Symbol.DUMMY_COMMENT),
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF),
                new Token.OneBlockToken("if", Symbol.COMPOUND_ELSE,
                    new StatementContainer(
                        new TokenContainer(),
                        new TokenContainer())),
                new Token.WideToken("hello", Symbol.DUMMY_COMMENT)
                )
        );
    LineNumberTracker lineTracker = new LineNumberTracker();
    lineTracker.calculateLineNumbersForStatements(code, 1);
    Assert.assertEquals(1, getLine(lineTracker, code, 0, 0));
    Assert.assertEquals(2, getLine(lineTracker, code, 1, 0));
    Assert.assertEquals(3, getLine(lineTracker, code, 1, 1));
    Assert.assertEquals(3, getLine(lineTracker, code, 1, 1, CodeRenderer.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertEquals(4, getLine(lineTracker, code, 1, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 0));
    Assert.assertEquals(5, getLine(lineTracker, code, 1, 2));
    Assert.assertEquals(6, getLine(lineTracker, code, 1, 2, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 0));
    Assert.assertEquals(7, getLine(lineTracker, code, 1, 2, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0));
    Assert.assertEquals(8, getLine(lineTracker, code, 1, 3));
    Assert.assertEquals(9, getLine(lineTracker, code, 1, 4));
  }
}
