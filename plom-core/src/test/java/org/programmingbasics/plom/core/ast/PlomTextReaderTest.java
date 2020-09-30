package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextReaderTest extends TestCase
{
  @Test
  public void testReadKeywords() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("var} if{+-/*\"hello\"0.23--36//");
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(in);
    Assert.assertEquals("var", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("if", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    Assert.assertEquals("-", reader.lexInput());
    Assert.assertEquals("/", reader.lexInput());
    Assert.assertEquals("*", reader.lexInput());
    Assert.assertEquals("\"hello\"", reader.lexInput());
    Assert.assertEquals("0.23", reader.lexInput());
    Assert.assertEquals("-", reader.lexInput());
    Assert.assertEquals("-36", reader.lexInput());
    Assert.assertEquals("//", reader.lexInput());
  }
  
  @Test
  public void testReadParameterTokens() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(".{ a hello:  {.{a}} big:{.{c}.{b}}} + 22");
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(in);
    
    Assert.assertEquals(".", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("a hello:", reader.lexParameterTokenPart());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals(".", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("a", reader.lexParameterTokenPart());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("big:", reader.lexParameterTokenPart());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals(".", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("c", reader.lexParameterTokenPart());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals(".", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("b", reader.lexParameterTokenPart());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    Assert.assertEquals("22", reader.lexInput());
    Assert.assertEquals(null, reader.lexParameterTokenPart());
  }
  
  @Test
  public void testReadTokenContainer() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(".{ a hello:  {.{a}} big:{.{c}.{b}}} + 22");
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    TokenContainer container = PlomTextReader.readTokenContainer(lexer);
    
    Assert.assertEquals(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a hello:big:", Symbol.DotVariable, 
                new TokenContainer(Token.ParameterToken.fromContents(".a", Symbol.DotVariable)),
                new TokenContainer(
                    Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable))),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("22", Symbol.Number)), 
        container);
  }
}
