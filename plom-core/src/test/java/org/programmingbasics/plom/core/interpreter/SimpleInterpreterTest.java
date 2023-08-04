package org.programmingbasics.plom.core.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.MachineContext.PrimitiveBlockingFunctionReturn;

import elemental.util.ArrayOf;
import elemental.util.impl.JreArrayOf;
import junit.framework.TestCase;

public class SimpleInterpreterTest extends TestCase
{
  public static class TestScopeWithTypes extends VariableScope
  {
    public TestScopeWithTypes(CoreTypeLibrary coreTypes)
    {
      this.coreTypes = coreTypes;
    }
    CoreTypeLibrary coreTypes;
    Map<String, Type> typeLookup = new HashMap<>(); 

    public void addType(Type t)
    {
      typeLookup.put(t.name, t);
    }
    
    @Override
    public Type typeFromUnboundType(UnboundType unboundType, VariableScope subTypeCreator) throws RunException
    {
      if (unboundType.mainToken.type == Symbol.FunctionTypeName) 
      {
        return helperFunctionTypeFromUnboundType(unboundType, subTypeCreator);
      }

      String name = unboundType.mainToken.getLookupName();
      Type toReturn = typeLookup.get(name);
      if (toReturn != null) return toReturn;
      switch (name)
      {
        case "number": return coreTypes.getNumberType(); 
        case "string": return coreTypes.getStringType(); 
        case "boolean": return coreTypes.getBooleanType(); 
        case "object": return coreTypes.getObjectType();
        case "null": return coreTypes.getNullType(); 
        case "object array": return coreTypes.getObjectArrayType();
        case "void": return coreTypes.getVoidType(); 
      }
      throw new RunException("Unknown class @" + name);
    }
  }
  
  static interface ConfigureTestGlobalScope
  {
    public void configure(TestScopeWithTypes scope, CoreTypeLibrary coreTypes);
  }
  
  static class GlobalsSaver implements ConfigureGlobalScope
  {
    GlobalsSaver(ConfigureTestGlobalScope passthrough)
    {
      this.passthrough = passthrough;
    }
    ConfigureTestGlobalScope passthrough;
    TestScopeWithTypes globalScope;
    CoreTypeLibrary coreTypes;
    
    @Override public void configure(VariableScope scope, CoreTypeLibrary coreTypes)
    {
      this.globalScope = new TestScopeWithTypes(coreTypes);
      scope.setParent(this.globalScope);
      this.coreTypes = coreTypes;
      if (passthrough != null)
        passthrough.configure(this.globalScope, coreTypes);
    }
  }
  
