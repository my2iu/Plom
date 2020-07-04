package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class ParseToAstTest extends TestCase
{
  @Test
  public void test() throws ParseToAst.ParseException
  {
    StatementContainer container = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("1", Symbol.Number)
            ));
    ParseToAst lineParser = new ParseToAst(container.statements.get(0).tokens, Symbol.EndStatement, null);
    AstNode node = lineParser.parseToEnd(Symbol.Statement);
    Assert.assertNotNull(node);
  }
  
  @Test
  public void testUnexpectedToken()
  {
    TokenContainer line = new TokenContainer(
        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
        new Token.SimpleToken(":=", Symbol.Assignment),
        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("1", Symbol.Number)
        );
    try {
      ParseToAst lineParser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
      AstNode node = lineParser.parseToEnd(Symbol.Expression);
      fail();
    }
    catch (ParseToAst.ParseException e)
    {
      // Ok
    }

  }
}
