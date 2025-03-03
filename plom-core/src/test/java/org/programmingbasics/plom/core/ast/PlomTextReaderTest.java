package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextReaderTest extends TestCase
{
  @Test
  public void testCoerceToNumberMatch()
  {
    Assert.assertEquals("-23.2398", 
        PlomTextReader.coerceToNumberMatch("0023-ad..f,,j\n\r 2398"));
    Assert.assertEquals("-232398", 
        PlomTextReader.coerceToNumberMatch("0023-2398.."));
    Assert.assertEquals("0.55", 
        PlomTextReader.coerceToNumberMatch(".55"));
    Assert.assertEquals("-0.25", 
        PlomTextReader.coerceToNumberMatch("-.25"));
    Assert.assertEquals("0", 
        PlomTextReader.coerceToNumberMatch("0000000"));
    Assert.assertEquals("0.003300", 
        PlomTextReader.coerceToNumberMatch("0000000.003300"));
    Assert.assertEquals("3", 
        PlomTextReader.coerceToNumberMatch("00000003"));
    Assert.assertEquals("3.000200", 
        PlomTextReader.coerceToNumberMatch("00000003.000200"));
    Assert.assertEquals("0", 
        PlomTextReader.coerceToNumberMatch("adsfasd"));
    Assert.assertEquals("2.0308", 
        PlomTextReader.coerceToNumberMatch("002fs.03zd08adsfasd."));
  }
  
  @Test
  public void testReadKeywords() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("var} if{+-/*/ *\"hello\"0.23--36//");
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(in);
    Assert.assertEquals("var", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("if", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    Assert.assertEquals("-", reader.lexInput());
    Assert.assertEquals("/*", reader.lexInput());
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
  
  @Test
  public void testReadFunctionType() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader inScanner = new PlomTextReader.StringTextReader("var 22f@{a}returns");
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(inScanner);
    Assert.assertEquals("var", reader.lexInput());
    Assert.assertEquals("22", reader.lexInput());
    Assert.assertEquals("f@", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("a", reader.lexParameterTokenPart());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("returns", reader.lexInput());
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("var .{b} f@ {b: { f@ {go} returns }} returns @{number}");
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    TokenContainer container = PlomTextReader.readTokenContainer(lexer);
    
    Assert.assertEquals(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@b:", Symbol.FunctionTypeName, 
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@go", Symbol.FunctionTypeName),
                    new Token.SimpleToken("returns", Symbol.Returns))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        container);
  }

  @Test
  public void testReadFunctionLiteral() throws PlomTextReader.PlomReadException
  {
    PlomTextReader.StringTextReader inScanner = new PlomTextReader.StringTextReader(
        "1 + lambda { } {\n"
            + " 2 + 3\n"
            + " }\n"
            + " +");
    PlomTextReader.PlomTextScanner reader = new PlomTextReader.PlomTextScanner(inScanner);
    Assert.assertEquals("1", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    Assert.assertEquals("lambda", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("{", reader.lexInput());
    Assert.assertEquals("\n", reader.lexInput());
    Assert.assertEquals("2", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    Assert.assertEquals("3", reader.lexInput());
    Assert.assertEquals("\n", reader.lexInput());
    Assert.assertEquals("}", reader.lexInput());
    Assert.assertEquals("\n", reader.lexInput());
    Assert.assertEquals("+", reader.lexInput());
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader("lambda {} {}\n"
        + "1 + lambda { } {\n"
        + " 2 + 3\n"
        + " } +");
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    StatementContainer container = PlomTextReader.readStatementContainer(lexer);
    
    Assert.assertEquals(
        new StatementContainer(
          new TokenContainer(
              new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                  new TokenContainer(),
                  new StatementContainer()
                  )
          ),
          new TokenContainer(
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
              )
        ),
        container);
  }
}
