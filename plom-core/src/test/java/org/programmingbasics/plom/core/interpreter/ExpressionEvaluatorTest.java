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
  public void testNumber() throws ParseException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parse(Symbol.Expression);
    Value val = ExpressionEvaluator.eval(parsed);
    Assert.assertNotNull(val);
  }
  
}
