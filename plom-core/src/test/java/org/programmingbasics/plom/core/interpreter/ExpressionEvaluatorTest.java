package org.programmingbasics.plom.core.interpreter;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class ExpressionEvaluatorTest extends TestCase
{
  @Test
  public void testNumber() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parse(Symbol.Expression);
    Value val = ExpressionEvaluator.eval(parsed);
    Assert.assertEquals(Type.NUMBER, val.type);
    Assert.assertEquals(Double.valueOf(1), val.val);
  }
  
  @Test
  public void testNumberAdd() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("2.5", Symbol.Number));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parse(Symbol.Expression);
    Value val = ExpressionEvaluator.eval(parsed);
    Assert.assertEquals(Type.NUMBER, val.type);
    Assert.assertEquals(Double.valueOf(0.5), val.val);
  }
  
  @Test
  public void testMathExpressionOrderOfOperations() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("*", Symbol.Multiply),
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("/", Symbol.Divide),
        new Token.SimpleToken("(", Symbol.OpenParenthesis),
        new Token.SimpleToken("4", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("5", Symbol.Number),
        new Token.SimpleToken(")", Symbol.ClosedParenthesis));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parse(Symbol.Expression);
    Value val = ExpressionEvaluator.eval(parsed);
    Assert.assertEquals(Type.NUMBER, val.type);
    Assert.assertEquals(Double.valueOf(-5), val.val);
  }

  @Test
  public void testString() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("\"hello \"", Symbol.String),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("\"world\"", Symbol.String));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parse(Symbol.Expression);
    Value val = ExpressionEvaluator.eval(parsed);
    Assert.assertEquals(Type.STRING, val.type);
    Assert.assertEquals("hello world", val.val);
  }

}
