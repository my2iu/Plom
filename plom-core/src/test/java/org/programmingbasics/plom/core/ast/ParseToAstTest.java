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
  
  @Test
  public void testFor() throws ParseToAst.ParseException
  {
    TokenContainer line = new TokenContainer(
        new Token.OneExpressionOneBlockToken("for", Symbol.COMPOUND_FOR, 
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken("in", Symbol.In),
                Token.ParameterToken.fromContents(".b", Symbol.DotVariable)),
            new StatementContainer(
                new TokenContainer(new Token.SimpleToken("1", Symbol.Number)))
        )
    );
    ParseToAst lineParser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    AstNode node = lineParser.parseToEnd(Symbol.Statement);
    AstNode nodeForFor = node.children.get(0).children.get(0);
    Assert.assertEquals(2, nodeForFor.internalChildren.size());
    Assert.assertEquals(4, nodeForFor.internalChildren.get(0).children.size());
    Assert.assertEquals(5, nodeForFor.internalChildren.get(0).symbols.size());
  }
  
//  @Test
//  public void testAs() throws ParseToAst.ParseException
//  {
//    TokenContainer line = new TokenContainer(
//        new Token.SimpleToken("5", Symbol.Number),
//        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable),
//        new Token.SimpleToken("as", Symbol.As),
//        Token.ParameterToken.fromContents("@string", Symbol.AtType),
//        Token.ParameterToken.fromContents(".+:", Symbol.DotVariable,
//            new TokenContainer(
//                new Token.SimpleToken("\"2\"", Symbol.String)
//                )
//        )
//    );
//    ParseToAst lineParser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
//    AstNode node = lineParser.parseToEnd(Symbol.Statement);
//    System.out.println(node.getDebugTreeString(0));
//    Assert.fail()
//  }
}
