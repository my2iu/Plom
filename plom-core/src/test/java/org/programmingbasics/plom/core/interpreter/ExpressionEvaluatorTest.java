package org.programmingbasics.plom.core.interpreter;

import java.util.List;

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
  Value evalTest(TokenContainer line, VariableScope scope) throws ParseException, RunException
  {
    // Parse the code
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parseToEnd(Symbol.Expression);
    // Set up the machine to hold the execution state
    MachineContext machine = new MachineContext();
    machine.scope = scope;
    // Start running the code
    machine.setStart(parsed, ExpressionEvaluator.expressionHandlers);
    machine.runToCompletion();
    // Result of the expression should be on the top of the stack
    Value val = machine.popValue();
    Assert.assertEquals(0, machine.valueStackSize());
    return val;
  }
  
  void evalAssignTest(TokenContainer line, VariableScope scope) throws ParseException, RunException
  {
    // Parse the code
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parseToEnd(Symbol.AssignmentExpression);
    // Set up the machine to hold the execution state
    MachineContext machine = new MachineContext();
    machine.scope = scope;
    // Start running the code
    machine.setStart(parsed, ExpressionEvaluator.assignmentLValueHandlers);
    machine.runToCompletion();
  }
  
  @Test
  public void testNumber() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number));
    Value val = evalTest(line, new VariableScope());
    Assert.assertEquals(Type.NUMBER, val.type);
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
    Assert.assertEquals(Type.NUMBER, val.type);
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
    Assert.assertEquals(Type.NUMBER, val.type);
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
    Assert.assertEquals(Type.STRING, val.type);
    Assert.assertEquals("hello world", val.val);
  }

  @Test
  public void testVariable() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    Value aVal = new Value();
    aVal.type = Type.NUMBER;
    aVal.val = 32;
    scope.addVariable("a", Type.NUMBER, aVal);
    
    // Read a variable
    {
      TokenContainer line = new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
      Value val = evalTest(line, scope);
      Assert.assertEquals(Type.NUMBER, val.type);
      Assert.assertEquals(32, val.val);
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
      Assert.assertEquals(Type.NUMBER, val.type);
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
      Assert.assertEquals(Type.BOOLEAN, val.type);
      Assert.assertEquals(true, val.val);
    }
    
    {
      TokenContainer line = new TokenContainer(
          new Token.SimpleToken("false", Symbol.FalseLiteral));
      Value val = evalTest(line, new VariableScope());
      Assert.assertEquals(Type.BOOLEAN, val.type);
      Assert.assertEquals(false, val.val);
    }
  }
  
  @Test
  public void testNoParamFunction() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    Value aVal = new Value();
    aVal.type = Type.makePrimitiveFunctionType(Type.NUMBER);
    aVal.val = new PrimitiveFunction() {
      @Override public Value call(List<Value> args)
      {
        return Value.createNumberValue(32);
      }
    };
    scope.addVariable("a", aVal.type, aVal);
    
    // Call the function
    TokenContainer line = new TokenContainer(
        Token.ParameterToken.fromContents(".a", Symbol.DotVariable));
    Value val = evalTest(line, scope);
    Assert.assertEquals(Type.NUMBER, val.type);
    Assert.assertEquals(32.0, val.val);
  }
  
  @Test
  public void testOneArgFunction() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    Value aVal = new Value();
    aVal.type = Type.makePrimitiveFunctionType(Type.NUMBER, Type.STRING);
    class CaptureFunction implements PrimitiveFunction {
      Value captured;
      @Override public Value call(List<Value> args)
      {
        Assert.assertEquals(1, args.size());
        captured = args.get(0);
        return Value.createNumberValue(32);
      }
    }
    CaptureFunction fun = new CaptureFunction();
    aVal.val = fun;
    scope.addVariable("a:", aVal.type, aVal);
    
    // Call the function
    TokenContainer line = new TokenContainer(
        Token.ParameterToken.fromContents(".a:", Symbol.DotVariable, 
            new TokenContainer(new Token.SimpleToken("\"hello\"", Symbol.String))));
    Value val = evalTest(line, scope);
    Assert.assertEquals(Type.NUMBER, val.type);
    Assert.assertEquals(32.0, val.val);
    
    // Check if call args were passed in correctly
    Assert.assertEquals("hello", fun.captured.val);
  }
}
