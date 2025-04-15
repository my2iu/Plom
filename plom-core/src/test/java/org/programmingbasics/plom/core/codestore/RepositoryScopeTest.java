package org.programmingbasics.plom.core.codestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.LogLevel;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.VariableScope;

import junit.framework.TestCase;

public class RepositoryScopeTest extends TestCase
{
  static class GlobalSaver implements ConfigureGlobalScope
  {
    GlobalSaver(SimpleInterpreter terp, ModuleCodeRepository repository, ErrorLogger errLogger) { this.terp = terp; this.repository = repository; this.errLogger = errLogger; }
    SimpleInterpreter terp;
    ModuleCodeRepository repository;
    ErrorLogger errLogger;
    
    VariableScope globalScope;
    @Override
    public void configure(VariableScope scope, CoreTypeLibrary coreTypes)
    {
      globalScope = scope;
      StandardLibrary.createGlobals(terp, scope, coreTypes);
      scope.setParent(new RepositoryScope(repository, coreTypes, errLogger));
    }
  }

  static class ErrorLoggerSaver extends SimpleInterpreter.ErrorLogger
  {
    List<Object> errs = new ArrayList<>();
    @Override public void error(Object errObj, ProgramCodeLocation location) { errs.add(errObj); }
    @Override public void debugLog(Object value) { errs.add(value); }
    @Override public void warn(Object errObj) { errs.add(errObj); }
    @Override public void log(String msg, LogLevel logLevel, ProgramCodeLocation location) { errs.add(msg); }
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
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
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
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
    terp.runNoReturn(scopeConfig);
    
    Type fnType = scopeConfig.globalScope.lookupType("b");
    Type fnTypeWithNames = scopeConfig.globalScope.lookupType("c");
    Assert.assertTrue(fnType instanceof Type.LambdaFunctionType);
    Assert.assertEquals("number", ((Type.LambdaFunctionType)fnType).returnType.name);
    Assert.assertEquals(0, ((Type.LambdaFunctionType)fnType).args.size());
    Assert.assertTrue(fnTypeWithNames instanceof Type.LambdaFunctionType);
    Assert.assertEquals("boolean", ((Type.LambdaFunctionType)fnTypeWithNames).returnType.name);
    Assert.assertEquals(1, ((Type.LambdaFunctionType)fnTypeWithNames).args.size());
    Assert.assertEquals("number", ((Type.LambdaFunctionType)fnTypeWithNames).args.get(0).name);
  }
  
