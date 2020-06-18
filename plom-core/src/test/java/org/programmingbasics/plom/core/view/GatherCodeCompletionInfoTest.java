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

}
