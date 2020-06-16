package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.MachineContext.PrimitiveBlockingFunctionReturn;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.InputElement;

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

  // When parsing type information, we need a structure for stashing
  // that type info in order to return it
  static class GatheredTypeInfo
  {
    Type type;
  }
  static AstNode.VisitorTriggers<GatheredTypeInfo, MachineContext, RuntimeException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredTypeInfo, MachineContext, RuntimeException>()
      .add(Rule.DotType_DotVariable, (triggers, node, typesToReturn, machine) -> {
        // Just pass through
        node.recursiveVisitChildren(triggers, typesToReturn, machine);
        return true;
      })
      .add(Rule.DotVariable, (triggers, node, typesToReturn, machine) -> {
        Type t = new Type(((Token.ParameterToken)node.token).getLookupName());
        typesToReturn.type = t;
        return true;
      });

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
      .add(Rule.VarStatement_Var_DotDeclareIdentifier_VarType_VarAssignment, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (!node.children.get(1).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
              throw new RunException();
            String name = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
            GatheredTypeInfo typeInfo = new GatheredTypeInfo();
            node.children.get(2).recursiveVisit(typeParsingHandlers, typeInfo, machine);
            Type type = typeInfo.type;
            if (type == null) type = Type.VOID;
            Value val = Value.NULL;
            if (!node.children.get(3).matchesRule(Rule.VarAssignment))
              throw new RunException("Not implemented yet");
            machine.scope.addVariable(name, type, val);
            machine.ip.pop();
          })
      .add(Rule.Statement_AssignmentExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
              machine.ip.pushAndAdvanceIdx(node.children.get(0), ExpressionEvaluator.assignmentLValueHandlers);
            else
              machine.ip.pop();
          })
      .add(Rule.WideStatement_COMPOUND_IF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (val.type != Type.BOOLEAN)
                throw new RunException();
              if (val.getBooleanValue())
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              else
                machine.ip.setIdx(3);
              break;
            case 2: // if is taken
              machine.ip.pop();
              break;
            case 3: // if is not taken
              machine.ip.pushAndAdvanceIdx(node.children.get(1), statementHandlers);
              break;
            case 4: // return from if not taken
              machine.ip.pop();
              break;
            }
          })
      .add(Rule.AfterIf_COMPOUND_ELSEIF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (val.type != Type.BOOLEAN)
                throw new RunException();
              if (val.getBooleanValue())
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              else
                machine.ip.setIdx(3);
              break;
            case 2: // if is taken
              machine.ip.pop();
              break;
            case 3: // if is not taken
              machine.ip.pushAndAdvanceIdx(node.children.get(1), statementHandlers);
              break;
            case 4: // return from if not taken
              machine.ip.pop();
              break;
            }
          })
      .add(Rule.AfterIf_COMPOUND_ELSE,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
              // Evaluate block
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), statementHandlers);
            else
              machine.ip.pop();
          })
      .add(Rule.WideStatement_COMPOUND_WHILE, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (val.type != Type.BOOLEAN)
                throw new RunException();
              if (val.getBooleanValue())
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              else
                machine.ip.pop();
              break;
            case 2: // go back to reevaluate the expression
              machine.ip.setIdx(0);
              break;
            }
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
        container.querySelector("a").focus();
      }
    };
    scope.addVariable("print:", printFun.type, printFun);
    
    Value inputFun = new Value();
    inputFun.type = Type.makePrimitiveBlockingFunctionType(Type.STRING, Type.STRING);
    inputFun.val = new PrimitiveFunction.PrimitiveBlockingFunction() {
      @Override
      public void call(PrimitiveBlockingFunctionReturn blockWait, List<Value> args)
      {
        Document doc = Browser.getDocument();
        DivElement container = doc.createDivElement();
        container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
        container.setInnerHTML("<div></div><div><form><input type=\"text\"></form></div><div><a href=\"#\">Ok</a></div>");
        container.querySelectorAll("div").item(0).setTextContent(args.get(0).getStringValue());
        container.querySelector("form").setOnsubmit((e) -> {
          e.preventDefault();
          doc.getBody().removeChild(container);
          blockWait.unblockAndReturn(Value.createStringValue(((InputElement)container.querySelector("input")).getValue()));
          continueRun();
        });
        container.querySelector("a").setOnclick((e) -> {
          e.preventDefault();
          doc.getBody().removeChild(container);
          blockWait.unblockAndReturn(Value.createStringValue(((InputElement)container.querySelector("input")).getValue()));
          continueRun();
        });
        doc.getBody().appendChild(container);
        container.querySelector("input").focus();
      }
    };
    scope.addVariable("input:", inputFun.type, inputFun);
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
