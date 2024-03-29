package org.programmingbasics.plom.core.view;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreterTest;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.StaticMemberSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;

import junit.framework.TestCase;

public class GatherCodeCompletionInfoTest extends TestCase
{
  @Test
  public void testFlatVariables() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF), 
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment)
            // Have an error in this line (missing some tokens)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            )
        );
    CodePosition pos = CodePosition.fromOffsets(1);
    CodeCompletionContext context = codeCompletionForPosition(code, pos);
    Assert.assertNull(context.currentScope().lookupType("b"));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("a"));
    Assert.assertNull(context.getExpectedExpressionType());
    
    pos = CodePosition.fromOffsets(3);
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("a"));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.currentScope().lookupType("b"));
    Assert.assertNull(context.getExpectedExpressionType());
  }

  private CodeCompletionContext codeCompletionForPosition(StatementContainer code, CodePosition pos) throws RunException
  {
    return codeCompletionForPosition(code, null, pos);
  }

  private CodeCompletionContext codeCompletionForPosition(StatementContainer code, String thisTypeString, CodePosition pos) throws RunException
  {
    return codeCompletionForPosition(code, thisTypeString, null, pos);
  }
  
  private CodeCompletionContext codeCompletionForPosition(StatementContainer code, String thisTypeString, ConfigureForCodeCompletion configuration, CodePosition pos) throws RunException    
  {
    CodeCompletionContext.Builder contextBuilder = CodeCompletionContext.builder();
    StandardLibrary.createCoreTypes(contextBuilder.coreTypes());
    SimpleInterpreterTest.TestScopeWithTypes globalScope = new SimpleInterpreterTest.TestScopeWithTypes(contextBuilder.coreTypes());
    contextBuilder.currentScope().setParent(globalScope);
    
    if (configuration != null)
      configuration.configure(contextBuilder, globalScope);
    
    if (thisTypeString != null)
    {
      Value thisValue = new Value();
      thisValue.type = contextBuilder.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(thisTypeString));
      contextBuilder.pushObjectScope(thisValue);
    }
    contextBuilder.pushNewScope();
    
    CodeCompletionContext context = contextBuilder.build();
    GatherCodeCompletionInfo.fromStatements(code, context, pos, 0);
    return context;
  }

  static interface ConfigureForCodeCompletion
  {
    public void configure(CodeCompletionContext.Builder context, SimpleInterpreterTest.TestScopeWithTypes globalScope);
  }
  
  @Test
  public void testLastTypeInExpression() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("Anumber", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("Astring", Symbol.AtType)
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
    Assert.assertNull(context.getExpectedExpressionType());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 1));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 3));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("number", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 4));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertEquals("number", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 5));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("number", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 6));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertEquals("number", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 0));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 1));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 2));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(3, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 0));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 1));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 2));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 4));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(4, 5));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertEquals("string", context.getExpectedExpressionType().name);
  }

  @Test
  public void testLastTypeInBooleanExpression() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@boolean", Symbol.AtType)
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

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 1));
    Assert.assertEquals(context.coreTypes().getBooleanType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 3));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 4));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0, CodePosition.EXPRBLOCK_POS_EXPR, 5));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
  }
  
  @Test
  public void testLastTypeInCall() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("\"hello\"", Symbol.String),
            Token.ParameterToken.fromContents(".substring from:to:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number)),
                new TokenContainer(
                    new Token.SimpleToken("3", Symbol.Number)))));

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 0));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 1));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getExpectedExpressionType());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1, CodePosition.PARAMTOK_POS_EXPRS, 1, 0));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1, CodePosition.PARAMTOK_POS_EXPRS, 1, 1));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getExpectedExpressionType());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertNull(context.getExpectedExpressionType());
  }
  
  @Test
  public void testLastTypeWithParenthesis() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("(", Symbol.OpenParenthesis),
            new Token.SimpleToken("5", Symbol.Number),
            new Token.SimpleToken("<", Symbol.Lt),
            new Token.SimpleToken("5", Symbol.Number),
            new Token.SimpleToken(")", Symbol.ClosedParenthesis),
            Token.ParameterToken.fromContents(".to string", Symbol.DotVariable)));

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 3));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 4));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 5));
    Assert.assertEquals(context.coreTypes().getBooleanType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 6));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
  }

  @Test
  public void testVariablesInBlocks() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("var", Symbol.Var),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//                        new Token.SimpleToken(":", Symbol.Colon),
                        Token.ParameterToken.fromContents("@string", Symbol.AtType)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable))
                    )), 
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@string", Symbol.AtType)
            )
        );
    
    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(2, 0));
    Assert.assertNull(context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("b"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 1));
    Assert.assertTrue(new VariableSuggester(context).gatherSuggestions("").contains("a"));
    Assert.assertFalse(new VariableSuggester(context).gatherSuggestions("").contains("b"));
  }
  
  @Test
  public void testMemberSuggestions() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("Anumber", Symbol.AtType)
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

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 3));
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("substring from:to:"));
    Assert.assertTrue(suggestions.contains("to string"));
  }

  @Test
  public void testStaticMemberSuggestions() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            Token.ParameterToken.fromContents(".parse US number:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("\"123\"", Symbol.String))),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable)
            ));

    CodeCompletionContext context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeForStaticCall());
    List<String> suggestions = new StaticMemberSuggester(context, true, true).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("parse US number:"));
    Assert.assertFalse(suggestions.contains("new"));
    
    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));

    context = codeCompletionForPosition(code, CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));

