package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.PrimitiveFunction.PrimitiveBlockingFunction;

import junit.framework.TestCase;

public class MachineContextTest extends TestCase
{
  static CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();

  @Test
  public void testRunInstructionsRecursively() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("2.5", Symbol.Number));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    AstNode parsed = parser.parseToEnd(Symbol.Expression);
    MachineContext machine = new MachineContext();
    machine.pushStackFrame(parsed, CodeUnitLocation.forUnknown(), new MachineContext.NodeHandlers());
    machine.runToCompletion();
  }
  
  @Test
  public void testBlockingFunction() throws ParseException, RunException
  {
    // Set-up some variables and functions
    CoreTypeLibrary coreTypes = CoreTypeLibrary.createTestLibrary();
    VariableScope scope = new VariableScope();
    class CaptureFunction implements PrimitiveBlockingFunction {
      Value captured;
      int checkedCount = 0;
      MachineContext.PrimitiveBlockingFunctionReturn blockWait;
      @Override public void call(MachineContext.PrimitiveBlockingFunctionReturn blockWait, List<Value> args)
      {
        Assert.assertEquals(1, args.size());
        captured = args.get(0);
        this.blockWait = blockWait;
        blockWait.checkDone = () -> { checkedCount++; };
      }
      void unblock()
      {
        blockWait.unblockAndReturn(Value.createNumberValue(coreTypes, 32));
      }
    }
    CaptureFunction fun = new CaptureFunction();
    Value aVal = Value.create(fun, Type.makePrimitiveBlockingFunctionType(coreTypes.getNumberType(), coreTypes.getStringType()));
    scope.addVariable("a:", aVal.type, aVal);
    scope.addVariable("b", coreTypes.getStringType(), Value.createStringValue(coreTypes, "hello "));
    scope.addVariable("c", coreTypes.getNumberType(), Value.createNumberValue(coreTypes, 2));
    
    // Run some code that uses those variables
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".c", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable, 
                new TokenContainer(Token.ParameterToken.fromContents(".b", Symbol.DotVariable)))
            )
        );
    AstNode parsed = ParseToAst.parseStatementContainer(code);
    MachineContext machine = new MachineContext();
    machine.coreTypes = coreTypes;
    machine.pushStackFrame(parsed, CodeUnitLocation.forUnknown(), SimpleInterpreter.statementHandlers);
    machine.pushScope(scope);
    // Machine should block
    Assert.assertFalse(machine.runToCompletion());
    Assert.assertEquals("hello ", fun.captured.val);
    Assert.assertEquals(2.0, scope.lookup("c").getNumberValue(), 0.0);
    Assert.assertEquals(1, fun.checkedCount);
    // Machine will still be blocked if we run again
    Assert.assertFalse(machine.runToCompletion());
    Assert.assertEquals(2.0, scope.lookup("c").getNumberValue(), 0.0);
    Assert.assertEquals(2, fun.checkedCount);
    // Unblock things and let the machine run to completion
    fun.unblock();
    Assert.assertTrue(machine.runToCompletion());
    Assert.assertEquals(3, fun.checkedCount);
    Assert.assertEquals(32.0, scope.lookup("c").getNumberValue(), 0.0);
  }

}
