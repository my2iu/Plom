package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class CodeNestingCounterTest extends TestCase
{
  private Token getToken(StatementContainer code, int...pos)
  {
    return GetToken.inStatements(code, CodePosition.fromOffsets(pos), 0);
  }

  private int getNesting(CodeNestingCounter nestingCounter, StatementContainer code, int ...pos)
  {
    return nestingCounter.tokenNesting.get(getToken(code, pos)).intValue();
  }
  
  @Test
  public void testStatements()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".boolean", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                    new Token.SimpleToken("and", Symbol.And),
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("=", Symbol.Gt),
                    new Token.SimpleToken("5", Symbol.Number)
                    ),
                new StatementContainer())
            )
        );

    CodeNestingCounter nestingCounter = new CodeNestingCounter(); 
    nestingCounter.calculateNestingForStatements(code);

    Assert.assertEquals(1, getNesting(nestingCounter, code, 0, 0));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 1, 1));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 1, 2));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1));
  }
  
  @Test
  public void testParameterToken()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("\"hello\"", Symbol.String),
            Token.ParameterToken.fromContents(".substring from:to:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number)),
                new TokenContainer(
                    new Token.SimpleToken("3", Symbol.Number)))),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".to string", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number),
            Token.ParameterToken.fromContents(".+:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number),
                    Token.ParameterToken.fromContents(".+:", Symbol.DotVariable,
                        new TokenContainer(
                            new Token.SimpleToken("3", Symbol.Number)
                            )
                        )
                    )
                )
            ));

    CodeNestingCounter nestingCounter = new CodeNestingCounter(); 
    nestingCounter.calculateNestingForStatements(code);

    Assert.assertEquals(1, getNesting(nestingCounter, code, 2, 0));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 2, 1));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 2, 2));
    Assert.assertEquals(2, getNesting(nestingCounter, code, 0, 1));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0));
    Assert.assertEquals(1, getNesting(nestingCounter, code, 3, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0));
    Assert.assertEquals(2, getNesting(nestingCounter, code, 3, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1));
    Assert.assertEquals(3, getNesting(nestingCounter, code, 3, 1));
  }
}
