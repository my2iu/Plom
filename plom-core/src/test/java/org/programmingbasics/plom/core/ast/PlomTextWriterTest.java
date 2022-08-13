package org.programmingbasics.plom.core.ast;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextWriterTest extends TestCase
{
  @Test
  public void testWriteToken() throws IOException, PlomTextReader.PlomReadException
  {
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    writer.writeToken(out, new Token.SimpleToken("return", Symbol.Return));
    writer.writeToken(out, new Token.SimpleToken("var", Symbol.Var));
    writer.writeToken(out, new Token.SimpleToken("(", Symbol.OpenParenthesis));
    writer.writeToken(out, new Token.SimpleToken("\"hello\"", Symbol.String));
    writer.writeToken(out, new Token.SimpleToken("0.156", Symbol.Number));
    writer.writeToken(out, new Token.SimpleToken("-22", Symbol.Number));
    Assert.assertEquals(" return var (\"hello\" 0.156 -22", strBuilder.toString());
    
    // Check if we can read back the output
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(strBuilder.toString());
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(in);
    Assert.assertEquals("return", reader.lexInput());
    Assert.assertEquals("var", reader.lexInput());
    Assert.assertEquals("(", reader.lexInput());
    Assert.assertEquals("\"hello\"", reader.lexInput());
    Assert.assertEquals("0.156", reader.lexInput());
    Assert.assertEquals("-22", reader.lexInput());
  }
  
  @Test
  public void testWriteParameterTokens() throws IOException
  {
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    PlomCodeOutputFormatter plomOut = new PlomCodeOutputFormatter(out);
    writer.writeToken(plomOut, Token.ParameterToken.fromContents(".a hello:b:", Symbol.DotVariable, 
        new TokenContainer(
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            Token.ParameterToken.fromContents(".next", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("1", Symbol.Number)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".sd d  a:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("\"good\"", Symbol.String)
                    )
                )
            )
        ));
    Assert.assertEquals(" . {a hello: { @ {number } . {next } + 1 }b: { . {sd d  a: {\"good\" } } } }", out.toString());
  } 

  @Test
  public void testWriteFunctionType() throws IOException, PlomReadException
  {
    TokenContainer code = new TokenContainer(
        new Token.SimpleToken("var", Symbol.Var),
        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
        Token.ParameterToken.fromPartsWithoutPostfix(Arrays.asList("f@b:", "\u2192"), Symbol.FunctionType, 
            Arrays.asList(
                new TokenContainer(
                    Token.ParameterToken.fromPartsWithoutPostfix(Arrays.asList("f@go \u2192"), Symbol.FunctionType, Arrays.asList(new TokenContainer()))), 
                new TokenContainer(Token.ParameterToken.fromContents("@number", Symbol.AtType)))
        ));
    
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    PlomCodeOutputFormatter plomOut = new PlomCodeOutputFormatter(out);
    PlomTextWriter.writeTokenContainer(plomOut, code);
    Assert.assertEquals(" var . {b } f@ {b: { f@ {go \u2192 { } } }\u2192 { @ {number } } }", out.toString());

    // Check if we can read back the output
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(out.toString());
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    TokenContainer read = PlomTextReader.readTokenContainer(lexer);
    Assert.assertEquals(code, read);
  }

  @Test
  public void testWriteFunctionLiteral() throws IOException, PlomReadException
  {
    TokenContainer code = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
            new TokenContainer(
                ),
            new StatementContainer(
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("3", Symbol.Number)
                    ))),
        new Token.SimpleToken("+", Symbol.Plus)
        );
    
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    PlomCodeOutputFormatter plomOut = new PlomCodeOutputFormatter(out);
    PlomTextWriter.writeTokenContainer(plomOut, code);
    Assert.assertEquals(" 1 + lambda { } {\n"
        + " 2 + 3\n"
        + " }\n"
        + " +", out.toString());

    // Check if we can read back the output
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(out.toString());
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    TokenContainer read = PlomTextReader.readTokenContainer(lexer);
    Token a = code.tokens.get(2);
    Token b = read.tokens.get(2);
    Assert.assertTrue(a.equals(b));
    Assert.assertEquals(code, read);
  }

  @Test
  public void testWriteStatementContainer() throws IOException, PlomTextReader.PlomReadException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("var", Symbol.Var),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//                        new Token.SimpleToken(":", Symbol.Colon),
                        Token.ParameterToken.fromContents("@string", Symbol.AtType)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable))
                    )), 
            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                new StatementContainer(
                    new TokenContainer(
                        new Token.WideToken("//Comment\nComment line 2", Symbol.DUMMY_COMMENT)
                        ))),
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@string", Symbol.AtType)
            )
        );
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    writer.writeStatementContainer(new PlomCodeOutputFormatter(out), code);

    Assert.assertEquals(" var . {a } @ {number }\n" + 
        " if { } {\n" + 
        " var . {b } @ {string }\n" + 
        " . {b }\n" + 
        " }\n" + 
        " else {\n" + 
        " //Comment\\nComment line 2\n" + 
        "\n" + 
        " }\n" + 
        " var . {c } @ {string }\n" + 
        "", out.toString());

    // Check if we can read back the output
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(out.toString());
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    StatementContainer read = PlomTextReader.readStatementContainer(lexer);
    Assert.assertEquals(code, read);
  }
}
