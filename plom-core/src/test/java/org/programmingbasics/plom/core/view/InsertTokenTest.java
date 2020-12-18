package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;


public class InsertTokenTest extends TestCase
{
  @Test
  public void testInsertEmptyContainer()
  {
    StatementContainer container = new StatementContainer();
    CodePosition pos = new CodePosition();
    InsertToken.insertTokenIntoStatementContainer(container, new Token.SimpleToken("1", Symbol.Number), pos, 0, false);
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer(new Token.SimpleToken("1", Symbol.Number))
        ), 
        container);
//    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
  }
  
  @Test
  public void testInsertAtEndOfLine()
  {
    StatementContainer container = 
      new StatementContainer(
          new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
      );
    CodePosition pos = CodePosition.fromOffsets(0, 3);
    InsertToken.insertTokenIntoStatementContainer(container, new Token.SimpleToken("A", Symbol.Number), pos, 0, false);
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number), new Token.SimpleToken("A", Symbol.Number))
        ), 
        container);
//    Assert.assertEquals(CodePosition.fromOffsets(0, 4), pos);
  }
  
  @Test
  public void testInsertMiddleOfLine()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
        );
      CodePosition pos = CodePosition.fromOffsets(0, 2);
      InsertToken.insertTokenIntoStatementContainer(container, new Token.SimpleToken("A", Symbol.Number), pos, 0, false);
      Assert.assertEquals(
          new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("A", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
          ), 
          container);
//      Assert.assertEquals(CodePosition.fromOffsets(0, 3), pos);
  }
  
  @Test
  public void testInsertBeforeWideToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number))
                    )
                )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0);
    InsertToken.insertTokenIntoStatementContainer(container, new Token.SimpleToken("A", Symbol.Number), pos, 0, true);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number))
                    )
                )
            )
        ),
        container);
//    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
  }
  
  @Test
  public void testInsertInIfBlock()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                    )
                )
            );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1);
    InsertToken.insertTokenIntoStatementContainer(container, new Token.SimpleToken("A", Symbol.Number), pos, 0, false);
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("A", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                    )
                )
            ),
        container);
//    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 2), pos);
  }
  
  public void testInsertWideTokenAfterSimpleToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
        );
    CodePosition pos = CodePosition.fromOffsets(0, 1);
    InsertToken.insertTokenIntoStatementContainer(container, new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF), pos, 0, true);
    Assert.assertEquals(CodePosition.fromOffsets(1, 1), pos);
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number)),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF),
                new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number)
                )
            ),
        container);
  }
}
