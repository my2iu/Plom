package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;

import junit.framework.TestCase;

public class GatherCodeCompletionInfoTest extends TestCase
{
  @Test
  public void testFlatVariables()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF), 
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment)
            // Have an error in this line (missing some tokens)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            )
        );
    CodePosition pos = CodePosition.fromOffsets(1);
    CodeCompletionContext context = new CodeCompletionContext();
    context.pushNewScope();
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    Assert.assertNull(context.currentScope().lookupType("b"));
    Assert.assertEquals(Type.NUMBER, context.currentScope().lookupType("a"));
    
    pos = CodePosition.fromOffsets(3);
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    Assert.assertEquals(Type.NUMBER, context.currentScope().lookupType("a"));
    Assert.assertEquals(Type.NUMBER, context.currentScope().lookupType("b"));
  }
  
  private CodeCompletionContext codeCompletionForPosition(StatementContainer code, CodePosition pos)
  {
    CodeCompletionContext context = new CodeCompletionContext();
    context.pushNewScope();
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    return context;
  }
  
  @Test
  public void testLastTypeInExpression()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".string", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF), 
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("3", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("3", Symbol.Number),
            new Token.SimpleToken("-", Symbol.Minus),
            new Token.SimpleToken("2", Symbol.Number)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("\"a\"", Symbol.String)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"a\"", Symbol.String)
            )
        );

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0));
    Assert.assertNull(context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 1));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 2));
    Assert.assertEquals(Type.NUMBER, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 3));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 4));
    Assert.assertEquals(Type.NUMBER, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 5));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 6));
    Assert.assertEquals(Type.NUMBER, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 1));
    Assert.assertEquals(Type.STRING, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 3));
    Assert.assertEquals(Type.STRING, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 1));
    Assert.assertEquals(Type.STRING, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 3));
    Assert.assertEquals(Type.STRING, context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 4));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 5));
    Assert.assertEquals(Type.STRING, context.getLastTypeUsed());
  }

}