  @Test
  public void testStatements() throws ParseException, RunException
  {
    // Set-up some variables and functions
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    class CaptureFunction implements PrimitiveFunction {
      Value captured;
      @Override public Value call(List<Value> args)
      {
        Assert.assertEquals(1, args.size());
        captured = args.get(0);
        return Value.createNumberValue(coreTypes, 32);
      }
    }
    CaptureFunction fun = new CaptureFunction();
    Value aVal = Value.create(fun, Type.makePrimitiveFunctionType(coreTypes.getNumberType(), coreTypes.getStringType()));
    scope.addVariable("a:", aVal.type, aVal);
    scope.addVariable("b", coreTypes.getStringType(), Value.createStringValue(coreTypes, "hello "));
    
    // Run some code that uses those variables
    StatementContainer code = new StatementContainer(
        new TokenContainer(),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"world\"", Symbol.String)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable, 
                new TokenContainer(Token.ParameterToken.fromContents(".b", Symbol.DotVariable)))
            )
        );
    SimpleInterpreter interpreter = new SimpleInterpreter(code);
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    interpreter.runFrameForTesting(ctx, scope);

    Assert.assertEquals("hello world", fun.captured.val);
  }
  
  @Test
  public void testCreateNewVariables() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)
            )
        );
    {
      SimpleInterpreter interpreter = new SimpleInterpreter(code);
      MachineContext ctx = new MachineContext();
      ctx.coreTypes = coreTypes;
      ctx.getGlobalScope().setParent(new TestScopeWithTypes(coreTypes));
      interpreter.runFrameForTesting(ctx, scope);
    }
    
    Assert.assertEquals(coreTypes.getNullType(), scope.lookup("a").type);
    
    code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("1", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("2", Symbol.Number)
            )
        );
    {
      SimpleInterpreter interpreter = new SimpleInterpreter(code);
      MachineContext ctx = new MachineContext();
      ctx.coreTypes = coreTypes;
      interpreter.runFrameForTesting(ctx, scope);
    }
    
    Assert.assertEquals(3.0, scope.lookup("a").getNumberValue(), 0.0);
  }
  
  @Test
  public void testIf() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    scope.addVariable("a", coreTypes.getNullType(), Value.createNumberValue(coreTypes, 5));
    scope.addVariable("b", coreTypes.getNullType(), Value.createNumberValue(coreTypes, 0));
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("<", Symbol.Lt),
                    new Token.SimpleToken("2", Symbol.Number)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("32", Symbol.Number)
                        )
                    )),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("1", Symbol.Number)
            )
        );
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(5.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(1.0, scope.lookup("b").getNumberValue(), 0.0);
    
    scope.assignTo("a", Value.createNumberValue(coreTypes, 0));
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(32.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(2.0, scope.lookup("b").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());
  }
  
  @Test
  public void testIfElse() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    scope.addVariable("b", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("<", Symbol.Lt),
                    new Token.SimpleToken("2", Symbol.Number)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("8", Symbol.Number)
                        )
                    )),
            new Token.OneExpressionOneBlockToken("elseif", Symbol.COMPOUND_ELSEIF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("<", Symbol.Lt),
                    new Token.SimpleToken("5", Symbol.Number)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("16", Symbol.Number)
                        )
                    )),
            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("32", Symbol.Number)
                        )
                    )),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("1", Symbol.Number)
            )
        );
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(8.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(1.0, scope.lookup("b").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());
    
    scope.assignTo("a", Value.createNumberValue(coreTypes, 2));
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(16.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(2.0, scope.lookup("b").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());

    scope.assignTo("a", Value.createNumberValue(coreTypes, 6));
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(32.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(3.0, scope.lookup("b").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());
  }

  @Test
  public void testWhile() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    scope.addVariable("b", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 1));
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("while", Symbol.COMPOUND_WHILE, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("<", Symbol.Lt),
                    new Token.SimpleToken("8", Symbol.Number)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                        new Token.SimpleToken("*", Symbol.Multiply),
                        new Token.SimpleToken("2", Symbol.Number)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("1", Symbol.Number)
                        )
                    )),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken("-", Symbol.Minus),
            new Token.SimpleToken("1", Symbol.Number)
            )
        );
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(8.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(255.0, scope.lookup("b").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());
  }

  @Test
  public void testIfScope() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      scope.addVariable("b", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                    new Token.SimpleToken("<", Symbol.Lt),
                    new Token.SimpleToken("2", Symbol.Number)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("var", Symbol.Var),
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                        new Token.SimpleToken(":", Symbol.Colon),
                        Token.ParameterToken.fromContents("@number", Symbol.AtType)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("8", Symbol.Number)
                        ),
                    new TokenContainer(
                        new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                            new TokenContainer(
                                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                                new Token.SimpleToken(">", Symbol.Gt),
                                new Token.SimpleToken("6", Symbol.Number)
                                ), 
                            new StatementContainer(
                                new TokenContainer(
                                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
                                    new Token.SimpleToken(":=", Symbol.Assignment),
                                    new Token.SimpleToken("3", Symbol.Number)
                                    )
                                )))
                    )),
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("<", Symbol.Lt),
                        new Token.SimpleToken("5", Symbol.Number)
                        ), 
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                            new Token.SimpleToken(":=", Symbol.Assignment),
                            new Token.SimpleToken("36", Symbol.Number)
                            )
                        ))

            )
        );
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(36.0, vars.globalScope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(3.0, vars.globalScope.lookup("b").getNumberValue(), 0.0);
  }

  @Test
  public void testCompoundComment() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    scope.addVariable("b", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("32", Symbol.Number)
            ),
        new TokenContainer(
            new Token.OneBlockToken("/*", Symbol.COMPOUND_COMMENT, 
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("16", Symbol.Number)
                        )
                    ))
            ));
    new SimpleInterpreter(code).runFrameForTesting(ctx, scope);
    Assert.assertEquals(32.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(scope, ctx.currentScope());
  }

  
  @Test
  public void testBooleanVariable() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 3));
      scope.addVariable("b", coreTypes.getBooleanType(), Value.createBooleanValue(coreTypes, false));
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken("=", Symbol.Eq),
            new Token.SimpleToken("3", Symbol.Number),
            new Token.SimpleToken("and", Symbol.And),
            new Token.SimpleToken("true", Symbol.TrueLiteral)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertTrue(vars.globalScope.lookup("b").getBooleanValue());
  }
  
  @Test
  public void testNoArgPrimitiveMethod() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, -3));
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".abs", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(vars.coreTypes.getNumberType(), vars.globalScope.lookup("a").type);
    Assert.assertEquals(3, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testVarDeclarationWithAssignment() throws ParseException, RunException
  {
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    MachineContext ctx = new MachineContext();
    ctx.coreTypes = coreTypes;
    ctx.getGlobalScope().setParent(new TestScopeWithTypes(coreTypes));
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("1", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("2", Symbol.Number)
            ));
    VariableScope localScope = new VariableScope();
    new SimpleInterpreter(code).runFrameForTesting(ctx, localScope);
    Assert.assertEquals(coreTypes.getNumberType(), localScope.lookup("a").type);
    Assert.assertEquals(3, localScope.lookup("a").getNumberValue(), 0);
    try {
      ctx.getGlobalScope().lookup("a");
      fail();
    } 
    catch (RunException e)
    {
      // Variable shouldn't exist in global scope
    }
  }
  
  @Test
  public void testFunctionCallWithParametersNoReturn() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      // Create an adder function that assigns the result to a global variable
      // (that way we don't need to handle return values yet)
      try {
        ExecutableFunction adderFn = ExecutableFunction.forCode(
            CodeUnitLocation.forFunction(""), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".val1", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents(".val2", Symbol.DotVariable)
                        )
                    )
                ), 
            null, Optional.empty(), Arrays.asList("val1", "val2"));
        Value adderFnVal = Value.create(adderFn, Type.makeFunctionType(coreTypes.getNumberType(), coreTypes.getNumberType(), coreTypes.getNumberType()));
        scope.addVariable("add:to:", Type.makeFunctionType(coreTypes.getNumberType(), coreTypes.getNumberType(), coreTypes.getNumberType()), adderFnVal);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".add:to:", Symbol.DotVariable,
                new TokenContainer(new Token.SimpleToken("2", Symbol.Number)),
                new TokenContainer(new Token.SimpleToken("3", Symbol.Number)))
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(5, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testFunctionReturningSimpleValue() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      try {
        ExecutableFunction getFn = ExecutableFunction.forCode(
            CodeUnitLocation.forFunction(""), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.SimpleToken("32", Symbol.Number)
                        )
                    )
                ), 
            null, Optional.empty(), Arrays.asList());
        Value getFnVal = Value.create(getFn, Type.makeFunctionType(coreTypes.getNumberType()));
        scope.addVariable("getVal", Type.makeFunctionType(coreTypes.getNumberType()), getFnVal);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".getVal", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(32, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testFunctionReturningNothingForVoid() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      try {
        ExecutableFunction setAFn = ExecutableFunction.forCode(
            CodeUnitLocation.forFunction(""), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("25", Symbol.Number)
                        ),
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return)
                        )
                    )
                ), 
            null, Optional.empty(), Arrays.asList());
        Value setA = Value.create(setAFn, Type.makeFunctionType(coreTypes.getVoidType()));
        scope.addVariable("set a", Type.makeFunctionType(coreTypes.getVoidType()), setA);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".set a", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(25, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testBlockingPrimitive() throws ParseException, RunException
  {
    // Set-up some variables and functions
    class CapturePrimitive implements PrimitivePassthrough {
      Value captured;
      int checkedCount = 0;
      MachineContext machine;
      MachineContext.PrimitiveBlockingFunctionReturn blockWait;
      @Override
      public void call(PrimitiveBlockingFunctionReturn blockWait,
          MachineContext machine) throws RunException
      {
        this.machine = machine;
        captured = machine.currentScope().lookup("val");
        this.blockWait = blockWait;
        blockWait.checkDone = () -> { checkedCount++; };
      }
      void unblock()
      {
        blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), 32));
      }
    }
    CapturePrimitive fun = new CapturePrimitive();
    
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      
      try {
        ExecutableFunction aFn = ExecutableFunction.forCode(
            CodeUnitLocation.forFunction("a:"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough)
                        )
                    )
                ), 
            null, Optional.empty(), Arrays.asList("val"));
        Value aFnVal = Value.create(aFn, Type.makeFunctionType(coreTypes.getNumberType(), coreTypes.getStringType()));
        scope.addVariable("a:", aFnVal.type, aFnVal);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
      coreTypes.addPrimitive(CodeUnitLocation.forFunction("a:"), fun); ;
      scope.addVariable("b", coreTypes.getStringType(), Value.createStringValue(coreTypes, "hello "));
      scope.addVariable("c", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 2));
    });
    
    // Run some code that uses those variables
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable, 
                new TokenContainer(Token.ParameterToken.fromContents(".b", Symbol.DotVariable)))
            )
        );
    SimpleInterpreter terp = new SimpleInterpreter(code); 
    // Machine should block
    terp.runNoReturn(vars);
    Assert.assertEquals("hello ", fun.captured.val);
    Assert.assertEquals(2.0, vars.globalScope.lookup("c").getNumberValue(), 0.0);
    Assert.assertEquals(1, fun.checkedCount);
    // Machine will still be blocked if we run again
    terp.continueRun();
    Assert.assertEquals(2.0, vars.globalScope.lookup("c").getNumberValue(), 0.0);
    Assert.assertEquals(2, fun.checkedCount);
    // Unblock things and let the machine run to completion
    fun.unblock();
    terp.continueRun();
    Assert.assertEquals(3, fun.checkedCount);
    Assert.assertEquals(32.0, vars.globalScope.lookup("c").getNumberValue(), 0.0);
  }

  @Test
  public void testBasicMethodCall() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      try {
        ExecutableFunction getFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("number", "testGetVal"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.SimpleToken("32", Symbol.Number)
                        )
                    )
                ), 
            coreTypes.getNumberType(), Optional.empty(), Arrays.asList());
        coreTypes.getNumberType().addMethod("testGetVal", getFn, coreTypes.getNumberType());
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("2", Symbol.Number),
            Token.ParameterToken.fromContents(".testGetVal", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(32, vars.globalScope.lookup("a").getNumberValue(), 0);
  }
  
  @Test
  public void testMethodCallWithThis() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      try {
        ExecutableFunction getFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("number", "testGetVal"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("this", Symbol.This),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("3", Symbol.Number)
                        )
                    )
                ), 
            coreTypes.getNumberType(), Optional.empty(), Arrays.asList());
        coreTypes.getNumberType().addMethod("testGetVal", getFn, coreTypes.getNumberType());
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("2", Symbol.Number),
            Token.ParameterToken.fromContents(".testGetVal", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(5, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testMethodCallSupertypeAccessSubtypeVariables() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getStringType(), coreTypes.getNullValue());
      scope.addVariable("b", coreTypes.getStringType(), coreTypes.getNullValue());
      
      Type childType = new Type("child", coreTypes.getObjectType());
//      childType.addMemberVariable("childVar", coreTypes.getStringType());
      Type subChildType = new Type("subchild", childType);
      subChildType.addMemberVariable("a", coreTypes.getStringType());
//      subChildType.addMemberVariable("subchildVar", coreTypes.getStringType());

      scope.addVariable("obj", subChildType, coreTypes.getNullValue());

      try {
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("child", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    )
                )
            ),
            childType, Optional.empty(), Arrays.asList());
        childType.addStaticMethod("new", createFn, coreTypes.getVoidType());
        scope.addType(childType);

        ExecutableFunction modAFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("child", "modify a"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"5\"", Symbol.String)
                    )
                )
            ),
            childType, Optional.empty(), Arrays.asList());
        childType.addMethod("modify a", modAFn, coreTypes.getVoidType());

        ExecutableFunction subCreateFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("subchild", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"3\"", Symbol.String)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addStaticMethod("new", subCreateFn, coreTypes.getVoidType());
        scope.addType(subChildType);

        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("subchild", "get"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addMethod("get", valFn, coreTypes.getStringType());
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".obj", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@subchild", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ),
        new TokenContainer(
            // Overwrites a, but should overwrite the global variable a
            // not the member variable
            Token.ParameterToken.fromContents(".obj", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".modify a", Symbol.DotVariable)
            ),
        new TokenContainer(
            // Returns the value of the a member variable defined in the
            // subchild
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".obj", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
            )
        );
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals("5", vars.globalScope.lookup("a").getStringValue());
    Assert.assertEquals("3", vars.globalScope.lookup("b").getStringValue());
  }

  @Test
  public void testStaticMethodCall() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      
      Type testType = new Type("test", coreTypes.getObjectType());
      try {
        ExecutableFunction getFn = ExecutableFunction.forCode(
            CodeUnitLocation.forStaticMethod("test", "calcVal"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("2", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents("@number", Symbol.AtType),
                        Token.ParameterToken.fromContents(".parse US number:", Symbol.DotVariable,
                            new TokenContainer(
                                new Token.SimpleToken("\"3\"", Symbol.String)
                            )
                        )
                    )
                )
            ),
            testType, Optional.empty(), Arrays.asList());
        testType.addStaticMethod("calcVal", getFn, coreTypes.getVoidType());
        scope.addType(testType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@test", Symbol.AtType),
            Token.ParameterToken.fromContents(".calcVal", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(5, vars.globalScope.lookup("a").getNumberValue(), 0);
  }
  
  @Test
  public void testConstructorCallCreateObject() throws RunException, ParseException
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
            Token.ParameterToken.fromContents("@object", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(vars.coreTypes.getObjectType(), vars.globalScope.lookup("a").type);
  }

  @Test
  public void testConstructObjectWithMemberVars() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type testType = new Type("test", coreTypes.getObjectType());
      testType.addMemberVariable("testMember", coreTypes.getObjectType());
      try {
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("test", "create"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".testMember", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("2", Symbol.Number)
                    )
                )
            ),
            testType, Optional.empty(), Arrays.asList());
        testType.addStaticMethod("create", createFn, coreTypes.getVoidType());
        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("test", "get"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".testMember", Symbol.DotVariable)
                    )
                )
            ),
            testType, Optional.empty(), Arrays.asList());
        testType.addMethod("get", valFn, coreTypes.getNumberType());
        scope.addType(testType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@test", Symbol.AtType),
            Token.ParameterToken.fromContents(".create", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
            )
        );
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(2, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testConstructorChaining() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getStringType(), coreTypes.getNullValue());
      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type childType = new Type("child", coreTypes.getObjectType());
      childType.addMemberVariable("childVar", coreTypes.getStringType());
      Type subChildType = new Type("subchild", childType);
      subChildType.addMemberVariable("childVar", coreTypes.getStringType());
      subChildType.addMemberVariable("subchildVar", coreTypes.getStringType());
      try {
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("child", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".childVar", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"2\"", Symbol.String)
                    )
                )
            ),
            childType, Optional.empty(), Arrays.asList());
        childType.addStaticMethod("new", createFn, coreTypes.getVoidType());
        scope.addType(childType);

        ExecutableFunction subCreateFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("subchild", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".subchildVar", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"3\"", Symbol.String)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addStaticMethod("new", subCreateFn, coreTypes.getVoidType());
        scope.addType(subChildType);

        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("test", "get"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".childVar", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents(".subchildVar", Symbol.DotVariable)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addMethod("get", valFn, coreTypes.getNumberType());
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@subchild", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
            )
        );
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals("23", vars.globalScope.lookup("a").getStringValue());
  }

  @Test
  public void testConstructorNotChainedError() throws ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      
      Type testType = new Type("test", coreTypes.getObjectType());
      testType.addMemberVariable("testMember", coreTypes.getObjectType());
      try {
        StatementContainer code = new StatementContainer(
        );
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("test", "create"), 
            ParseToAst.parseStatementContainer(code),
            testType, Optional.of(code), Arrays.asList());
        testType.addStaticMethod("create", createFn, coreTypes.getVoidType());
        scope.addType(testType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("@test", Symbol.AtType),
            Token.ParameterToken.fromContents(".create", Symbol.DotVariable)
            )
        );
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("this is not initialized yet. You may be trying to access this before calling the supertype constructor", e.getMessage());
      Assert.assertEquals("test", e.getErrorLocation().getClassName());
      Assert.assertTrue(e.getErrorLocation().isStatic());
      Assert.assertEquals("create", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(0), e.getErrorLocation().getPosition());
      return;
    }
    Assert.fail("Expecting an error");
    
  }
  
  @Test
  public void testConstructorErrorAccessMemberVarsBeforeConstructorChaining() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type testType = new Type("test", coreTypes.getObjectType());
      testType.addMemberVariable("testMember", coreTypes.getObjectType());
      try {
        StatementContainer code = new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".testMember", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                new Token.SimpleToken("2", Symbol.Number)
                ),
            new TokenContainer(
                new Token.SimpleToken("super", Symbol.Super),
                Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                )
        );
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("test", "create"), 
            ParseToAst.parseStatementContainer(code),
            testType, Optional.of(code), Arrays.asList());
        testType.addStaticMethod("create", createFn, coreTypes.getVoidType());
