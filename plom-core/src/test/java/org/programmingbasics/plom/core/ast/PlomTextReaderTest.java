package org.programmingbasics.plom.core.ast;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextReaderTest extends TestCase
{
  @Test
  public void testReadKeywords() throws IOException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("var} if{+-/*\"hello\"0.23--36//");
    PlomTextReader reader = new PlomTextReader();
    PlomTextReader.StringToken tok = new PlomTextReader.StringToken();
    reader.lexInput(in, tok);
    Assert.assertEquals("var", tok.str);
    Assert.assertEquals(Symbol.Var, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("}", tok.str);
    Assert.assertEquals(null, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("if", tok.str);
    Assert.assertEquals(Symbol.COMPOUND_IF, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("{", tok.str);
    Assert.assertEquals(null, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("+", tok.str);
    Assert.assertEquals(Symbol.Plus, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("-", tok.str);
    Assert.assertEquals(Symbol.Minus, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("/", tok.str);
    Assert.assertEquals(Symbol.Divide, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("*", tok.str);
    Assert.assertEquals(Symbol.Multiply, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("\"hello\"", tok.str);
    Assert.assertEquals(Symbol.String, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("0.23", tok.str);
    Assert.assertEquals(Symbol.Number, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("-", tok.str);
    Assert.assertEquals(Symbol.Minus, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("-36", tok.str);
    Assert.assertEquals(Symbol.Number, tok.sym);
    reader.lexInput(in, tok);
    Assert.assertEquals("//", tok.str);
    Assert.assertEquals(Symbol.DUMMY_COMMENT, tok.sym);
  }
}
