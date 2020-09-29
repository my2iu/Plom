package org.programmingbasics.plom.core.ast;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextWriterTest extends TestCase
{
  private static void assertTokenEquals(String str, Symbol sym, PlomTextReader.StringToken tok)
  {
    Assert.assertEquals(str, tok.str);
    Assert.assertEquals(sym, tok.sym);
  }
  
  @Test
  public void testWriteToken() throws IOException
  {
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    writer.writeToken(out, new Token.SimpleToken("return", Symbol.Return));
    writer.writeToken(out, new Token.SimpleToken("var", Symbol.Var));
    writer.writeToken(out, new Token.SimpleToken("(", Symbol.OpenParenthesis));
    writer.writeToken(out, new Token.SimpleToken("\"hello\"", Symbol.String));
    writer.writeToken(out, new Token.SimpleToken("0.156", Symbol.Number));
    writer.writeToken(out, new Token.SimpleToken("-22", Symbol.Number));
    Assert.assertEquals(" return var (\"hello\" 0.156 -22", out.toString());
    
    // Check if we can read back the output
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(out.toString());
    PlomTextReader reader = new PlomTextReader();
    PlomTextReader.StringToken tok = new PlomTextReader.StringToken();
    reader.lexInput(in, tok);
    assertTokenEquals("return", Symbol.Return, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("var", Symbol.Var, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("(", Symbol.OpenParenthesis, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("\"hello\"", Symbol.String, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("0.156", Symbol.Number, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("-22", Symbol.Number, tok);
  }
  
  @Test
  public void testWriteParameterTokens() throws IOException
  {
    PlomTextWriter writer = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    writer.writeToken(out, Token.ParameterToken.fromContents(".a hello:b:", Symbol.DotVariable, 
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
            )));
    Assert.assertEquals(".{a hello:{@{number}.{next} + 1}b:{.{sd d  a:{\"good\"}}}}", out.toString());
  }  
}
