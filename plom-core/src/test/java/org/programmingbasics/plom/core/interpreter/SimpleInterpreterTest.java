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
    scope.addVariable("b", Type.STRING, Value.createStringValue("hello "));
    
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
  
  @Test
  public void testCreateNewVariables() throws ParseException, RunException
  {
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
      ctx.scope = scope;
      interpreter.runCode(ctx);
    }
    
    Assert.assertEquals(Type.NULL, scope.lookup("a").type);
    
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
      ctx.scope = scope;
      interpreter.runCode(ctx);
    }
    
    Assert.assertEquals(3.0, scope.lookup("a").getNumberValue(), 0.0);
  }
  
  @Test
  public void testIf() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    scope.addVariable("a", Type.NUMBER, Value.createNumberValue(5));
    scope.addVariable("b", Type.NUMBER, Value.createNumberValue(0));
    MachineContext ctx = new MachineContext();
    ctx.scope = scope;
    
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
    new SimpleInterpreter(code).runCode(ctx);
    Assert.assertEquals(5.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(1.0, scope.lookup("b").getNumberValue(), 0.0);
    
    scope.assignTo("a", Value.createNumberValue(0));
    new SimpleInterpreter(code).runCode(ctx);
    Assert.assertEquals(32.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(2.0, scope.lookup("b").getNumberValue(), 0.0);
  }
  
  @Test
  public void testIfElse() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    scope.addVariable("a", Type.NUMBER, Value.createNumberValue(0));
    scope.addVariable("b", Type.NUMBER, Value.createNumberValue(0));
    MachineContext ctx = new MachineContext();
    ctx.scope = scope;
    
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
    new SimpleInterpreter(code).runCode(ctx);
    Assert.assertEquals(8.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(1.0, scope.lookup("b").getNumberValue(), 0.0);
    
    scope.assignTo("a", Value.createNumberValue(2));
    new SimpleInterpreter(code).runCode(ctx);
    Assert.assertEquals(16.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(2.0, scope.lookup("b").getNumberValue(), 0.0);

    scope.assignTo("a", Value.createNumberValue(6));
    new SimpleInterpreter(code).runCode(ctx);
    Assert.assertEquals(32.0, scope.lookup("a").getNumberValue(), 0.0);
    Assert.assertEquals(3.0, scope.lookup("b").getNumberValue(), 0.0);
}

}
