package org.programmingbasics.plom.core.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.FindToken;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.RenderedHitBox.Rect;

import junit.framework.TestCase;

public class FindTokenTest extends TestCase
{
  @Test
  public void testTokenToPosition()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("2", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            Token.ParameterToken.fromContents(".fun:to:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("3", Symbol.Number),
                    new Token.SimpleToken("4", Symbol.Number)
                    ),
                new TokenContainer(
                    new Token.SimpleToken("5", Symbol.Number),
                    new Token.SimpleToken("6", Symbol.Number)
                    ))
            ),
        new TokenContainer(),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                new TokenContainer(
                      new Token.SimpleToken("7", Symbol.Number),
                      new Token.SimpleToken("8", Symbol.Number)
                    ),
                new StatementContainer(
                    new TokenContainer(),
                    new TokenContainer(
                        new Token.WideToken("//comment", Symbol.DUMMY_COMMENT)
                        )
                    )),
            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("9", Symbol.Number),
                        new Token.SimpleToken("10", Symbol.Number)

                        )
                    ))
            )
        );
    
    CodePosition pos;
    Token token;

    // Simple token in flat list
    pos = CodePosition.fromOffsets(0, 1);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("+", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));
    
    // Parameter token
    pos = CodePosition.fromOffsets(0, 2);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals(".fun:to:", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));

    // Recurse into parameter token
    pos = CodePosition.fromOffsets(0, 2, CodePosition.PARAMTOK_POS_EXPRS, 1, 1);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("6", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));

    // One block token
    pos = CodePosition.fromOffsets(2, 1);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("else", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));
    
    // Recurse into one block token
    pos = CodePosition.fromOffsets(2, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("10", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));
    
    // One expression one block token
    pos = CodePosition.fromOffsets(2, 0);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("if", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));
    
    // Recurse into expression of one expression one block token 
    pos = CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("7", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));

    // Recurse into block of one expression one block token 
    // Comment
    pos = CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0);
    token = GetToken.inStatements(code, pos, 0);
    Assert.assertEquals("//comment", ((Token.TokenWithEditableTextContent)token).getTextContent());
    Assert.assertEquals(pos, FindToken.tokenToPosition(token, code));
  }
}
