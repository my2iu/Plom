package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class ParseContextTest extends TestCase
{
  @Test
  public void testEmptyCode()
  {
    StatementContainer container = new StatementContainer();
    CodePosition pos = new CodePosition();
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.FullStatement, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(),
        parseContext.tokens);
  }

  @Test
  public void testIf()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("Z", Symbol.Number)),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                )
            )
        );
    // Inside the block of the if
    CodePosition pos = CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1);
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.FullStatement, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(new Token.SimpleToken("1", Symbol.Number)),
        parseContext.tokens);
    
    // Inside the expression of the if (beginning)
    pos = CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ExpressionOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(),
        parseContext.tokens);

    // Inside the expression of the if (end)
    pos = CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 1);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ExpressionOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(new Token.SimpleToken("A", Symbol.Number)),
        parseContext.tokens);

    // Before the if
    pos = CodePosition.fromOffsets(0, 1);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.FullStatement, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(container.statements.get(0).tokens.get(0)),
        parseContext.tokens);
    
    // After the if
    pos = CodePosition.fromOffsets(1, 1);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.FullStatement, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(container.statements.get(1).tokens.get(0)),
        parseContext.tokens);
  }
  
  @Test
  public void testFor()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("for", Symbol.COMPOUND_FOR, 
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("in", Symbol.In),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                )
            )
        );
    // Inside the block of the for
    CodePosition pos = CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1);
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.FullStatement, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(new Token.SimpleToken("1", Symbol.Number)),
        parseContext.tokens);
    
    // Inside the expression of the for (beginning)
    pos = CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ForExpressionOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(),
        parseContext.tokens);

    // Inside the expression of the for (end)
    pos = CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_EXPR, 3);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ForExpressionOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken("in", Symbol.In),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable)),
        parseContext.tokens);
  }

  @Test
  public void testFunctionType()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("var", Symbol.Var),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                Token.ParameterToken.fromContents("f@call:with:", Symbol.FunctionTypeName,
                    new TokenContainer(
                        Token.ParameterToken.fromContents("@array", Symbol.AtType)),
                    new TokenContainer()
                )
            )
        );
    // Parameter of a function type
    CodePosition pos = CodePosition.fromOffsets(0, 2, CodePosition.PARAMTOK_POS_EXPRS, 0, 0);
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ParameterFieldOptionalNameOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(),
        parseContext.tokens);
    
    pos = CodePosition.fromOffsets(0, 2, CodePosition.PARAMTOK_POS_EXPRS, 0, 1);
    parseContext = ParseContext.findPredictiveParseContextForStatements(container, pos, 0);
    Assert.assertEquals(Symbol.ParameterFieldOptionalNameOnly, parseContext.baseContext);
    Assert.assertEquals(
        Arrays.asList(Token.ParameterToken.fromContents("@array", Symbol.AtType)),
        parseContext.tokens);
  }
}
