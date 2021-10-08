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
      scope.addVariable("a", coreTypes.getNullType(), coreTypes.getNullValue());
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@object array", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(vars.coreTypes.getObjectArrayType(), vars.globalScope.lookup("a").type);
  }
}
