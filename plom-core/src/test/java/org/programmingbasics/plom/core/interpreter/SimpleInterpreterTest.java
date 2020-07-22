package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;

import junit.framework.TestCase;

public class SimpleInterpreterTest extends TestCase
{
  static class GlobalsSaver implements ConfigureGlobalScope
  {
    GlobalsSaver(ConfigureGlobalScope passthrough)
    {
      this.passthrough = passthrough;
    }
    ConfigureGlobalScope passthrough;
    VariableScope globalScope;
    CoreTypeLibrary coreTypes;
    
    @Override public void configure(VariableScope scope, CoreTypeLibrary coreTypes)
    {
      this.globalScope = scope;
      this.coreTypes = coreTypes;
      if (passthrough != null)
        passthrough.configure(scope, coreTypes);
    }
  }
  
  @Test
  public void testStatements() throws ParseException, RunException
  {
    // Set-up some variables and functions
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    Value aVal = new Value();
    aVal.type = Type.makePrimitiveFunctionType(coreTypes.getNumberType(), coreTypes.getStringType());
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
    aVal.val = fun;
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
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
            )
        );
    {
      SimpleInterpreter interpreter = new SimpleInterpreter(code);
      MachineContext ctx = new MachineContext();
      ctx.coreTypes = coreTypes;
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
                        new Token.SimpleToken(":", Symbol.Colon),
                        Token.ParameterToken.fromContents(".number", Symbol.DotVariable)
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
    
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":", Symbol.Colon),
            Token.ParameterToken.fromContents(".number", Symbol.DotVariable),
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

}
