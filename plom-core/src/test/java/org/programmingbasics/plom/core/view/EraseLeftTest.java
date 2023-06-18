package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class EraseLeftTest extends TestCase
{
  @Test
  public void testEmptyContainer()
  {
    StatementContainer container = new StatementContainer();
    CodePosition pos = new CodePosition();
    Assert.assertEquals(EraseLeft.AfterAction.ERASE_BEGINNING, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer()), 
        container);
    Assert.assertEquals(CodePosition.fromOffsets(), pos);
  }
  
  @Test
  public void testMiddleOfLine()
  {
    StatementContainer container = new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))
        );
    CodePosition pos = CodePosition.fromOffsets(0, 2);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("3", Symbol.Number))), 
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
  }

  @Test
  public void testInsideIfExpression()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), pos);
  }

  @Test
  public void testInsideIfBlock()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 2);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number)))
                )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 1), pos);
  }

  @Test
  public void testInsideIfBlockMergeLines()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number), 
                            new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 2), pos);
  }

  @Test
  public void testMergeLinesNonWideWithWide()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
            new TokenContainer(new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE))
        );
    CodePosition pos = CodePosition.fromOffsets(1, 0);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
            new TokenContainer(new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE))
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
  }

  @Test
  public void testEraseAfterIfIntoIf()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            ),
            new TokenContainer(new Token.SimpleToken("B", Symbol.Number))
        );
    CodePosition pos = CodePosition.fromOffsets(1, 0);
    // First erase left will put the normal token right after the if token
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                ),
                new Token.SimpleToken("B", Symbol.Number)
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
    // Next backspace will not delete the "if" but just move the cursor into the if
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                ),
                new Token.SimpleToken("B", Symbol.Number)
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 2), pos);
  }

  @Test
  public void testErasePastBeginningOfIfBlock()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1), pos);
  }

  @Test
  public void testErasePastBeginningOfIfExpression()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(new Token.SimpleToken("11", Symbol.Number), new Token.SimpleToken("22", Symbol.Number)))
                ),
                new Token.SimpleToken("B", Symbol.Number)
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0);
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("B", Symbol.Number)
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0), pos);
  }

  @Test
  public void testParameterBlock()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable),
                Token.ParameterToken.fromContents(".call param1:param2:", Symbol.DotVariable,
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new TokenContainer(new Token.SimpleToken("1", Symbol.Number))
                    )
            )
        );
    CodePosition pos = CodePosition.fromOffsets(0, 2);
    // First backspace should go to the last parameter
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable),
                Token.ParameterToken.fromContents(".call param1:param2:", Symbol.DotVariable, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new TokenContainer(new Token.SimpleToken("1", Symbol.Number))
                    )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 1, 1), pos);
    // Second backspace deletes the token in the last parameter
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable),
                Token.ParameterToken.fromContents(".call param1:param2:", Symbol.DotVariable, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new TokenContainer()
                    )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 1, 0), pos);
    // Third backspace moves to the first parameter
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable),
                Token.ParameterToken.fromContents(".call param1:param2:", Symbol.DotVariable,
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new TokenContainer()
                    )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1), pos);
    // Fourth backspace deletes the token in the first parameter
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable),
                Token.ParameterToken.fromContents(".call param1:param2:", Symbol.DotVariable, 
                    new TokenContainer(),
                    new TokenContainer()
                    )
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0), pos);
    // Backspace from the first parameter will delete the token
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".variable", Symbol.DotVariable)
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
    // Backspace over parameter token with no parameters
    Assert.assertEquals(EraseLeft.AfterAction.NONE, EraseLeft.eraseLeftFromStatementContainer(container, pos, 0));
    Assert.assertEquals(
        new StatementContainer(
            new TokenContainer(
            )
        ),
        container);
    Assert.assertEquals(CodePosition.fromOffsets(0, 0), pos);
  }
}