//        ExecutableFunction valFn = ExecutableFunction.forCode(
//            CodeUnitLocation.forMethod("test", "get"), 
//            ParseToAst.parseStatementContainer(
//                new StatementContainer(
//                    new TokenContainer(
//                        new Token.SimpleToken("return", Symbol.Return),
//                        Token.ParameterToken.fromContents(".testMember", Symbol.DotVariable)
//                    )
//                )
//            ),
//            testType, Optional.empty(), Arrays.asList());
//        testType.addMethod("get", valFn, coreTypes.getNumberType());
        scope.addType(testType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@test", Symbol.AtType),
            Token.ParameterToken.fromContents(".create", Symbol.DotVariable)
            )
//        new TokenContainer(
//            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//            new Token.SimpleToken(":=", Symbol.Assignment),
//            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//            Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
//            )
        );
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("this value not initialized. Supertype constructor possibly not called before accessing member variables.", e.getMessage());
      Assert.assertEquals("test", e.getErrorLocation().getClassName());
      Assert.assertEquals("create", e.getErrorLocation().getFunctionMethodName());
      Assert.assertTrue(e.getErrorLocation().isStatic());
      Assert.assertEquals(CodePosition.fromOffsets(0, 0), e.getErrorLocation().getPosition());
      return;
    }
    Assert.fail("Expecting an error");
  }
  
  @Test
  public void testConstructorErrorAccessThisBeforeConstructorChaining() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type testType = new Type("test", coreTypes.getObjectType());
      testType.addMemberVariable("testMember", coreTypes.getObjectType());
      try {
        StatementContainer code = new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("this", Symbol.This),
                Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
                ),
            new TokenContainer(
                new Token.SimpleToken("super", Symbol.Super),
                Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                )
        );
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("test", "create"), 
            ParseToAst.parseStatementContainer(code),
            testType, Optional.of(code), Arrays.asList());
        testType.addStaticMethod("create", createFn, coreTypes.getVoidType());
        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("test", "get"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.SimpleToken("5", Symbol.Number)
                    )
                )
            ),
            testType, Optional.empty(), Arrays.asList());
        testType.addMethod("get", valFn, coreTypes.getNumberType());
        scope.addType(testType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@test", Symbol.AtType),
            Token.ParameterToken.fromContents(".create", Symbol.DotVariable)
            )
        );
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("this is not initialized yet. You may be trying to access this before calling the supertype constructor", e.getMessage());
      Assert.assertEquals("test", e.getErrorLocation().getClassName());
      Assert.assertTrue(e.getErrorLocation().isStatic());
      Assert.assertEquals("create", e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(0, 0), e.getErrorLocation().getPosition());
      return;
    }
    Assert.fail("Expecting an error");
  }

  @Test
  public void testConstructorChainingErrorWhenSupertypeAccessSubtypeVariables() throws RunException, ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getStringType(), coreTypes.getNullValue());
      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type childType = new Type("child", coreTypes.getObjectType());
      childType.addMemberVariable("childVar", coreTypes.getStringType());
      Type subChildType = new Type("subchild", childType);
      subChildType.addMemberVariable("childVar", coreTypes.getStringType());
      subChildType.addMemberVariable("subchildVar", coreTypes.getStringType());
      try {
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("child", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".childVar", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"2\"", Symbol.String)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".subchildVar", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"badvalue\"", Symbol.String)
                    )

                )
            ),
            childType, Optional.empty(), Arrays.asList());
        childType.addStaticMethod("new", createFn, coreTypes.getVoidType());
        scope.addType(childType);

        ExecutableFunction subCreateFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("subchild", "new"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                    ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".subchildVar", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("\"3\"", Symbol.String)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addStaticMethod("new", subCreateFn, coreTypes.getVoidType());
        scope.addType(subChildType);

        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("test", "get"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".childVar", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents(".subchildVar", Symbol.DotVariable)
                    )
                )
            ),
            subChildType, Optional.empty(), Arrays.asList());
        subChildType.addMethod("get", valFn, coreTypes.getNumberType());
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents("@subchild", Symbol.AtType),
            Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".get", Symbol.DotVariable)
            )
        );
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("Cannot find value .subchildVar", e.getMessage());
      return;
    }
    Assert.assertEquals("23", vars.globalScope.lookup("a").getStringValue());
    Assert.fail("Should fail before here");
  }

  @Test
  public void testForLoop() throws RunException, ParseException
  {
    // Create a fake iterator and use it in a loop
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 0));
//      scope.addVariable("b", coreTypes.getObjectType(), coreTypes.getNullValue());
      
      Type iteratorType = new Type("number iterator", coreTypes.getObjectType());
      iteratorType.addMemberVariable("current", coreTypes.getNumberType());
      try {
        ExecutableFunction createFn = ExecutableFunction.forCode(
            CodeUnitLocation.forConstructorMethod("number iterator", "start:"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("super", Symbol.Super),
                        Token.ParameterToken.fromContents(".new", Symbol.DotVariable)
                        ),
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".current", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".start", Symbol.DotVariable)
                    )
                )
            ),
            iteratorType, Optional.empty(), Arrays.asList("start"));
        iteratorType.addStaticMethod("start:", createFn, coreTypes.getVoidType(), coreTypes.getNumberType());
        ExecutableFunction atEndFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("number iterator", ".at end"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".current", Symbol.DotVariable),
                        new Token.SimpleToken(">=", Symbol.Ge),
                        new Token.SimpleToken("4", Symbol.Number)
                    )
                )
            ),
            iteratorType, Optional.empty(), Arrays.asList());
        iteratorType.addMethod("at end", atEndFn, coreTypes.getBooleanType());
        ExecutableFunction valFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("number iterator", "value"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".current", Symbol.DotVariable)
                    )
                )
            ),
            iteratorType, Optional.empty(), Arrays.asList());
        iteratorType.addMethod("value", valFn, coreTypes.getNumberType());
        ExecutableFunction nextFn = ExecutableFunction.forCode(
            CodeUnitLocation.forMethod("number iterator", "next"), 
            ParseToAst.parseStatementContainer(
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".current", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".current", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("1", Symbol.Number)
                    ),
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.SimpleToken("this", Symbol.This)
                    )
                )
            ),
            iteratorType, Optional.empty(), Arrays.asList());
        iteratorType.addMethod("next", nextFn, iteratorType);
        scope.addType(iteratorType);
      } catch (ParseException e) { throw new IllegalArgumentException(e); }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("for", Symbol.COMPOUND_FOR,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
//                    new Token.SimpleToken(":", Symbol.Colon),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType),
                    new Token.SimpleToken("in", Symbol.In),
                    Token.ParameterToken.fromContents("@number iterator", Symbol.AtType),
                    Token.ParameterToken.fromContents(".start:", Symbol.DotVariable, 
                        new TokenContainer(
                            new Token.SimpleToken("2", Symbol.Number)
                            ))
                    ),
                new StatementContainer(
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents(".b", Symbol.DotVariable)
                        )))
            )
        );
    SimpleInterpreter interpreter = new SimpleInterpreter(code);
    interpreter.runNoReturn(vars);
    Assert.assertEquals(0, interpreter.ctx.valueStackSize());
    Assert.assertEquals(5.0, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testFunctionLiteralNoArgsNoClosures() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 3));
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call", Symbol.FunctionTypeName),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents(".returned value", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@call", Symbol.FunctionTypeName),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents(".returned value", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.SimpleToken("2", Symbol.Number)
                        )))
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call", Symbol.DotVariable)
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(2.0, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testFunctionLiteralOneArg() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 3));
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents(".returned value", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@number", Symbol.AtType))),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents(".returned value", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("2", Symbol.Number)
                        )))
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("5", Symbol.Number)))
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(7.0, vars.globalScope.lookup("a").getNumberValue(), 0);
  }
  
  @Test
  public void testFunctionLiteralWithClosures() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      scope.addVariable("a", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 3));
    });

    StatementContainer code = new StatementContainer(
        // Create a lambda containing a local variable. The lambda then returns another lambda which accesses that local variable
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".makeLambda", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@make", Symbol.FunctionTypeName),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@make", Symbol.FunctionTypeName),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@number", Symbol.AtType))),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("var", Symbol.Var),
                        Token.ParameterToken.fromContents(".val", Symbol.DotVariable),
                        Token.ParameterToken.fromContents("@number", Symbol.AtType),
                        new Token.SimpleToken(":=", Symbol.Assignment),
                        new Token.SimpleToken("20", Symbol.Number)
                        ),
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                            new TokenContainer(
                                Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                                    new TokenContainer(
                                        Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                                        Token.ParameterToken.fromContents("@number", Symbol.AtType))),
                                new Token.SimpleToken("returns", Symbol.Returns),
                                Token.ParameterToken.fromContents("@number", Symbol.AtType)),
                            new StatementContainer(
                                new TokenContainer(
                                    Token.ParameterToken.fromContents(".val", Symbol.DotVariable),
                                    new Token.SimpleToken(":=", Symbol.Assignment),
                                    Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                                    new Token.SimpleToken("+", Symbol.Plus),
                                    Token.ParameterToken.fromContents(".val", Symbol.DotVariable)
                                    ),
                                new TokenContainer(
                                    new Token.SimpleToken("return", Symbol.Return),
                                    Token.ParameterToken.fromContents(".val", Symbol.DotVariable)
                                    ))
                        )
                        )))
            ),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType))),
            new Token.SimpleToken("returns", Symbol.Returns),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".makeLambda", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".make", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("1", Symbol.Number)))
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".call:", Symbol.DotVariable,
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number)))
            ));
    new SimpleInterpreter(code).runNoReturn(vars);
    Assert.assertEquals(23.0, vars.globalScope.lookup("a").getNumberValue(), 0);
  }

  @Test
  public void testFunctionLiteralWithThis() throws ParseException, RunException
  {
    // TODO: test lambdas that access "this"
  }
  
  @Test
  public void testCallPlomLambdaFromJs() throws ParseException, RunException
  {
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
      try {
        // function type of the lambda
        UnboundType funType = UnboundType.forSimpleFunctionType("number", "call:", "number"); 
        scope.addVariable("fun", 
            scope.typeFromUnboundTypeFromScope(funType), coreTypes.getNullValue());
      } catch (RunException e) {
        throw new IllegalArgumentException(e);
      }
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("6", Symbol.Number)),
        new TokenContainer(
            Token.ParameterToken.fromContents(".fun", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral,
                new TokenContainer(
                    Token.ParameterToken.fromContents("f@call:", Symbol.FunctionTypeName,
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".arg", Symbol.DotVariable),
                            Token.ParameterToken.fromContents("@number", Symbol.AtType))),
                    new Token.SimpleToken("returns", Symbol.Returns),
                    Token.ParameterToken.fromContents(".returned value", Symbol.DotVariable),
                    Token.ParameterToken.fromContents("@number", Symbol.AtType)),
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("return", Symbol.Return),
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("+", Symbol.Plus),
                        Token.ParameterToken.fromContents(".arg", Symbol.DotVariable)
                        )))
            ));
    // Run to create a lambda
    SimpleInterpreter oldTerp = new SimpleInterpreter(code); 
    oldTerp.runNoReturn(vars);
    
    // Now try calling the lambda from outside
    Value lambda = vars.globalScope.lookup("fun");
    ArrayOf<Value> arguments = new JreArrayOf<>();
    arguments.push(Value.createNumberValue(oldTerp.ctx.coreTypes(), 10));
    Value returned = SimpleInterpreter.callPlomLambdaFromJs(oldTerp.ctx, (LambdaFunction)lambda.val, arguments);
    Assert.assertEquals(16, returned.getNumberValue(), 0.01);
  }

  @Test
  public void testMissingFunctionInsideUnnamed() throws ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(),
        new TokenContainer(
            Token.ParameterToken.fromContents(".missing function", Symbol.DotVariable)
            ));
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("Cannot find value .missing function", e.getMessage());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertNull(e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(1, 0), e.getErrorLocation().getPosition());
      return;
    }
    Assert.fail("Expecting an exception");
  }
  
  @Test
  public void testMethodOnNull() throws ParseException
  {
    // Calls a method added to a primitive type that doesn't access
    // any data
    GlobalsSaver vars = new GlobalsSaver((scope, coreTypes) -> {
      StandardLibrary.createCoreTypes(coreTypes);
    });
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("null", Symbol.NullLiteral)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            Token.ParameterToken.fromContents(".test", Symbol.DotVariable)
            ));
    try {
      new SimpleInterpreter(code).runNoReturn(vars);
    }
    catch (RunException e)
    {
      Assert.assertEquals("Cannot find method @null .test", e.getMessage());
      Assert.assertNull(e.getErrorLocation().getClassName());
      Assert.assertNull(e.getErrorLocation().getFunctionMethodName());
      Assert.assertEquals(CodePosition.fromOffsets(1, 1), e.getErrorLocation().getPosition());
      return;
    }
    Assert.fail("Expecting an exception");
  }

}
