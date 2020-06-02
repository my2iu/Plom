package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
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
    ParseToAst lineParser = new ParseToAst(container.statements.get(0).tokens, Symbol.EndStatement);
    AstNode node = lineParser.parse(Symbol.Statement);
    Assert.assertNotNull(node);
  }
}
