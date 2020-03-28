package org.programmingbasics.plom.core.ast;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;


public class LL1ParserTest extends TestCase
{
   @Test
   public void testBasic()
   {
      LL1Parser parser = new LL1Parser();
      parser.stack.add(Symbol.Statement);
      
      new SimpleToken("", Symbol.Number).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.AdditiveExpressionMore, parser.stack.get(parser.stack.size() - 1));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Plus));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Minus));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));

      new SimpleToken("", Symbol.Plus).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.AdditiveExpression, parser.stack.get(parser.stack.size() - 1));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Number));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.String));
      Assert.assertTrue(!parser.allowedNextSymbols().contains(Symbol.EndStatement));

   }

}
