package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

public class ExpressionEvaluator
{
  VariableScope scope;

  static interface BinaryOperatorHandler
  {
    public Value apply(Value left, Value right) throws RunException;
  }

  // The pattern of binary operators expressed in an LL1 grammar with a More rule is pretty common,
  // so we have a function for easily making handlers for that situation 
  static MachineContext.MachineNodeVisitor createBinaryOperatorHandlerMore(BinaryOperatorHandler doOp)
  {
    return (machine, node, idx) -> {
      switch(idx)
      {
      case 0:
        machine.ipPushAndAdvanceIdx(node.children.get(1), expressionHandlers);
        break;
      case 1:
        {
          Value right = machine.popValue();
          Value left = machine.popValue();
          Value toReturn = doOp.apply(left, right);
          machine.pushValue(toReturn);
          machine.ipPop();
          break;
        }
      }
    };
  }

  
  static MachineContext.NodeHandlers expressionHandlers = new MachineContext.NodeHandlers();
  static {
    expressionHandlers
      .add(Rule.AdditiveExpressionMore_Plus_AdditiveExpression,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() + right.getNumberValue());
            else if (left.type == Type.STRING && right.type == Type.STRING)
              return Value.createStringValue(left.getStringValue() + right.getStringValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.AdditiveExpressionMore_Minus_AdditiveExpression,
          createBinaryOperatorHandlerMore((left, right) -> {
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                return Value.createNumberValue(left.getNumberValue() - right.getNumberValue());
              else
                throw new RunException();
          })
      )
      .add(Rule.MultiplicativeExpressionMore_Multiply_MultiplicativeExpression,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() * right.getNumberValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.MultiplicativeExpressionMore_Divide_MultiplicativeExpression,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() / right.getNumberValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.String, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.STRING;
            String rawStr = ((Token.SimpleToken)node.token).contents;
            val.val = rawStr.substring(1, rawStr.length() - 1);
            machine.pushValue(val);
            machine.ipPop();
      })
      .add(Rule.Number, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.NUMBER;
            val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
            machine.pushValue(val);
            machine.ipPop();
      })
      .add(Rule.TrueLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.BOOLEAN;
            val.val = Boolean.TRUE;
            machine.pushValue(val);
            machine.ipPop();
      })
      .add(Rule.FalseLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.BOOLEAN;
            val.val = Boolean.FALSE;
            machine.pushValue(val);
            machine.ipPop();
      })
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx < node.internalChildren.size())
            {
              machine.ipPushAndAdvanceIdx(node.internalChildren.get(idx), expressionHandlers);
              return;
            }
            Value toReturn = machine.scope.lookup(((Token.ParameterToken)node.token).getLookupName());
            if (toReturn.type.isFunction())
            {
              List<Value> args = new ArrayList<>();
              for (int n = 0; n < node.internalChildren.size(); n++)
              {
                args.add(machine.readValue(node.internalChildren.size() - n - 1));
              }
              machine.popValues(node.internalChildren.size());
              toReturn = ((PrimitiveFunction)toReturn.val).call(args);
            }
            machine.pushValue(toReturn);
            machine.ipPop();
      });
  }

  static MachineContext.NodeHandlers assignmentLValueHandlers = new MachineContext.NodeHandlers();
  static {
    assignmentLValueHandlers
      .add(Rule.AssignmentExpression_Expression_AssignmentExpressionMore,
          (MachineContext machine, AstNode node, int idx) -> {
            switch(idx)
            {
            case 0:
              if (node.children.get(1).matchesRule(Rule.AssignmentExpressionMore))
              {
                // This isn't an assignment, just an expression evaluation
                machine.ipSetIdx(1);
              }
              else
              {
                // This is an assignment, so handle that specially
                machine.ipSetIdx(16);
              }
              break;
            case 1:
              // Not an assignment, just evaluate things as a normal expression
              machine.ipPushAndAdvanceIdx(node.children.get(0), expressionHandlers);
              break;
            case 2:
              // Not an assignment, after evaluating the children as an expression
              machine.popValue();
              machine.ipPop();
              break;
            case 16:
              // An assignment, figure out what we're assignment to
              machine.ipPushAndAdvanceIdx(node.children.get(0), assignmentLValueHandlers);
              // Just for safety, make sure unexpected things aren't on the stack in later steps
              machine.setLValueAssertCheck(machine.valueStackSize());
              break;
            case 17:
              // An assignment, figured out LValue, make sure nothing weird is on the stack 
              if (machine.valueStackSize() != machine.getLValueAssertCheck())
                throw new RunException();
              // Now figure out the value to be assigned
              machine.ipPushAndAdvanceIdx(node.children.get(1), expressionHandlers);
              break;
            case 18:
              // An assignment, gathered all needed data, so perform the assignment
              LValue lval = machine.popLValue();
              Value val = machine.popValue();
              if (machine.valueStackSize() != machine.getLValueAssertCheck())
                throw new RunException();
              if (machine.lValueStackSize() != 0)
                throw new RunException();
              if (lval.sourceScope == null)
                throw new RunException();
              lval.sourceScope.assignTo(lval.sourceBinding, val);
              machine.ipPop();
              break;
            }
      })
      .add(Rule.AdditiveExpressionMore_Plus_AdditiveExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            throw new RunException();
      })
      .add(Rule.AdditiveExpressionMore_Minus_AdditiveExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            throw new RunException();
      })
      .add(Rule.MultiplicativeExpressionMore_Multiply_MultiplicativeExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            throw new RunException();
      })
      .add(Rule.MultiplicativeExpressionMore_Divide_MultiplicativeExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            throw new RunException();
      })
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0:
              LValue toReturn = machine.scope.lookupLValue(((Token.ParameterToken)node.token).getLookupName());
              if (toReturn.type.isFunction())
              {
                machine.ipPushAndAdvanceIdx(node, expressionHandlers);
              }
              else
              {
                machine.pushLValue(toReturn);
                machine.ipPop();
              }
              break;
            case 1:
              // Not sure what to do with the result of a function call
              throw new RunException();
            }
      });
  }

}
