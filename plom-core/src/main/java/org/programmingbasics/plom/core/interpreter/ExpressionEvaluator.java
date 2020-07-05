package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.MachineContext.MachineNodeVisitor;
import org.programmingbasics.plom.core.interpreter.PrimitiveFunction.PrimitiveBlockingFunction;
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
        machine.ip.pushAndAdvanceIdx(node.children.get(1), expressionHandlers);
        break;
      case 1:
        {
          Value right = machine.popValue();
          Value left = machine.popValue();
          Value toReturn = doOp.apply(left, right);
          machine.pushValue(toReturn);
          machine.ip.advanceIdx();
          break;
        }
      case 2:
        machine.ip.pushAndAdvanceIdx(node.children.get(2), expressionHandlers);
        break;
      case 3:
        machine.ip.pop();
      }
    };
  }

  
  static MachineContext.NodeHandlers expressionHandlers = new MachineContext.NodeHandlers();
  static {
    expressionHandlers
      .add(Rule.AdditiveExpressionMore_Plus_MultiplicativeExpression_AdditiveExpressionMore,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createNumberValue(left.getNumberValue() + right.getNumberValue());
            else if (Type.STRING.equals(left.type) && Type.STRING.equals(right.type))
              return Value.createStringValue(left.getStringValue() + right.getStringValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.AdditiveExpressionMore_Minus_MultiplicativeExpression_AdditiveExpressionMore,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
                return Value.createNumberValue(left.getNumberValue() - right.getNumberValue());
              else
                throw new RunException();
          })
      )
      .add(Rule.MultiplicativeExpressionMore_Multiply_MemberExpression_MultiplicativeExpressionMore,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createNumberValue(left.getNumberValue() * right.getNumberValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.MultiplicativeExpressionMore_Divide_MemberExpression_MultiplicativeExpressionMore,
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createNumberValue(left.getNumberValue() / right.getNumberValue());
            else
              throw new RunException();
          })
      )
      .add(Rule.RelationalExpressionMore_Eq_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() == right.getNumberValue());
            else if (Type.STRING.equals(left.type) && Type.STRING.equals(right.type))
              return Value.createBooleanValue(left.getStringValue().equals(right.getStringValue()));
            else if (Type.BOOLEAN.equals(left.type) && Type.BOOLEAN.equals(right.type))
              return Value.createBooleanValue(left.getBooleanValue() == right.getBooleanValue());
            else if (Type.NULL.equals(left.type) && Type.NULL.equals(right.type))
              return Value.TRUE;
            else
              return Value.FALSE;
          })
      )
      .add(Rule.RelationalExpressionMore_Ne_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() != right.getNumberValue());
            else if (left.type == Type.STRING && right.type == Type.STRING)
              return Value.createBooleanValue(!left.getStringValue().equals(right.getStringValue()));
            else if (left.type == Type.BOOLEAN && right.type == Type.BOOLEAN)
              return Value.createBooleanValue(left.getBooleanValue() != right.getBooleanValue());
            else if (left.type == Type.NULL && right.type == Type.NULL)
              return Value.FALSE;
            else
              return Value.TRUE;
          })
      )
      .add(Rule.RelationalExpressionMore_Gt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() > right.getNumberValue());
            throw new RunException();
          })
      )
      .add(Rule.RelationalExpressionMore_Ge_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() >= right.getNumberValue());
            throw new RunException();
          })
      )
      .add(Rule.RelationalExpressionMore_Lt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() < right.getNumberValue());
            throw new RunException();
          })
      )
      .add(Rule.RelationalExpressionMore_Le_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMore((left, right) -> {
            if (Type.NUMBER.equals(left.type) && Type.NUMBER.equals(right.type))
              return Value.createBooleanValue(left.getNumberValue() <= right.getNumberValue());
            throw new RunException();
          })
      )
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore, 
          (machine, node, idx) -> {
            switch(idx)
            {
            case 0:
              Value left = machine.popValue();
              if (!Type.BOOLEAN.equals(left.type)) throw new RunException();
              if (left.getBooleanValue())
              {
                machine.pushValue(Value.createBooleanValue(true));
                machine.ip.advanceIdx();
              }
              else
                machine.ip.pushAndAdvanceIdx(node.children.get(1), expressionHandlers);
              break;
            case 1:
              machine.ip.pushAndAdvanceIdx(node.children.get(2), expressionHandlers);
              break;
            case 2:
              machine.ip.pop();
              break;
            }
          }
      )
      .add(Rule.AndExpressionMore_And_RelationalExpression_AndExpressionMore, 
          (machine, node, idx) -> {
            switch(idx)
            {
            case 0:
              Value left = machine.popValue();
              if (!Type.BOOLEAN.equals(left.type)) throw new RunException();
              if (!left.getBooleanValue())
              {
                machine.pushValue(Value.createBooleanValue(false));
                machine.ip.advanceIdx();
              }
              else
                machine.ip.pushAndAdvanceIdx(node.children.get(1), expressionHandlers);
              break;
            case 1:
              machine.ip.pushAndAdvanceIdx(node.children.get(2), expressionHandlers);
              break;
            case 2:
              machine.ip.pop();
              break;
            }
          }
      )
      .add(Rule.String, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.STRING;
            String rawStr = ((Token.SimpleToken)node.token).contents;
            val.val = rawStr.substring(1, rawStr.length() - 1);
            machine.pushValue(val);
            machine.ip.pop();
      })
      .add(Rule.Number, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = new Value();
            val.type = Type.NUMBER;
            val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
            machine.pushValue(val);
            machine.ip.pop();
      })
      .add(Rule.TrueLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(Value.TRUE);
            machine.ip.pop();
      })
      .add(Rule.FalseLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(Value.FALSE);
            machine.ip.pop();
      })
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx < node.internalChildren.size())
            {
              machine.ip.pushAndAdvanceIdx(node.internalChildren.get(idx), expressionHandlers);
              return;
            }
            Value toReturn = machine.currentScope().lookup(((Token.ParameterToken)node.token).getLookupName());
            if (toReturn.type.isFunction())
            {
              List<Value> args = new ArrayList<>();
              for (int n = 0; n < node.internalChildren.size(); n++)
              {
                args.add(machine.readValue(node.internalChildren.size() - n - 1));
              }
              machine.popValues(node.internalChildren.size());
              if (toReturn.type.isPrimitiveNonBlockingFunction())
              {
                toReturn = ((PrimitiveFunction)toReturn.val).call(args);
                machine.pushValue(toReturn);
                machine.ip.pop();
                return;
              }
              else if (toReturn.type.isPrimitiveBlockingFunction())
              {
                MachineContext.PrimitiveBlockingFunctionReturn blockWait = new MachineContext.PrimitiveBlockingFunctionReturn(); 
                ((PrimitiveBlockingFunction)toReturn.val).call(blockWait, args);
                machine.waitOnBlockingFunction(blockWait);
                machine.ip.pop();
                return;
              }
              else 
                throw new RunException();
            }
            machine.pushValue(toReturn);
            machine.ip.pop();
      });
  }

  /** Just an easy way to throw an exception in lvalue code */
  static MachineNodeVisitor lValueInvalid() { 
    return (MachineContext machine, AstNode node, int idx) -> {
      throw new RunException();
    };
  };

  /** Handlers for reading the lValue of an assignment */
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
                machine.ip.setIdx(1);
              }
              else
              {
                // This is an assignment, so handle that specially
                machine.ip.setIdx(16);
              }
              break;
            case 1:
              // Not an assignment, just evaluate things as a normal expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0), expressionHandlers);
              break;
            case 2:
              // Not an assignment, after evaluating the children as an expression
              machine.popValue();
              machine.ip.pop();
              break;
            case 16:
              // An assignment, figure out what we're assignment to
              machine.ip.pushAndAdvanceIdx(node.children.get(0), assignmentLValueHandlers);
              // Just for safety, make sure unexpected things aren't on the stack in later steps
              machine.setLValueAssertCheck(machine.valueStackSize());
              break;
            case 17:
              // An assignment, figured out LValue, make sure nothing weird is on the stack 
              if (machine.valueStackSize() != machine.getLValueAssertCheck())
                throw new RunException();
              // Now figure out the value to be assigned
              machine.ip.pushAndAdvanceIdx(node.children.get(1), expressionHandlers);
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
              machine.ip.pop();
              break;
            }
      })
      .add(Rule.AdditiveExpressionMore_Plus_MultiplicativeExpression_AdditiveExpressionMore, lValueInvalid())
      .add(Rule.AdditiveExpressionMore_Minus_MultiplicativeExpression_AdditiveExpressionMore, lValueInvalid())
      .add(Rule.MultiplicativeExpressionMore_Multiply_MemberExpression_MultiplicativeExpressionMore, lValueInvalid())
      .add(Rule.MultiplicativeExpressionMore_Divide_MemberExpression_MultiplicativeExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Eq_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Ne_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Gt_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Ge_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Lt_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Le_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore, lValueInvalid())
      .add(Rule.AndExpressionMore_And_RelationalExpression_AndExpressionMore, lValueInvalid())
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0:
              LValue toReturn = machine.currentScope().lookupLValue(((Token.ParameterToken)node.token).getLookupName());
              if (toReturn.type.isFunction())
              {
                machine.ip.pushAndAdvanceIdx(node, expressionHandlers);
              }
              else
              {
                machine.pushLValue(toReturn);
                machine.ip.pop();
              }
              break;
            case 1:
              // Not sure what to do with the result of a function call
              throw new RunException();
            }
      });
  }

}
