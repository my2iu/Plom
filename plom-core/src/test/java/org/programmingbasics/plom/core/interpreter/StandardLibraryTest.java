package org.programmingbasics.plom.core.interpreter;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreterTest.GlobalsSaver;

import junit.framework.TestCase;

public class StandardLibraryTest extends TestCase
{
  @Test
  public void testObjectArray() throws RunException, ParseException
  {
    // Create a plain object
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getObjectArrayType(), coreTypes.getNullValue());
      scope.addVariable("b", coreTypes.getNumberType(), coreTypes.getNullValue());
      scope.addVariable("c", coreTypes.getNumberType(), coreTypes.getNullValue());
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@object array", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".append:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number)
                    ))),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".at:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("0", Symbol.Number)
                    ))),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".at:set:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("0", Symbol.Number)),
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    Token.ParameterToken.fromContents(".size", Symbol.DotVariable)
                    ))
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".at:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("0", Symbol.Number)
                    )))
            );
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(vars.coreTypes.getObjectArrayType(), vars.globalScope.lookup("a").type);
    Assert.assertEquals(vars.coreTypes.getNumberType(), vars.globalScope.lookup("b").type);
    Assert.assertEquals(2, vars.globalScope.lookup("b").getNumberValue(), 0.001);
    Assert.assertEquals(vars.coreTypes.getNumberType(), vars.globalScope.lookup("c").type);
    Assert.assertEquals(1, vars.globalScope.lookup("c").getNumberValue(), 0.001);
  }
}
