package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
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
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.VariableScope;

import junit.framework.TestCase;

public class RepositoryScopeTest extends TestCase
{
  static class GlobalSaver implements ConfigureGlobalScope
  {
    GlobalSaver(SimpleInterpreter terp, ModuleCodeRepository repository) { this.terp = terp; this.repository = repository; }
    SimpleInterpreter terp;
    ModuleCodeRepository repository;
    
    VariableScope globalScope;
    @Override
    public void configure(VariableScope scope, CoreTypeLibrary coreTypes)
    {
      globalScope = scope;
      StandardLibrary.createGlobals(terp, scope, coreTypes);
      scope.setParent(new RepositoryScope(repository, coreTypes));
    }
  }

  
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
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository);
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(3, scopeConfig.globalScope.lookup("b").getNumberValue(), 0.001);
  }
  
  @Test
  public void testFunctionType() throws IOException, ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@get number value", Symbol.FunctionTypeName),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
        ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@number transform:", Symbol.FunctionTypeName, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".param", Symbol.DotVariable), 
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)
                )),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents(".return", Symbol.DotVariable), 
            Token.ParameterToken.fromContents("@boolean", Symbol.AtType))));
    
    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer());
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository);
    terp.runNoReturn(scopeConfig);
    
    Type fnType = scopeConfig.globalScope.lookupType("b");
    Type fnTypeWithNames = scopeConfig.globalScope.lookupType("c");
    Assert.assertTrue(fnType instanceof Type.FunctionType);
    Assert.assertEquals("number", ((Type.FunctionType)fnType).returnType.name);
    Assert.assertEquals(0, ((Type.FunctionType)fnType).args.size());
    Assert.assertTrue(fnTypeWithNames instanceof Type.FunctionType);
    Assert.assertEquals("boolean", ((Type.FunctionType)fnTypeWithNames).returnType.name);
    Assert.assertEquals(1, ((Type.FunctionType)fnTypeWithNames).args.size());
    Assert.assertEquals("number", ((Type.FunctionType)fnTypeWithNames).args.get(0).name);
  }  
}
