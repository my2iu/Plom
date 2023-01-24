package org.programmingbasics.plom.core;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.Type;

import junit.framework.TestCase;

public class CodeWidgetBaseTest extends TestCase
{
  @Test
  public void testMakeTokensForType()
  {
    Type.LambdaFunctionType type = new Type.LambdaFunctionType("call:", new Type("number"), Arrays.asList(".arg1"), Arrays.asList(new Type("string")));
    List<Token> result = CodeWidgetBase.makeTokensForType(type); 
    Assert.assertEquals(
        Arrays.asList(
            Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".arg1", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@string", Symbol.AtType)
                    )),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ), 
        result);
  }
}
