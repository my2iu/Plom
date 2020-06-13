package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;

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
  AstNode parsedCode;
  
  static MachineContext.NodeHandlers statementHandlers = new MachineContext.NodeHandlers();
  static {
    statementHandlers
      .add(Rule.ASSEMBLED_STATEMENTS_BLOCK, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx < node.internalChildren.size())
              machine.ip.pushAndAdvanceIdx(node.internalChildren.get(idx), statementHandlers);
            else
              machine.ip.pop();
      })
      .add(Rule.Statement_AssignmentExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
              machine.ip.pushAndAdvanceIdx(node.children.get(0), ExpressionEvaluator.assignmentLValueHandlers);
            else
              machine.ip.pop();
          });
  }
  
  void createGlobals(VariableScope scope)
  {
    Value printFun = new Value();
    printFun.type = Type.makePrimitiveFunctionType(Type.NUMBER, Type.STRING);
    printFun.val = new PrimitiveFunction() {
      @Override public Value call(List<Value> args)
      {
        Browser.getWindow().alert(args.get(0).getStringValue());
        return Value.createNumberValue(0);
      }
    };
    scope.addVariable("print:", printFun);
  }
  
  public void runCode(MachineContext ctx) throws ParseException, RunException
  {
    if (parsedCode == null)
    {
      parsedCode = ParseToAst.parseStatementContainer(code);
    }
    
    ctx.setStart(parsedCode, statementHandlers);
    ctx.runToCompletion();
  }
  
  public void run() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    createGlobals(scope);
    MachineContext ctx = new MachineContext();
    ctx.scope = scope;
    runCode(ctx);
  }
}
