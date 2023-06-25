package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class NextPositionTest extends TestCase
{
  @Test
  public void testSimpleToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("1", Symbol.Number), 
                new Token.SimpleToken("2", Symbol.Number)),
            new TokenContainer(
                new Token.SimpleToken("A", Symbol.Number) 
                )
          );
    CodePosition pos = new CodePosition();
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(0, 2), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 1), pos);
    Assert.assertTrue(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 1), pos);
  }
  
  @Test
  public void testBlock()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("Z", Symbol.Number)),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("1", Symbol.Number), 
                            new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                            Token.ParameterToken.fromContents(".call:param1:param2:", Symbol.DotVariable, 
                                new TokenContainer(new Token.SimpleToken("arg1", Symbol.Number)),
                                new TokenContainer(),
                                new TokenContainer(new Token.SimpleToken("arg3", Symbol.Number))))
                            )
                ),
                new Token.WideToken("comment", Symbol.DUMMY_COMMENT)
            )
        );
    CodePosition pos = new CodePosition();
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(0, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 2), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1, CodePosition.PARAMTOK_POS_EXPRS, 1, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1, CodePosition.PARAMTOK_POS_EXPRS, 2, 0), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1, CodePosition.PARAMTOK_POS_EXPRS, 2, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 2), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 1), pos);
    Assert.assertFalse(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 2), pos);
    Assert.assertTrue(NextPosition.nextPositionOfStatements(container, pos, 0));
    Assert.assertEquals(CodePosition.fromOffsets(1, 2), pos);
  }
}
