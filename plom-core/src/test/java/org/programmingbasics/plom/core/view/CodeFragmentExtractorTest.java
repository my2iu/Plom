package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class CodeFragmentExtractorTest extends TestCase
{
  @Test
  public void testExtractFragmentFromLine()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                new Token.SimpleToken("1", Symbol.Number),
                new Token.SimpleToken("+", Symbol.Plus),
                new Token.SimpleToken("1", Symbol.Number)
                ),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                            new TokenContainer(
                                new Token.SimpleToken("2", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("3", Symbol.Number)))
                        ), 
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("5", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("6", Symbol.Number)
                            )
                        ))
                )
            );
    // Extract part of a line
    Assert.assertEquals(" := 1 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(0, 4)));
    // Extract part of a line containing a wide token
    Assert.assertEquals(" if { . {print: { 2 + 3 } } } {\n 5 + 6\n }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0), CodePosition.fromOffsets(1, 1)));
    // Extract part of an expression in an if
    Assert.assertEquals(" . {print: { 2 + 3 } }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1)));
    // Extract part of a block in an if
    Assert.assertEquals(" + 6", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 3)));
    // Extract part of a function call
    Assert.assertEquals(" 2 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 2)));
  }
  
  @Test
  public void testExtractLineFromStatements()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                new Token.SimpleToken("1", Symbol.Number),
                new Token.SimpleToken("+", Symbol.Plus),
                new Token.SimpleToken("1", Symbol.Number)
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                    new TokenContainer(
                        new Token.SimpleToken("2", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("3", Symbol.Number)))
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable)
                )
            );
    // Extract part of a line
    Assert.assertEquals(" := 1 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(2, 1)));
  }
}
