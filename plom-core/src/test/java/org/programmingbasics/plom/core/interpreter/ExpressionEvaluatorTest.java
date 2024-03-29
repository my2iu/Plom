package org.programmingbasics.plom.core.interpreter;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class ExpressionEvaluatorTest extends TestCase
{
  static CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
  
  Value evalTest(TokenContainer line, VariableScope scope) throws ParseException, RunException
  {
    // Parse the code
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    AstNode parsed = parser.parseToEnd(Symbol.Expression);
    // Set up the machine to hold the execution state
    MachineContext machine = new MachineContext();
    machine.coreTypes = coreTypes;
    // Start running the code
    machine.pushStackFrame(parsed, CodeUnitLocation.forUnknown(), Optional.empty(), null, ExpressionEvaluator.expressionHandlers);
    machine.pushScope(scope);
    machine.runToCompletion();
    // Result of the expression should be on the top of the stack
    Value val = machine.popValue();
    Assert.assertEquals(0, machine.valueStackSize());
    return val;
  }
  
  void evalAssignTest(TokenContainer line, VariableScope scope) throws ParseException, RunException
  {
    // Parse the code
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    AstNode parsed = parser.parseToEnd(Symbol.AssignmentExpression);
    // Set up the machine to hold the execution state
    MachineContext machine = new MachineContext();
    machine.coreTypes = coreTypes;
    // Start running the code
    machine.pushStackFrame(parsed, CodeUnitLocation.forUnknown(), Optional.empty(), null, ExpressionEvaluator.assignmentLValueHandlers);
    machine.pushScope(scope);
    machine.runToCompletion();
  }
  
  @Test
  public void testNumber() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(Double.valueOf(1), val.val);
  }
  
  @Test
  public void testNumberAdd() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("2.5", Symbol.Number));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(Double.valueOf(0.5), val.val);
  }
  
  @Test
  public void testMathExpressionOrderOfOperations() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("*", Symbol.Multiply),
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("/", Symbol.Divide),
        new Token.SimpleToken("(", Symbol.OpenParenthesis),
        new Token.SimpleToken("4", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("5", Symbol.Number),
        new Token.SimpleToken(")", Symbol.ClosedParenthesis));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(Double.valueOf(-5), val.val);
  }

  @Test
  public void testString() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("\"hello \"", Symbol.String),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("\"world\"", Symbol.String));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getStringType(), val.type);
    Assert.assertEquals("hello world", val.val);
  }

  @Test
  public void testVariable() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    Value aVal = Value.createNumberValue(coreTypes, 32);
    scope.addVariable("a", coreTypes.getNumberType(), aVal);
    
    // Read a variable
    {
      TokenContainer line = new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
      Value val = evalTest(line, scope);
      Assert.assertEquals(coreTypes.getNumberType(), val.type);
      Assert.assertEquals(32.0, val.val);
    }
    
    // Overwrite the variable
    {
      TokenContainer line = new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
          new Token.SimpleToken(":=", Symbol.Assignment),
          new Token.SimpleToken("5", Symbol.Number));
      evalAssignTest(line, scope);
      Assert.assertEquals(5.0, scope.lookup("a").val);
    }
    
    // Read back the variable to confirm the changes
    {
      TokenContainer line = new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
      Value val = evalTest(line, scope);
      Assert.assertEquals(coreTypes.getNumberType(), val.type);
      Assert.assertEquals(5.0, val.val);
    }
  }
  
  @Test
  public void testReadUnknownVariable() throws ParseException, RunException
  {
    try {
      TokenContainer line = new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
      evalAssignTest(line, new VariableScope());
      fail("Expecting a RunException");
    } 
    catch (RunException e)
    {
      
    }
  }
  
  @Test
  public void testBoolean() throws ParseException, RunException
  {
    {
      TokenContainer line = new TokenContainer(
          new Token.SimpleToken("true", Symbol.TrueLiteral));
      Value val = evalTest(line, new VariableScope());
      Assert.assertEquals(coreTypes.getBooleanType(), val.type);
      Assert.assertEquals(true, val.val);
    }
    
    {
      TokenContainer line = new TokenContainer(
          new Token.SimpleToken("false", Symbol.FalseLiteral));
      Value val = evalTest(line, new VariableScope());
      Assert.assertEquals(coreTypes.getBooleanType(), val.type);
      Assert.assertEquals(false, val.val);
    }
  }
  
  @Test
  public void testNoParamFunction() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    Value aVal = Value.create(
        new PrimitiveFunction() {
          @Override public Value call(List<Value> args)
          {
            return Value.createNumberValue(coreTypes, 32);
          }
        }, Type.makePrimitiveFunctionType(coreTypes.getNumberType()));
    scope.addVariable("a", aVal.type, aVal);
    
    // Call the function
    TokenContainer line = new TokenContainer(
        Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
    Value val = evalTest(line, scope);
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(32.0, val.val);
  }
  
  @Test
  public void testOneArgFunction() throws ParseException, RunException
  {
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
    
    // Call the function
    TokenContainer line = new TokenContainer(
        Token.ParameterToken.fromContents(".a:", Symbol.DotVariable, 
            new TokenContainer(new Token.SimpleToken("\"hello\"", Symbol.String))));
    Value val = evalTest(line, scope);
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(32.0, val.val);
    
    // Check if call args were passed in correctly
    Assert.assertEquals("hello", fun.captured.val);
  }
  
  @Test
  public void testBooleanOperators() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("<", Symbol.Lt),
        new Token.SimpleToken("5", Symbol.Number));
    Assert.assertEquals(coreTypes.getTrueValue(), evalTest(line, new VariableScope()));

    line = new TokenContainer(
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken(">", Symbol.Gt),
        new Token.SimpleToken("5", Symbol.Number));
    Assert.assertEquals(coreTypes.getFalseValue(), evalTest(line, new VariableScope()));

    line = new TokenContainer(
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("==", Symbol.Eq),
        new Token.SimpleToken("\"hi\"", Symbol.String));
    Assert.assertEquals(coreTypes.getFalseValue(), evalTest(line, new VariableScope()));

    line = new TokenContainer(
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("!=", Symbol.Ne),
        new Token.SimpleToken("\"hi\"", Symbol.String));
    Assert.assertEquals(coreTypes.getTrueValue(), evalTest(line, new VariableScope()));

    line = new TokenContainer(
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("<=", Symbol.Le),
        new Token.SimpleToken("3", Symbol.Number),
        new Token.SimpleToken("==", Symbol.Eq),
        new Token.SimpleToken("true", Symbol.TrueLiteral));
    Assert.assertEquals(coreTypes.getTrueValue(), evalTest(line, new VariableScope()));
  }
  
  @Test
  public void testPrimitiveMethodCallNoArgsChained() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("-5.5", Symbol.Number),
        Token.ParameterToken.fromContents(".floor", Symbol.DotVariable),
        Token.ParameterToken.fromContents(".abs", Symbol.DotVariable));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals(6, val.getNumberValue(), 0);
  }
  
  @Test
  public void testPrimitiveMethodCallWithArgs() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("\"abcdefg\"", Symbol.String),
        Token.ParameterToken.fromContents(".substring from:to:", Symbol.DotVariable,
            new TokenContainer(new Token.SimpleToken("2", Symbol.Number)),
            new TokenContainer(new Token.SimpleToken("5", Symbol.Number))));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(coreTypes.getStringType(), val.type);
    Assert.assertEquals("cde", val.getStringValue());
  }

  @Test
  public void testAs() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("5", Symbol.Number),
        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable),
        new Token.SimpleToken("as", Symbol.As),
        Token.ParameterToken.fromContents("@string", Symbol.AtType),
        Token.ParameterToken.fromContents(".+:", Symbol.DotVariable,
            new TokenContainer(new Token.SimpleToken("\"2\"", Symbol.String)))
        );
    Value val = evalTest(line, new SimpleInterpreterTest.TestScopeWithTypes(coreTypes));
    Assert.assertEquals(coreTypes.getStringType(), val.type);
    Assert.assertEquals("5.02", val.getStringValue());
  }

  @Test
  public void testIs() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("5", Symbol.Number),
        new Token.SimpleToken("is", Symbol.Is),
        Token.ParameterToken.fromContents("@string", Symbol.AtType)
        );
    Value val = evalTest(line, new SimpleInterpreterTest.TestScopeWithTypes(coreTypes));
    Assert.assertEquals(coreTypes.getBooleanType(), val.type);
    Assert.assertEquals(false, val.getBooleanValue());
    
    line = new TokenContainer(
        new Token.SimpleToken("5", Symbol.Number),
        new Token.SimpleToken("is", Symbol.Is),
        Token.ParameterToken.fromContents("@object", Symbol.AtType)
        );
    val = evalTest(line, new SimpleInterpreterTest.TestScopeWithTypes(coreTypes));
    Assert.assertEquals(coreTypes.getBooleanType(), val.type);
    Assert.assertEquals(true, val.getBooleanValue());
  }

  @Test
  public void testRetype() throws ParseException, RunException
  {
    // Retype is just a hack needed to support JS objects easily
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("\"abcdefg\"", Symbol.String),
        new Token.SimpleToken("retype", Symbol.Retype),
        Token.ParameterToken.fromContents("@number", Symbol.AtType));
    Value val = evalTest(line, new SimpleInterpreterTest.TestScopeWithTypes(coreTypes));
    Assert.assertEquals(coreTypes.getNumberType(), val.type);
    Assert.assertEquals("abcdefg", val.getStringValue());
  }
}
