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
  @Test
  public void testStatements() throws ParseException, RunException
  {
    // Set-up some variables and functions
    VariableScope scope = new VariableScope();
    Value aVal = new Value();
    aVal.type = Type.makeFunctionType(Type.NUMBER, Type.STRING);
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
    scope.addVariable("a:", aVal);
    scope.addVariable("b", Value.createStringValue("hello "));
    
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
    ctx.scope = scope;
    interpreter.runCode(ctx);

    Assert.assertEquals("hello world", fun.captured.val);
  } 
}
