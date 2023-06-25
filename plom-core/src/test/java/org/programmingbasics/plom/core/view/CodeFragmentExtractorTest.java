package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
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
    Assert.assertEquals(" . {print: { 2 + 3 } }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 1)));
    // Extract part of a block in an if
    Assert.assertEquals(" + 6", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 3)));
    // Extract part of a function call
    Assert.assertEquals(" 2 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 0), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 2)));
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
                ),
            new TokenContainer(
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("5", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("6", Symbol.Number)),
                        new TokenContainer(
                            new Token.SimpleToken("7", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("8", Symbol.Number))
                        ))
                )
            );
    // Extract part of a line
    Assert.assertEquals(" := 1 + 1\n" + 
        " . {print: { 2 + 3 } }\n" + 
        " . {b }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(2, 1)));
    // Extract part of a line (and positions specify a line but not a position within that line
    Assert.assertEquals(" := 1 + 1\n" + 
        " . {print: { 2 + 3 } }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(2)));
    Assert.assertEquals(" . {print: { 2 + 3 } }\n" + 
        " . {b }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1), CodePosition.fromOffsets(2, 1)));
    // Extract part of a line from within a block
    Assert.assertEquals(" + 6\n" + 
        " 7", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(3, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(3, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 1)));
  }

  @Test
  public void testExtractAfter()
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
                            ),
                        new TokenContainer(
                            new Token.SimpleToken("0", Symbol.Number))
                        )
                    ),
                Token.ParameterToken.fromContents(".a:b:", Symbol.DotVariable, 
                    new TokenContainer(
                        new Token.SimpleToken("2", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("3", Symbol.Number)),
                    new TokenContainer(
                        new Token.SimpleToken("4", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("5", Symbol.Number))
                    ),
                new Token.SimpleToken("+", Symbol.Plus),
                new Token.SimpleToken("7", Symbol.Number)
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                    new TokenContainer(
                        new Token.SimpleToken("8", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("9", Symbol.Number)))
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable)
                )
            );
    // Extract the end of a parameter token
    Assert.assertEquals(" 3\n\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 2), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 1)));
    // Extract the end of a parameter token and the rest of a parameter list
    Assert.assertEquals(" + 3\n 4 + 5\n\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 1), CodePosition.fromOffsets(1, 2)));
    // Extract end of a block in an if
    Assert.assertEquals(" + 6\n 0\n\n . {a: { 2 + 3 }b: { 4 + 5 } }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(1, 2)));
    // Extract end of the expression in an if
    Assert.assertEquals(" . {print: { 2 + 3 } }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0)));
    // Extract end of the expression and block in an if
    Assert.assertEquals(" . {print: { 2 + 3 } }\n"
        + " 5 + 6\n"
        + " 0\n\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 1)));
    // Extract end of a block in an if, plus some of the successive tokens in the same token container as the if
    Assert.assertEquals(" . {print: { 2 + 3 } }\n"
        + " 5 + 6\n"
        + " 0\n\n"
        + " . {a: { 2 + 3 }b: { 4 + 5 } } +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 3)));
  }

  @Test
  public void testExtractBefore()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// comment", Symbol.DUMMY_COMMENT),
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("1", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("2", Symbol.Number)),
                        new TokenContainer(Token.ParameterToken.fromContents(".a:b:", Symbol.DotVariable, 
                            new TokenContainer(
                                new Token.SimpleToken("3", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("4", Symbol.Number)
                                ),
                            new TokenContainer(
                                new Token.SimpleToken("5", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("6", Symbol.Number)
                                )
                            ))
                        )
                    ),
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                    new TokenContainer(
                        new Token.SimpleToken("7", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("8", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("9", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("0", Symbol.Number))
                        ))
                )
            );
    // Extract the first part of a parameter token
    Assert.assertEquals(" . {a: { 3 }b: { } }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0), CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 1)));
    // Extract part of the 2nd parameter of a parameter token
    Assert.assertEquals(" . {a: { 3 + 4 }b: { 5 + } }", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0), CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0, CodePosition.PARAMTOK_POS_EXPRS, 1, 2)));
    // Extract the beginning of an else token (including the else)
    Assert.assertEquals(" else {\n 1 +\n }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 2)));
    // Extract the beginning of an expression of an if token (including the if)
    Assert.assertEquals(" if { 7 + } {\n }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 2), CodePosition.fromOffsets(0, 2, CodePosition.EXPRBLOCK_POS_EXPR, 2)));
    // Extract the beginning of the block of an if token (including the if)
    Assert.assertEquals(" if { 7 + 8 } {\n 9\n }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 2), CodePosition.fromOffsets(0, 2, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1)));
    // Extract through multiple levels
    Assert.assertEquals(" // comment\n"
        + " else {\n"
        + " 1 + 2\n"
        + " . {a: { 3 }b: { } }\n"
        + " }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 0), CodePosition.fromOffsets(0, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 1)));
  }
  
  @Test
  public void testExtractAfterBefore()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".a:", Symbol.DotVariable,
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a:b:c:", Symbol.DotVariable, 
                            new TokenContainer(
                                new Token.SimpleToken("1", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("2", Symbol.Number)
                                ),
                            new TokenContainer(
                                new Token.SimpleToken("3", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("4", Symbol.Number)
                                ),
                            new TokenContainer(
                                new Token.SimpleToken("5", Symbol.Number),
                                new Token.SimpleToken("+", Symbol.Plus),
                                new Token.SimpleToken("6", Symbol.Number)
                                ))
                        )
                    )
                ),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                    new TokenContainer(
                        new Token.SimpleToken("7", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("8", Symbol.Number)
                        ),
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("9", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("0", Symbol.Number)
                            )
                        )),
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("11", Symbol.Number),
                            new Token.SimpleToken("+", Symbol.Plus),
                            new Token.SimpleToken("12", Symbol.Number)
                            )
                        ))
                )
            );
    // Extract between the end of one parameter in a parameter token and the beginning of another parameter
    Assert.assertEquals(" 2\n 3 + 4\n 5", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 2), CodePosition.fromOffsets(0, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 0, CodePosition.PARAMTOK_POS_EXPRS, 2, 1)));
    // Extract between an if expression and an if block
    Assert.assertEquals(" 8\n 9 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 2), CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 2)));
    // Extract between an if block and an else block
    Assert.assertEquals(" 0\n\n else {\n 11\n }\n", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 2), CodePosition.fromOffsets(1, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1)));
  }
}
