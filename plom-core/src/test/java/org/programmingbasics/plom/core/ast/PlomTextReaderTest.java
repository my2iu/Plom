package org.programmingbasics.plom.core.ast;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextReaderTest extends TestCase
{
  private static void assertTokenEquals(String str, Symbol sym, PlomTextReader.StringToken tok)
  {
    Assert.assertEquals(str, tok.str);
    Assert.assertEquals(sym, tok.sym);
  }

  @Test
  public void testReadKeywords() throws IOException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("var} if{+-/*\"hello\"0.23--36//");
    PlomTextReader reader = new PlomTextReader();
    PlomTextReader.StringToken tok = new PlomTextReader.StringToken();
    reader.lexInput(in, tok);
    assertTokenEquals("var", Symbol.Var, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("if", Symbol.COMPOUND_IF, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("+", Symbol.Plus, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("-", Symbol.Minus, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("/", Symbol.Divide, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("*", Symbol.Multiply, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("\"hello\"", Symbol.String, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("0.23", Symbol.Number, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("-", Symbol.Minus, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("-36", Symbol.Number, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("//", Symbol.DUMMY_COMMENT, tok);
  }
  
  @Test
  public void testReadParameterTokens() throws IOException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(".{ a hello:  {.{a}} big:{.{c}.{b}}} + 22");
    PlomTextReader reader = new PlomTextReader();
    PlomTextReader.StringToken tok = new PlomTextReader.StringToken();
    
    reader.lexInput(in, tok);
    assertTokenEquals(".", Symbol.DotVariable, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    Assert.assertEquals("a hello:", reader.lexParameterTokenPart(in));
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals(".", Symbol.DotVariable, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    Assert.assertEquals("a", reader.lexParameterTokenPart(in));
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    Assert.assertEquals("big:", reader.lexParameterTokenPart(in));
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals(".", Symbol.DotVariable, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    Assert.assertEquals("c", reader.lexParameterTokenPart(in));
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals(".", Symbol.DotVariable, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("{", null, tok);
    Assert.assertEquals("b", reader.lexParameterTokenPart(in));
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("}", null, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("+", Symbol.Plus, tok);
    reader.lexInput(in, tok);
    assertTokenEquals("22", Symbol.Number, tok);
    Assert.assertEquals(null, reader.lexParameterTokenPart(in));
  }
}
