package org.programmingbasics.plom.core.view;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;

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
    StandardLibrary.createCoreTypes(context.coreTypes()); 
    context.pushNewScope();
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    Assert.assertNull(context.currentScope().lookupType("b"));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("a"));
    
    pos = CodePosition.fromOffsets(3);
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("a"));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("b"));
  }
  
  private CodeCompletionContext codeCompletionForPosition(StatementContainer code, CodePosition pos)
  {
    CodeCompletionContext context = new CodeCompletionContext();
    StandardLibrary.createCoreTypes(context.coreTypes());
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
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 3));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 4));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 5));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 6));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 1));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 1));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 4));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 5));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
  }

  @Test
  public void testLastTypeInBooleanExpression()
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
            Token.ParameterToken.fromContents(".boolean", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                    new Token.SimpleToken("and", Symbol.And),
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("=", Symbol.Gt),
                    new Token.SimpleToken("5", Symbol.Number)
                    ),
                new StatementContainer())
            )
        );

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 1));
    Assert.assertEquals(context.coreTypes().getBooleanType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 3));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 4));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 5));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
  }

  @Test
  public void testVariablesInBlocks()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("var", Symbol.Var),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                        new Token.SimpleToken(":", Symbol.Colon),
                        Token.ParameterToken.fromContents(".string", Symbol.DotVariable)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable))
                    )), 
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".string", Symbol.DotVariable)
            )
        );
    
    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0));
    Assert.assertNull(context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 1));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));
  }
  
  @Test
  public void testMemberSuggestions()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".to string", Symbol.DotVariable)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0));
    Assert.assertNull(context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 1));
    List<String> suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 2));
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("substring from:to:"));
    Assert.assertTrue(suggestions.contains("to string"));
  }
}
