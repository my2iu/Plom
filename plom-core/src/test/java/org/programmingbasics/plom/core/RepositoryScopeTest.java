package org.programmingbasics.plom.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.VariableScope;

import junit.framework.TestCase;

public class RepositoryScopeTest extends TestCase
{
  @Test
  public void testSimpleRun() throws IOException, ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
        )));

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("2", Symbol.Number)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable)),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("1", Symbol.Number))

        ));
    class GlobalSaver implements ConfigureGlobalScope
    {
      VariableScope globalScope;
      @Override
      public void configure(VariableScope scope, CoreTypeLibrary coreTypes)
      {
        globalScope = scope;
        StandardLibrary.createGlobals(terp, scope, coreTypes);
        scope.setParent(new RepositoryScope(repository, coreTypes));
      }
    }
    GlobalSaver scopeConfig = new GlobalSaver();
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(3, scopeConfig.globalScope.lookup("b").getNumberValue(), 0.001);
  }
}
