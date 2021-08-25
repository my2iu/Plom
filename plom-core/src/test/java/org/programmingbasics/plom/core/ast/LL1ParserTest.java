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
      parser.addToParse(Symbol.Statement);
      
      new SimpleToken("", Symbol.Number).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.MemberExpressionMore, parser.topOfParsingStack());
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Plus));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Minus));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));

      new SimpleToken("", Symbol.Plus).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.MultiplicativeExpression, parser.topOfParsingStack());
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Number));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.String));
      Assert.assertTrue(!parser.allowedNextSymbols().contains(Symbol.EndStatement));
   }

   @Test
   public void testStatementComment()
   {
      LL1Parser parser = new LL1Parser();
      parser.addToParse(Symbol.FullStatement);
      
      new SimpleToken("", Symbol.DUMMY_COMMENT).visit(parser);
      Assert.assertEquals(false, parser.isError);
      Assert.assertEquals(Symbol.StatementOrEmpty, parser.topOfParsingStack());
      Assert.assertEquals(Symbol.EndStatement, parser.nextOnParsingStack(1));
      Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));
   }

   @Test
   public void testAllowedSymbolExpression()
   {
     LL1Parser parser = new LL1Parser();
     parser.addToParse(Symbol.EndStatement);
     parser.addToParse(Symbol.Expression);
     
     new SimpleToken("", Symbol.Number).visit(parser);
     Assert.assertEquals(false, parser.isError);
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.EndStatement));
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Plus));
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Divide));
   }
   
   @Test
   public void testExpandedSymbols()
   {
     LL1Parser parser = new LL1Parser();
     parser.addToParse(Symbol.FullStatement);
     new SimpleToken("", Symbol.COMPOUND_IF).visit(parser);
     Assert.assertTrue(parser.peekExpandedSymbols(Symbol.COMPOUND_ELSE).contains(Symbol.AfterIf));
   }
   
   @Test
   public void testExpandedSymbolsDotVariable()
   {
     LL1Parser parser = new LL1Parser();
     parser.addToParse(Symbol.FullStatement);
     new SimpleToken("", Symbol.Var).visit(parser);
     Assert.assertTrue(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotDeclareIdentifier));
     Assert.assertFalse(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotMember));
     new SimpleToken("", Symbol.DotVariable).visit(parser);
     new SimpleToken("", Symbol.Colon).visit(parser);
     new SimpleToken("", Symbol.AtType).visit(parser);
     new SimpleToken("", Symbol.Assignment).visit(parser);
     Assert.assertFalse(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotDeclareIdentifier));
     Assert.assertFalse(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotMember));
     new SimpleToken("", Symbol.DotVariable).visit(parser);
     Assert.assertFalse(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotDeclareIdentifier));
     Assert.assertTrue(parser.peekExpandedSymbols(Symbol.DotVariable).contains(Symbol.DotMember));
   }
   
   @Test
   public void testAndOr()
   {
     // Checking a problem with the grammar for expressions mixing "and" and "or"
     LL1Parser parser = new LL1Parser();
     parser.addToParse(Symbol.FullStatement);
     new SimpleToken("true", Symbol.TrueLiteral).visit(parser);
     new SimpleToken("or", Symbol.Or).visit(parser);
     new SimpleToken("true", Symbol.TrueLiteral).visit(parser);
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.Or));
     Assert.assertTrue(parser.allowedNextSymbols().contains(Symbol.And));
     Assert.assertTrue(parser.peekParseSymbol(Symbol.And));

   }
}
