package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.MachineContext.PrimitiveBlockingFunctionReturn;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.DivElement;

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
  MachineContext ctx;
  
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
    printFun.type = Type.makePrimitiveBlockingFunctionType(Type.NUMBER, Type.STRING);
    printFun.val = new PrimitiveFunction.PrimitiveBlockingFunction() {
      @Override
      public void call(PrimitiveBlockingFunctionReturn blockWait, List<Value> args)
      {
        Document doc = Browser.getDocument();
        DivElement container = doc.createDivElement();
        container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
        container.setInnerHTML("<div></div><div><a href=\"#\">Ok</a></div>");
        container.querySelectorAll("div").item(0).setTextContent(args.get(0).getStringValue());
        container.querySelector("a").setOnclick((e) -> {
          e.preventDefault();
          doc.getBody().removeChild(container);
          blockWait.unblockAndReturn(Value.createNumberValue(0));
          continueRun();
        });
        doc.getBody().appendChild(container);
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

  public void continueRun()
  {
    try {
      ctx.runToCompletion();
    } 
    catch (RunException e)
    {
      // Swallow errors for now
    }
  }
  
  public void run() throws ParseException, RunException
  {
    VariableScope scope = new VariableScope();
    createGlobals(scope);
    ctx = new MachineContext();
    ctx.scope = scope;
    runCode(ctx);
  }
}
