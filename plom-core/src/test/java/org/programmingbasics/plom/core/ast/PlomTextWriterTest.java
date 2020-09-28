package org.programmingbasics.plom.core.ast;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextWriterTest extends TestCase
{
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
    Assert.assertEquals("return", tok.str);
    Assert.assertEquals(Symbol.Return, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("var", tok.str);
    Assert.assertEquals(Symbol.Var, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("(", tok.str);
    Assert.assertEquals(Symbol.OpenParenthesis, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("\"hello\"", tok.str);
    Assert.assertEquals(Symbol.String, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("0.156", tok.str);
    Assert.assertEquals(Symbol.Number, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("-22", tok.str);
    Assert.assertEquals(Symbol.Number, tok.sym);
  }
}
