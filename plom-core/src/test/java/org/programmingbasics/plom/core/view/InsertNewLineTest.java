package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;


public class InsertNewLineTest extends TestCase
{
  @Test
  public void testInsertEmptyContainer()
  {
    StatementContainer container = new StatementContainer();
    CodePosition pos = new CodePosition();
    InsertNewLine.insertNewlineIntoStatementContainer(container, pos, 0);
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer(), 
          new TokenContainer()), 
        container);
  }
  
  @Test
  public void testInsertLastEmptyLine()
  {
    StatementContainer container = new StatementContainer(new TokenContainer(), new TokenContainer());
    CodePosition pos = new CodePosition();
    pos.setOffset(0, 1);
    InsertNewLine.insertNewlineIntoStatementContainer(container, pos, 0);
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer(), 
          new TokenContainer(), 
          new TokenContainer()), 
        container);
  }

  @Test
  public void testInsertInMiddleOfLine()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
        );
    CodePosition pos = new CodePosition();
    pos.setOffset(0, 0);
    pos.setOffset(1, 2);
    InsertNewLine.insertNewlineIntoStatementContainer(container, pos, 0);
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
            new TokenContainer(new Token.SimpleToken("3", Symbol.Number))
        ), 
        container);
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
    InsertNewLine.insertNewlineIntoStatementContainer(container, pos, 0);
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("2", Symbol.Number)))
                )
            )
        ),
        container);
  }

}