//    context = codeCompletionForPosition(code, CodePosition.fromOffsets(1, 3));
//    suggestions = new MemberSuggester(context).gatherSuggestions("");
//    Assert.assertTrue(suggestions.contains("substring from:to:"));
//    Assert.assertTrue(suggestions.contains("to string"));
//
  }

  @Test
  public void testConstructorSuggestions() throws RunException
  {
    ConfigureForCodeCompletion configuration = (ctx, globalScope) -> { 
      Type childType = new Type("child", ctx.coreTypes().getObjectType());
      globalScope.addType(childType);
      childType.addStaticMethod("constructor", 
          ExecutableFunction.forCode(CodeUnitLocation.forConstructorMethod("child", "constructor"), null, childType, Optional.empty(), Collections.emptyList()),
          ctx.coreTypes().getVoidType());
      childType.addMethod("testname", 
          ExecutableFunction.forCode(CodeUnitLocation.forConstructorMethod("child", "constructor"), null, childType, Optional.empty(), Collections.emptyList()), 
          ctx.coreTypes().getNumberType());
    };
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@child", Symbol.AtType),
            Token.ParameterToken.fromContents(".constructor", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".testname", Symbol.DotVariable)
            ));

    CodeCompletionContext context = codeCompletionForPosition(code, "object", configuration, CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals("child", context.getLastTypeForStaticCall().name);
    List<String> suggestions = new StaticMemberSuggester(context, true, true).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("constructor"));
    Assert.assertFalse(suggestions.contains("new"));
    
    context = codeCompletionForPosition(code, "object", configuration, CodePosition.fromOffsets(0, 2));
    Assert.assertEquals("child", context.getLastTypeUsed().name);
    Assert.assertNull(context.getLastTypeForStaticCall());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("to string"));
    Assert.assertTrue(suggestions.contains("testname"));
    Assert.assertFalse(suggestions.contains("constructor"));

    context = codeCompletionForPosition(code, "object", configuration, CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));
  }

  @Test
  public void testThis() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("return", Symbol.Return),
            new Token.SimpleToken("this", Symbol.This),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 2));
    List<String> suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 3));
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("abs"));
    Assert.assertTrue(suggestions.contains("floor"));
    Assert.assertTrue(suggestions.contains("ceiling"));
  }
  
  @Test
  public void testSuperForConstructorChaining() throws RunException
  {
    ConfigureForCodeCompletion configuration = (ctx, globalScope) -> { 
      Type childType = new Type("child", ctx.coreTypes().getObjectType());
      globalScope.addType(childType);
      ctx.setDefinedClassOfMethod(childType);
      ctx.setIsConstructorMethod(true);
      ctx.setIsStaticMethod(true);
    };
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("return", Symbol.Return),
            new Token.SimpleToken("super", Symbol.Super),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 2));
    Assert.assertNull(context.getLastTypeUsed());
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("object")), context.getLastTypeForStaticCall());
    List<String> suggestions = new StaticMemberSuggester(context, true, true).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("new"));

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getVoidType(), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
  }

  @Test
  public void testSuperForInstanceMethodChaining() throws RunException
  {
    ConfigureForCodeCompletion configuration = (ctx, globalScope) -> { 
      Type childType = new Type("child", ctx.coreTypes().getObjectType());
      globalScope.addType(childType);
      ctx.setDefinedClassOfMethod(childType);
      ctx.setIsConstructorMethod(false);
      ctx.setIsStaticMethod(false);
    };
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("return", Symbol.Return),
            new Token.SimpleToken("super", Symbol.Super),
            Token.ParameterToken.fromContents(".to string", Symbol.DotVariable)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("object")), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
    List<String> suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("to string"));

    context = codeCompletionForPosition(code, "child", configuration, CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    Assert.assertNull(context.getLastTypeForStaticCall());
  }

  @Test
  public void testFor() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("for", Symbol.COMPOUND_FOR, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("in", Symbol.In),
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable)),
                new StatementContainer(
                    new TokenContainer(new Token.SimpleToken("1", Symbol.Number)))
            ),
            new Token.SimpleToken("2", Symbol.Number)
        ));
    CodeCompletionContext context;
    List<String> suggestions;
    
    // Before the for
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertFalse(suggestions.contains("a"));

    // Start of the for expression
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertNull(context.getLastTypeUsed());
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertFalse(suggestions.contains("a"));

    // After the "in"
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_EXPR, 2));
    Assert.assertNull(context.getLastTypeUsed());
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertFalse(suggestions.contains("a"));

    // Inside the block of the for
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 0));
    Assert.assertNull(context.getLastTypeUsed());
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("a"));

    // After the for
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 1));
    Assert.assertNull(context.getLastTypeUsed());
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertFalse(suggestions.contains("a"));
  }

  @Test
  public void testAs() throws ParseException, RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("5", Symbol.Number),
            Token.ParameterToken.fromContents(".ceiling", Symbol.DotVariable),
            new Token.SimpleToken("as", Symbol.As),
            Token.ParameterToken.fromContents("@string", Symbol.AtType),
            Token.ParameterToken.fromContents(".to string", Symbol.DotVariable)
            )
        );
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 1));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 4));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 5));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
  }

  @Test
  public void testIs() throws ParseException, RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("5", Symbol.Number),
            new Token.SimpleToken("is", Symbol.Is),
            Token.ParameterToken.fromContents("@string", Symbol.AtType),
            new Token.SimpleToken("and", Symbol.And),
            new Token.SimpleToken("true", Symbol.TrueLiteral)
            )
        );
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 1));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getBooleanType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 4));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 5));
    Assert.assertEquals(context.coreTypes().getBooleanType(), context.getLastTypeUsed());
  }

  @Test
  public void testRetype() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("123", Symbol.Number),
            new Token.SimpleToken("retype", Symbol.Retype),
            Token.ParameterToken.fromContents("@string", Symbol.AtType)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 1));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 2));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 3));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    List<String> suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("substring from:to:"));
    Assert.assertTrue(suggestions.contains("to string"));
  }
  
  @Test
  public void testFunctionTypeNoParams() throws RunException
  {
    // Will the methods available on a function type be listed properly?
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@go", Symbol.FunctionTypeName),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@string", Symbol.AtType)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".go", Symbol.DotVariable)
            ));
    
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 0));
    Assert.assertNull(context.getLastTypeUsed());

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 1));
    UnboundType funType = UnboundType.forSimpleFunctionType("string", "go");
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getLastTypeUsed());
    List<String> suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("go"));

    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 2));
    Assert.assertEquals(context.coreTypes().getStringType(), context.getLastTypeUsed());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("substring from:to:"));
  }
  
  @Test
  public void testFunctionLiteralCallParams() throws RunException
  {
    // Will a function literal with multiple parameters be suggested correctly? 
    // Will the arguments within a function literal be suggested correctly?
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call:with:", Symbol.FunctionTypeName,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".argA", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@boolean", Symbol.AtType)),
                new TokenContainer(
                    Token.ParameterToken.fromContents(".argB", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@string", Symbol.AtType))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@call:with:", Symbol.FunctionTypeName,
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg1", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@boolean", Symbol.AtType)),
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg2", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@string", Symbol.AtType))),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)
                    ),
                new StatementContainer(
                    new TokenContainer(
                      new Token.SimpleToken("var", Symbol.Var),
                      Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                      Token.ParameterToken.fromContents("@number", Symbol.AtType)
                    )))
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call:with:", Symbol.DotVariable,
                new TokenContainer(), 
                new TokenContainer())
            ));
        
    CodeCompletionContext context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 0));
    Assert.assertNull(context.getLastTypeUsed());

    // Function literal arguments aren't available outside the function literal
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 1));
    List<String> suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertFalse(suggestions.contains("arg1"));
    Assert.assertFalse(suggestions.contains("arg2"));
    Assert.assertFalse(suggestions.contains("argA"));
    Assert.assertFalse(suggestions.contains("argB"));
    Assert.assertFalse(suggestions.contains("b"));
    Assert.assertTrue(suggestions.contains("a"));
    
    // Function type with parameters handled properly
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 1));
    UnboundType funType = UnboundType.forSimpleFunctionType("number", "call:with:", "boolean", "string");
    UnboundType incorrectFunParamsType = UnboundType.forSimpleFunctionType("number", "call:with:", "number", "string");
    UnboundType incorrectFunParamCountType = UnboundType.forSimpleFunctionType("number", "call:", "boolean");
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getLastTypeUsed());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("call:with:"));
    Assert.assertNotEquals(context.currentScope().typeFromUnboundTypeFromScope(incorrectFunParamCountType), context.getLastTypeUsed());
    Assert.assertNotEquals(context.currentScope().typeFromUnboundTypeFromScope(incorrectFunParamsType), context.getLastTypeUsed());

    // Return type of function type with parameters
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(1, 2));
    Assert.assertEquals(context.coreTypes().getNumberType(), context.getLastTypeUsed());
    suggestions = new MemberSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("-:"));

    // Check that we can have enough type information to
    // suggest a match for the function literal type
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 6));
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getExpectedExpressionType());

    // We need the expected type to push into the lambda too?
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 6, CodePosition.EXPRBLOCK_POS_EXPR, 0));
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getExpectedExpressionType());

    // Types are suggested properly for function literal args 
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 6, CodePosition.EXPRBLOCK_POS_EXPR, 0, CodePosition.PARAMTOK_POS_EXPRS, 0, 1));
    // skip test
    
    // Function literal arguments are suggested
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 6, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 0));
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("arg1"));
    Assert.assertTrue(suggestions.contains("arg2"));
    Assert.assertFalse(suggestions.contains("argA"));
    Assert.assertFalse(suggestions.contains("argB"));
    Assert.assertFalse(suggestions.contains("a"));
    Assert.assertFalse(suggestions.contains("b"));

    // Function literal arguments are suggested
    context = codeCompletionForPosition(code, "number", CodePosition.fromOffsets(0, 6, CodePosition.EXPRBLOCK_POS_BLOCK, 1, 0));
    suggestions = new VariableSuggester(context).gatherSuggestions("");
    Assert.assertTrue(suggestions.contains("arg1"));
    Assert.assertTrue(suggestions.contains("arg2"));
    Assert.assertFalse(suggestions.contains("argA"));
    Assert.assertFalse(suggestions.contains("argB"));
    Assert.assertFalse(suggestions.contains("a"));
    Assert.assertTrue(suggestions.contains("b"));
  }
  
  @Test
  public void testFunctionLiteralWithFunctionLiteralParam() throws RunException
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".argA", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("f@call2:", Symbol.FunctionTypeName,
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg1", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@boolean", Symbol.AtType))
                    ),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                        new TokenContainer(),
                        new StatementContainer()

                    )
                  )
            )));
    
    // Check that function types are suggested when used as a method argument
    CodePosition pos = CodePosition.fromOffsets(1, 1, CodePosition.PARAMTOK_POS_EXPRS, 0);
    CodeCompletionContext context = codeCompletionForPosition(code, pos);
    UnboundType funType = UnboundType.forSimpleFunctionType("number", "call2:", "boolean");
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getExpectedExpressionType());
    
    pos = CodePosition.fromOffsets(1, 1, CodePosition.PARAMTOK_POS_EXPRS, 0, 0, CodePosition.EXPRBLOCK_POS_EXPR, 0);
    context = codeCompletionForPosition(code, pos);
    funType = UnboundType.forSimpleFunctionType("number", "call2:", "boolean");
    Assert.assertEquals(context.currentScope().typeFromUnboundTypeFromScope(funType), context.getExpectedExpressionType());

  }
}
