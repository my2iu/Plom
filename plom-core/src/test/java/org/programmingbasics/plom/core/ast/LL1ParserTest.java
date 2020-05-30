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
      Assert.assertEquals(Symbol.MemberExpressionMore, parser.stack.get(parser.stack.size() - 1));
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

   @Test
   public void testStatementComment()
   {
      LL1Parser parser = new LL1Parser();
      parser.stack.add(Symbol.FullStatement);
      
      new SimpleToken("", Symbol.DUMMY_COMMENT).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.StatementOrEmpty, parser.stack.get(parser.stack.size() - 1));
      Assert.assertEquals(Symbol.EndStatement, parser.stack.get(parser.stack.size() - 2));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));
   }

   @Test
   public void testAllowedSymbolExpression()
   {
     LL1Parser parser = new LL1Parser();
     parser.stack.add(Symbol.EndStatement);
     parser.stack.add(Symbol.Expression);
     
     new SimpleToken("", Symbol.Number).visit(parser);
     Assert.assertEquals(false, parser.isError);
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Plus));
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Divide));
   }
}
