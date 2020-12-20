package org.programmingbasics.plom.core.view;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class EraseSelectionTest extends TestCase
{
  private String toString(StatementContainer container)
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    try {
      PlomTextWriter.writeStatementContainer(out, container);
    } catch (IOException e) {}
    return strBuilder.toString();
  }
  
  @Test
  public void testEraseFromLine()
  {
    Supplier<StatementContainer> supplier = () ->  
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
    // Erase part of a line
    StatementContainer container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(0, 4));
    Assert.assertEquals(" . {a } 1\n" + 
        " if { . {print: { 2 + 3 } } } {\n" + 
        " 5 + 6\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase part of a line containing a wide token
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0), CodePosition.fromOffsets(1, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n\n", toString(container));
    // Erase part of an expression in an if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { } {\n" + 
        " 5 + 6\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase part of a block in an if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 3));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { . {print: { 2 + 3 } } } {\n" + 
        " 5\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase part of a function call
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 2));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { . {print: { 3 } } } {\n" + 
        " 5 + 6\n" + 
        " }\n" + 
        "\n", toString(container));
  }
  
  @Test
  public void testEraseLineFromStatements()
  {
    Supplier<StatementContainer> supplier = () -> 
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
    // Erase part of a line
    StatementContainer container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(2, 1));
    Assert.assertEquals(" . {a } := . {a }\n" + 
        " else {\n" +
        " 5 + 6\n" +
        " 7 + 8\n" +
        " }\n\n", toString(container));
    // Erase part of a line (and positions specify a line but not a position within that line
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(2));
    Assert.assertEquals(" . {a }\n" + 
        " . {b } := . {a }\n" + 
        " else {\n" + 
        " 5 + 6\n" + 
        " 7 + 8\n" + 
        " }\n" + 
        "\n", toString(container));
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1), CodePosition.fromOffsets(2, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " := . {a }\n" + 
        " else {\n" + 
        " 5 + 6\n" + 
        " 7 + 8\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase part of a line from within a block
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(3, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(3, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " . {print: { 2 + 3 } }\n" + 
        " . {b } := . {a }\n" + 
        " else {\n" + 
        " 5 + 8\n" + 
        " }\n" + 
        "\n" + 
        "", toString(container));
  }

  @Test
  public void testEraseAfter()
  {
    Supplier<StatementContainer> supplier = () ->
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
    // Erase the end of a parameter token
    StatementContainer container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 2), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { . {print: { 2 + } } } {\n" + 
        " 5 + 6\n" + 
        " 0\n" + 
        " }\n" + 
        " . {a: { 2 + 3 }b: { 4 + 5 } } + 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
    // Erase the end of a parameter token and the rest of a parameter list
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 1, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1), CodePosition.fromOffsets(1, 2));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { . {print: { 2 + 3 } } } {\n" + 
        " 5 + 6\n" + 
        " 0\n" + 
        " }\n" + 
        " . {a: { 2 }b: { } } + 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
    // Erase end of a block in an if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1), CodePosition.fromOffsets(1, 2));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { . {print: { 2 + 3 } } } {\n" + 
        " 5\n" + 
        " }\n" + 
        " + 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
    // Erase end of the expression in an if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { } {\n" + 
        " 5 + 6\n" + 
        " 0\n" + 
        " }\n" + 
        " . {a: { 2 + 3 }b: { 4 + 5 } } + 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
    // Erase end of the expression and block in an if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 1));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { } {\n" + 
        " }\n" + 
        " . {a: { 2 + 3 }b: { 4 + 5 } } + 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
    // Erase end of a block in an if, plus some of the successive tokens in the same token container as the if
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0), CodePosition.fromOffsets(1, 3));
    Assert.assertEquals(" . {a } := 1 + 1\n" + 
        " if { } {\n" + 
        " }\n" + 
        " 7\n" + 
        " . {print: { 8 + 9 } }\n" + 
        " . {b } := . {a }\n", toString(container));
  }

  @Test
  public void testEraseBefore()
  {
    Supplier<StatementContainer> supplier = () -> 
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
    // Erase the first part of a parameter token
    StatementContainer container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0), CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1));
    Assert.assertEquals(" // comment\n" + 
        " else {\n" + 
        " 1 + 2\n" + 
        " + 4 5 + 6\n" + 
        " }\n" + 
        " if { 7 + 8 } {\n" + 
        " 9 + 0\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase part of the 2nd parameter of a parameter token
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0), CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 1, 2));
    Assert.assertEquals(" // comment\n" + 
        " else {\n" + 
        " 1 + 2\n" + 
        " 6\n" + 
        " }\n" + 
        " if { 7 + 8 } {\n" + 
        " 9 + 0\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase the beginning of an else token (including the else)
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 2));
    Assert.assertEquals(" // comment\n" + 
        " 2\n" + 
        " . {a: { 3 + 4 }b: { 5 + 6 } }\n" +
        " if { 7 + 8 } {\n" + 
        " 9 + 0\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase the beginning of an expression of an if token (including the if)
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 2), CodePosition.fromOffsets(0, 2, CodeRenderer.EXPRBLOCK_POS_EXPR, 2));
    Assert.assertEquals(" // comment\n" + 
        " else {\n" + 
        " 1 + 2\n" + 
        " . {a: { 3 + 4 }b: { 5 + 6 } }\n" +
        " }\n" + 
        " 8\n" + 
        " 9 + 0\n" + 
        "\n", toString(container));
    // Erase the beginning of the block of an if token (including the if)
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 2), CodePosition.fromOffsets(0, 2, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1));
    Assert.assertEquals(" // comment\n" + 
        " else {\n" + 
        " 1 + 2\n" + 
        " . {a: { 3 + 4 }b: { 5 + 6 } }\n" +
        " }\n" + 
        " + 0\n" + 
        "\n", toString(container));
    // Erase through multiple levels
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 0), CodePosition.fromOffsets(0, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1));
    Assert.assertEquals(" + 4\n"
        + " 5 + 6\n" + 
        " if { 7 + 8 } {\n" + 
        " 9 + 0\n" + 
        " }\n" + 
        "\n", toString(container));
  }
  
  @Test
  public void testEraseAfterBefore()
  {
    Supplier<StatementContainer> supplier = () -> 
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
    // Erase between the end of one parameter in a parameter token and the beginning of another parameter
    StatementContainer container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 2), CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 2, 1));
    Assert.assertEquals(" . {a: { . {a: { 1 + }b: { }c: { + 6 } } } }\n" + 
        " if { 7 + 8 } {\n" + 
        " 9 + 0\n" + 
        " }\n" + 
        " else {\n" + 
        " 11 + 12\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase between an if expression and an if block
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 2), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 2));
    Assert.assertEquals(" . {a: { . {a: { 1 + 2 }b: { 3 + 4 }c: { 5 + 6 } } } }\n" + 
        " if { 7 + } {\n" + 
        " 0\n" + 
        " }\n" + 
        " else {\n" + 
        " 11 + 12\n" + 
        " }\n" + 
        "\n", toString(container));
    // Erase between an if block and an else block
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 2), CodePosition.fromOffsets(1, 1, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1));
    Assert.assertEquals(" . {a: { . {a: { 1 + 2 }b: { 3 + 4 }c: { 5 + 6 } } } }\n" + 
        " if { 7 + 8 } {\n" + 
        " 9 +\n" + 
        " }\n" + 
        " + 12\n" + 
        "\n", toString(container));
    // Erase from a non-wide token to inside the start of a wide token that comes before another wide token
    container = supplier.get();
    EraseSelection.fromStatements(container, CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 2, 1), CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 1));
    Assert.assertEquals(" . {a: { . {a: { 1 + 2 }b: { 3 + 4 }c: { 5 } } } } + 0\n" + 
        " else {\n" + 
        " 11 + 12\n" + 
        " }\n" + 
        "\n", toString(container));
  }
}