  @Test
  public void testBadFunctionSignatureBadReturnType() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "get"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));
    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("unknown type"), "bad return"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("null", Symbol.NullLiteral)))));

    // Run some code
    try {
      SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
          new TokenContainer(ParameterToken.fromContents(".get", Symbol.DotVariable)),
          new TokenContainer(ParameterToken.fromContents(".bad return", Symbol.DotVariable))
          ));
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
      terp.runNoReturn(scopeConfig);
    }
    catch (RunException e)
    {
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertNull(e.getErrorLocation().getPosition());
      Assert.assertEquals("bad return", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals("Function signature is invalid", e.getMessage());
      Assert.assertEquals("Problem with return type", e.getCause().getMessage());
      Assert.assertEquals("Unknown class: @unknown type", e.getCause().getCause().getMessage());
      Assert.assertNull(e.getCause().getCause().getCause());
      return;
    }
    Assert.fail("Expecting error in function signature");
  }
  
  @Test
  public void testBadFunctionSignatureBadArgType() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(new TokenContainer(ParameterToken.fromContents("@number", Symbol.AtType)), Arrays.asList("test"), Arrays.asList(new TokenContainer(ParameterToken.fromContents(".asdf1", Symbol.DotVariable), ParameterToken.fromContents("@asdf2", Symbol.AtType))), null),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));

    // Run some code
    try {
      SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
          new TokenContainer(ParameterToken.fromContents(".test:", Symbol.DotVariable, new TokenContainer(new Token.SimpleToken("3", Symbol.Number))))
          ));
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
      terp.runNoReturn(scopeConfig);
    }
    catch (RunException e)
    {
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertNull(e.getErrorLocation().getPosition());
      Assert.assertEquals("test:", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals("Function signature is invalid", e.getMessage());
      Assert.assertEquals("Problem with function argument 1", e.getCause().getMessage());
      Assert.assertEquals("Unknown class: @asdf2", e.getCause().getCause().getMessage());
      Assert.assertNull(e.getCause().getCause().getCause());
      return;
    }
    Assert.fail("Expecting error in function signature");
  }

  @Test
  public void testBadFunctionSignatureBadArgParse() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(new TokenContainer(ParameterToken.fromContents("@number", Symbol.AtType)), Arrays.asList("test"), Arrays.asList(new TokenContainer(ParameterToken.fromContents(".asdf1", Symbol.DotVariable), ParameterToken.fromContents(".asdf2", Symbol.DotVariable))), null),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));

    // Run some code
    try {
      SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
          new TokenContainer(ParameterToken.fromContents(".test:", Symbol.DotVariable, new TokenContainer(new Token.SimpleToken("3", Symbol.Number))))
          ));
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
      terp.runNoReturn(scopeConfig);
    }
    catch (RunException e)
    {
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertNull(e.getErrorLocation().getPosition());
      Assert.assertEquals("test:", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals("Function signature is invalid", e.getMessage());
      Assert.assertEquals("Problem with argument 1", e.getCause().getMessage());
      Assert.assertNull(e.getCause().getCause());
      return;
    }

    // TODO: Track parse errors in function signature
    Assert.fail("Expecting error in function signature");
  }

  @Test
  public void testBadGlobalVariables() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            new Token.SimpleToken("var", Symbol.Var)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@badtype", Symbol.AtType)
            )
        ));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        ));
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(3, errLogger.errs.size());
    
    Assert.assertEquals("Could not determine type for global variable b", ((Throwable)errLogger.errs.get(0)).getMessage());
    Assert.assertEquals("Unknown class: @badtype", ((Throwable)errLogger.errs.get(0)).getCause().getMessage());
    // TODO: Track down why there are two var errors 
    Assert.assertEquals("Problem with variable declarations", ((Throwable)errLogger.errs.get(1)).getMessage());
    Assert.assertEquals("Problem with variable declarations", ((Throwable)errLogger.errs.get(2)).getMessage());
  }

  @Test
  public void testBadClassMemberVariables() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("classB"));
    classA.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            new Token.SimpleToken("var", Symbol.Var)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@badtype", Symbol.AtType)
            )
        ));
    ClassDescription classB = repository.addClass("classB");
    classB.setSuperclass(UnboundType.forClassLookupName("object"));
    classB.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@badtype2", Symbol.AtType)
            )
        ));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@classA", Symbol.AtType)
            )
        ));
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(5, errLogger.errs.size());
    
    Assert.assertEquals("Could not determine type for member variable c", ((Throwable)errLogger.errs.get(0)).getMessage());
    Assert.assertEquals("classB", ((RunException)errLogger.errs.get(0)).getErrorLocation().getClassName());
    Assert.assertEquals("Unknown class: @badtype2", ((Throwable)errLogger.errs.get(0)).getCause().getMessage());
    Assert.assertEquals("Could not determine type for member variable c", ((Throwable)errLogger.errs.get(1)).getMessage());
    Assert.assertEquals("classB", ((RunException)errLogger.errs.get(1)).getErrorLocation().getClassName());
    Assert.assertEquals("Unknown class: @badtype2", ((Throwable)errLogger.errs.get(1)).getCause().getMessage());
    Assert.assertEquals("Could not determine type for member variable b", ((Throwable)errLogger.errs.get(2)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(2)).getErrorLocation().getClassName());
    Assert.assertEquals("Unknown class: @badtype", ((Throwable)errLogger.errs.get(2)).getCause().getMessage());
    // TODO: Track down why there are two var errors 
    Assert.assertEquals("Problem with member variable declarations", ((Throwable)errLogger.errs.get(3)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(3)).getErrorLocation().getClassName());
    Assert.assertEquals("Problem with member variable declarations", ((Throwable)errLogger.errs.get(4)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(4)).getErrorLocation().getClassName());
  }

  @Test
  public void testBadClassMethodSignature() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("object"));
    
    FunctionSignature sig = FunctionSignature.from(UnboundType.forClassLookupName("badType"), "badf");
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer()));
    
    sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), "construct:", 
        Arrays.asList("arg"), Arrays.asList(UnboundType.forClassLookupName("badtype2")));
    sig.setIsConstructor(true);
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer()));
        
    sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), "f");
    sig.setIsStatic(true);
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer()));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@classA", Symbol.AtType),
            Token.ParameterToken.fromContents(".f", Symbol.DotVariable)
            )
        ));
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(2, errLogger.errs.size());
    
    Assert.assertEquals("Method signature is invalid", ((Throwable)errLogger.errs.get(0)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(0)).getErrorLocation().getClassName());
    Assert.assertEquals("badf", ((RunException)errLogger.errs.get(0)).getErrorLocation().getFunctionMethodName());
    Assert.assertFalse(((RunException)errLogger.errs.get(0)).getErrorLocation().isStatic());
    Assert.assertEquals("Problem with return type", ((Throwable)errLogger.errs.get(0)).getCause().getMessage());
    Assert.assertEquals("Unknown class: @badType", ((Throwable)errLogger.errs.get(0)).getCause().getCause().getMessage());

    Assert.assertEquals("Method signature is invalid", ((Throwable)errLogger.errs.get(1)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(1)).getErrorLocation().getClassName());
    Assert.assertEquals("construct:", ((RunException)errLogger.errs.get(1)).getErrorLocation().getFunctionMethodName());
    Assert.assertTrue(((RunException)errLogger.errs.get(1)).getErrorLocation().isStatic());
    Assert.assertEquals("Problem with function argument 1", ((Throwable)errLogger.errs.get(1)).getCause().getMessage());
    Assert.assertEquals("Unknown class: @badtype2", ((Throwable)errLogger.errs.get(1)).getCause().getCause().getMessage());
  }

  @Test
  public void testFunctionParseException() throws ParseException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "test"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("var", Symbol.Var)))));

    // Run some code
    try {
      SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
          new TokenContainer(ParameterToken.fromContents(".test", Symbol.DotVariable))
          ));
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, null);
      terp.runNoReturn(scopeConfig);
    }
    catch (RunException e)
    {
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertEquals(CodePosition.fromOffsets(0, 1), e.getErrorLocation().getPosition());
      Assert.assertEquals("test", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals("Syntax error in function code", e.getMessage());
      Assert.assertNull(e.getCause().getMessage());
      return;
    }
    Assert.fail("Expecting error in function signature");
  }

  @Test
  public void testBadClassMethodParseException() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("object"));
    
    FunctionSignature sig = FunctionSignature.from(UnboundType.forClassLookupName("number"), "badf");
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer(
            new TokenContainer(),
            new TokenContainer(
                new Token.SimpleToken("var", Symbol.Var)
                ))));
    
    sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), "construct:", 
        Arrays.asList("arg"), Arrays.asList(UnboundType.forClassLookupName("badtype2")));
    sig.setIsConstructor(true);
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer()));
        
    sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), "f");
    sig.setIsStatic(true);
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer()));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@classA", Symbol.AtType)
            )
        ));
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
    terp.runNoReturn(scopeConfig);
    
    Assert.assertEquals(2, errLogger.errs.size());
    
    Assert.assertEquals("Method signature is invalid", ((Throwable)errLogger.errs.get(0)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(0)).getErrorLocation().getClassName());
    Assert.assertEquals("badf", ((RunException)errLogger.errs.get(0)).getErrorLocation().getFunctionMethodName());
    Assert.assertFalse(((RunException)errLogger.errs.get(0)).getErrorLocation().isStatic());
    Assert.assertEquals(CodePosition.fromOffsets(1, 0), ((RunException)errLogger.errs.get(0)).getErrorLocation().getPosition());

    Assert.assertEquals("Method signature is invalid", ((Throwable)errLogger.errs.get(1)).getMessage());
    Assert.assertEquals("classA", ((RunException)errLogger.errs.get(1)).getErrorLocation().getClassName());
    Assert.assertEquals("construct:", ((RunException)errLogger.errs.get(1)).getErrorLocation().getFunctionMethodName());
    Assert.assertTrue(((RunException)errLogger.errs.get(1)).getErrorLocation().isStatic());
    Assert.assertEquals("Problem with function argument 1", ((Throwable)errLogger.errs.get(1)).getCause().getMessage());
    Assert.assertEquals("Unknown class: @badtype2", ((Throwable)errLogger.errs.get(1)).getCause().getCause().getMessage());
  }
  
  @Test
  public void testMissingMethodFromLambdaInStaticMethodException() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("object"));
    
    FunctionSignature sig = FunctionSignature.from(UnboundType.forSimpleFunctionType("void", "has bad method call"), "make lambda");
    sig.isStatic = true;
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer(
            new TokenContainer(),
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                    new TokenContainer(
                        Token.ParameterToken.fromContents("f@has bad method call", Symbol.FunctionTypeName),
                        new Token.SimpleToken("returns", Symbol.Returns),
                        Token.ParameterToken.fromContents("@void", Symbol.AtType)),
                    new StatementContainer(
                        new TokenContainer(
                            new Token.SimpleToken("hello", Symbol.String),
                            Token.ParameterToken.fromContents(".missing method", Symbol.DotVariable)
                            )
                        ))
                ))));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@classA", Symbol.AtType),
            Token.ParameterToken.fromContents(".make lambda", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".has bad method call", Symbol.DotVariable)
            )
        ));
    try {
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
      terp.runNoReturn(scopeConfig);
    
    }
    catch (RunException e)
    {
      Assert.assertEquals(0, errLogger.errs.size());

      Assert.assertEquals("Cannot find method @string .missing method", e.getMessage());
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertEquals("classA", e.getErrorLocation().getClassName());
      Assert.assertEquals("make lambda", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(1, 1, CodePosition.EXPRBLOCK_POS_BLOCK, 0, 1), e.getErrorLocation().getPosition());
      Assert.assertNull(e.getCause());
      return;
    }
    Assert.fail("Expecting error when running code");

  }

  @Test
  public void testMissingStaticMethodFromFunctionException() throws ParseException, RunException
  {
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("object"));

    repository.addFunction(new FunctionDescription(
        FunctionSignature.from(new TokenContainer(ParameterToken.fromContents("@void", Symbol.AtType)), Arrays.asList("test"), Collections.emptyList(), null),
        new StatementContainer(
            new TokenContainer(),
            new TokenContainer(
                ParameterToken.fromContents("@classA", Symbol.AtType),
                ParameterToken.fromContents(".go", Symbol.DotVariable)
                ))));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".test", Symbol.DotVariable)
            )
        ));
    try {
      GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
      terp.runNoReturn(scopeConfig);
    
    }
    catch (RunException e)
    {
      Assert.assertEquals(0, errLogger.errs.size());

      Assert.assertEquals("Cannot find static method with the name .go", e.getMessage());
      Assert.assertNotNull(e.getErrorLocation());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertEquals("test", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(1, 1), e.getErrorLocation().getPosition());
      Assert.assertNull(e.getCause());
      return;
    }
    Assert.fail("Expecting error when running code");

  }

  @Test
  public void testConstructor() throws ParseException, RunException
  {
    // Constructors will often have an empty or invalid return type,
    // and it shouldn't matter since the return type is not used
    
    ModuleCodeRepository repository = new ModuleCodeRepository();
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    ClassDescription classA = repository.addClass("classA");
    classA.setSuperclass(UnboundType.forClassLookupName("object"));
    
    FunctionSignature sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), "make");
    sig.setReturnTypeCode(new TokenContainer(), null);
    sig.isConstructor = true;
    classA.addMethod(new FunctionDescription(
        sig, 
        new StatementContainer(
            new TokenContainer(),
            new TokenContainer(
                new Token.SimpleToken("super", Symbol.Super),
                Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                ))));
    ErrorLoggerSaver errLogger = new ErrorLoggerSaver();

    // Run some code
    SimpleInterpreter terp = new SimpleInterpreter(new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@classA", Symbol.AtType),
            Token.ParameterToken.fromContents(".make", Symbol.DotVariable)
            )
        ));
    GlobalSaver scopeConfig = new GlobalSaver(terp, repository, errLogger);
    terp.runNoReturn(scopeConfig);
    
    // No errors expected
    Assert.assertEquals(0, errLogger.errs.size());
  }
  
}
