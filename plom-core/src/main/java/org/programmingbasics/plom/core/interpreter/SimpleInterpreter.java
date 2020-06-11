package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.client.Browser;

/**
 * In order to work out how the language will work, I need a simple 
 * code interpreter to make things more concrete and to experiment
 * with different possibilities.
 * 
 * This just runs a simple script or code sequence.
 */
public class SimpleInterpreter
{
  public SimpleInterpreter(StatementContainer code)
  {
    this.code = code;
  }
  
  StatementContainer code;
  
  static class Context
  {
    VariableScope scope;
  }

  static AstNode.VisitorTriggers<Void, Context, RunException> triggers = new AstNode.VisitorTriggers<Void, Context, RunException>()
      .add(Rule.Statement_AssignmentExpression, (triggers, node, returned, context) -> {
        ExpressionEvaluator.eval(node, context.scope);
        return true;
      });

  
  void createGlobals(VariableScope scope)
  {
    Value printFun = new Value();
    printFun.type = Type.makeFunctionType(Type.NUMBER, Type.STRING);
    printFun.val = new PrimitiveFunction() {
      @Override public Value call(List<Value> args)
      {
        Browser.getWindow().alert(args.get(0).getStringValue());
        return Value.createNumberValue(0);
      }
    };
    scope.addVariable("print:", printFun);
  }
  
  public void runCode(Context ctx) throws ParseException, RunException
  {
    for (TokenContainer line: code.statements)
    {
      ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
      AstNode parsed = parser.parseToEnd(Symbol.StatementOrEmpty);
      parsed.recursiveVisit(triggers, null, ctx);
    }
  }
  
  public void run() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    createGlobals(scope);
    Context ctx = new Context();
    ctx.scope = scope;
    runCode(ctx);
  }
}
