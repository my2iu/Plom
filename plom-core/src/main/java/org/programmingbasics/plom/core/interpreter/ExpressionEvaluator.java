package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.MachineContext.MachineNodeVisitor;
import org.programmingbasics.plom.core.interpreter.PrimitiveFunction.PrimitiveBlockingFunction;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.GatheredTypeInfo;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

public class ExpressionEvaluator
{
  VariableScope scope;

  static interface BinaryOperatorHandler
  {
    public Value apply(MachineContext ctx, Value left, Value right) throws RunException;
  }
  
//  private static BinaryOperatorHandler createBinaryOperatorToPrimitiveMethodCall(String methodName)
//  {
//    return (ctx, left, right) -> {
//      PrimitiveFunction.PrimitiveMethod primitiveMethod = left.type.lookupPrimitiveMethod(methodName);
//      if (primitiveMethod == null)
//        throw new RunException();
//      return primitiveMethod.call(left, Collections.singletonList(right));
//    };
//  }

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
          Value toReturn = doOp.apply(machine, left, right);
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

  // The pattern of binary operators expressed in an LL1 grammar with a More rule is pretty common,
  // and we often passthrough to a method, so we have a function for easily making handlers for that situation 
  static MachineContext.MachineNodeVisitor createBinaryOperatorHandlerMoreForMethodPassthrough(String methodName)
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
          ExecutableFunction method = left.type.lookupMethod(methodName);
          if (method == null)
            throw new RunException();
          Value self = left;
          machine.ip.advanceIdx();
          machine.pushStackFrame(method.code, method.codeUnit, SimpleInterpreter.statementHandlers);
          machine.pushObjectScope(self);
          machine.pushNewScope();
          machine.currentScope().addVariable(method.argPosToName.get(0), right.type, right);
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
          createBinaryOperatorHandlerMoreForMethodPassthrough("+:")
      )
      .add(Rule.AdditiveExpressionMore_Minus_MultiplicativeExpression_AdditiveExpressionMore,
          createBinaryOperatorHandlerMoreForMethodPassthrough("-:")
      )
      .add(Rule.MultiplicativeExpressionMore_Multiply_MemberExpression_MultiplicativeExpressionMore,
          createBinaryOperatorHandlerMoreForMethodPassthrough("*:")
      )
      .add(Rule.MultiplicativeExpressionMore_Divide_MemberExpression_MultiplicativeExpressionMore,
          createBinaryOperatorHandlerMoreForMethodPassthrough("/:")
      )
      .add(Rule.RelationalExpressionMore_Eq_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough("=:")
      )
      .add(Rule.RelationalExpressionMore_Ne_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough("!=:")
      )
      .add(Rule.RelationalExpressionMore_Gt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough(">:")
      )
      .add(Rule.RelationalExpressionMore_Ge_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough(">=:")
      )
      .add(Rule.RelationalExpressionMore_Lt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough("<:")
      )
      .add(Rule.RelationalExpressionMore_Le_AdditiveExpression_RelationalExpressionMore, 
          createBinaryOperatorHandlerMoreForMethodPassthrough("<=:")
      )
      .add(Rule.RelationalExpressionMore_Retype_AtType_RelationalExpressionMore, 
          (machine, node, idx) -> {
            GatheredTypeInfo typeInfo = new GatheredTypeInfo();
            node.children.get(1).recursiveVisit(SimpleInterpreter.typeParsingHandlers, typeInfo, machine);
            Type retypeType = typeInfo.type;
            Value left = machine.popValue();
            if (left.isNull())
            {
              machine.pushValue(left);
            }
            else
            {
              Value retypedValue = Value.create(left.val, retypeType);
              machine.pushValue(retypedValue);
            }
            machine.ip.pop();
          }
      )
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore, 
          (machine, node, idx) -> {
            switch(idx)
            {
            case 0:
              Value left = machine.popValue();
              if (!machine.coreTypes().getBooleanType().equals(left.type)) throw new RunException();
              if (left.getBooleanValue())
              {
                machine.pushValue(Value.createBooleanValue(machine.coreTypes(), true));
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
              if (!machine.coreTypes().getBooleanType().equals(left.type)) throw new RunException();
              if (!left.getBooleanValue())
              {
                machine.pushValue(Value.createBooleanValue(machine.coreTypes(), false));
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
            String rawStr = ((Token.SimpleToken)node.token).contents;
            Value val = Value.createStringValue(machine.coreTypes(), rawStr.substring(1, rawStr.length() - 1));
            machine.pushValue(val);
            machine.ip.pop();
      })
      .add(Rule.Number, 
          (MachineContext machine, AstNode node, int idx) -> {
            Value val = Value.createNumberValue(machine.coreTypes(), Double.parseDouble(((Token.SimpleToken)node.token).contents));
            machine.pushValue(val);
            machine.ip.pop();
      })
      .add(Rule.TrueLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(machine.coreTypes().getTrueValue());
            machine.ip.pop();
      })
      .add(Rule.FalseLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(machine.coreTypes().getFalseValue());
            machine.ip.pop();
      })
      .add(Rule.NullLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(machine.coreTypes().getNullValue());
            machine.ip.pop();
      })
      .add(Rule.This, 
          (MachineContext machine, AstNode node, int idx) -> {
            machine.pushValue(machine.currentScope().lookupThis());
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
            if (toReturn.type.isCallable())
            {
              if (toReturn.type.isNormalFunction())
              {
                ExecutableFunction fn = (ExecutableFunction)toReturn.val;
                callMethodOrFunction(machine, node, null, fn, false);
                return;
              }
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
      })
      .add(Rule.DotMember_DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            AstNode methodNode = node.children.get(0);
            if (idx < methodNode.internalChildren.size())
            {
              machine.ip.pushAndAdvanceIdx(methodNode.internalChildren.get(idx), expressionHandlers);
              return;
            }
            else 
            {
              Value self = machine.readValue(methodNode.internalChildren.size());
              ExecutableFunction method = self.type.lookupMethod(((Token.ParameterToken)methodNode.token).getLookupName());
              if (method != null)
              {
                callMethodOrFunction(machine, methodNode, self, method, false);
                return;
              }
              else
              {
                PrimitiveFunction.PrimitiveMethod primitiveMethod = self.type.lookupPrimitiveMethod(((Token.ParameterToken)methodNode.token).getLookupName());
                if (primitiveMethod == null)
                  throw new RunException();
                List<Value> args = new ArrayList<>();
                for (int n = 0; n < methodNode.internalChildren.size(); n++)
                {
                  args.add(machine.readValue(methodNode.internalChildren.size() - n - 1));
                }
                machine.popValues(methodNode.internalChildren.size() + 1);
                Value toReturn = primitiveMethod.call(self, args);
                machine.pushValue(toReturn);
                machine.ip.pop();
              }
            }
      })
      .add(Rule.StaticMethodCallExpression_AtType_DotMember, 
          (MachineContext machine, AstNode node, int idx) -> {
            AstNode methodNode = node.children.get(1).children.get(0);
            if (idx < methodNode.internalChildren.size())
            {
              machine.ip.pushAndAdvanceIdx(methodNode.internalChildren.get(idx), expressionHandlers);
              return;
            }
            else if (idx == methodNode.internalChildren.size()) 
            {
              GatheredTypeInfo typeInfo = new GatheredTypeInfo();
              node.children.get(0).recursiveVisit(SimpleInterpreter.typeParsingHandlers, typeInfo, machine);
              Type calleeType = typeInfo.type;
//              Value self = machine.readValue(methodNode.internalChildren.size());
              ExecutableFunction method = calleeType.lookupStaticMethod(((Token.ParameterToken)methodNode.token).getLookupName());
              if (method != null)
              {
                if (method.codeUnit.isStatic)
                {
                  callMethodOrFunction(machine, methodNode, null, method, false);
                }
                else if (method.codeUnit.isConstructor)
                {
                  // Create empty object
                  Value self = Value.createEmptyObject(machine.coreTypes(), calleeType);
                  // We need to do adjust the return value of the constructor so that
                  // it points to the new object
                  machine.ip.advanceIdx();
                  // Call constructor on the empty object to configure it
                  callMethodOrFunction(machine, methodNode, self, method, true);
                }
                return;
              }
              else
              {
                throw new RunException();
              }
            }
            else  // idx == methodNode.internalChildren.size() + 1
            {
              // For constructors, we get control after returning from the constructor
              // so that we can pop off the result of the constructor method (i.e. void),
              // leaving only the constructed object
              machine.popValue();
              machine.ip.pop();
            }
      });
  }



  static void callMethodOrFunction(MachineContext machine, AstNode methodNode,
      Value self, ExecutableFunction method, boolean isConstructor)
  {
    List<Value> args = new ArrayList<>();
    for (int n = 0; n < methodNode.internalChildren.size(); n++)
    {
      args.add(machine.readValue(methodNode.internalChildren.size() - n - 1));
    }
    if (self != null && !isConstructor)
      machine.popValues(methodNode.internalChildren.size() + 1);
    else
      machine.popValues(methodNode.internalChildren.size());

    if (!isConstructor)
      machine.ip.pop();
    else
      machine.pushValue(self);
    machine.pushStackFrame(method.code, method.codeUnit, SimpleInterpreter.statementHandlers);
    if (self != null)
      machine.pushObjectScope(self);
    machine.pushNewScope();
    for (int n = 0; n < method.argPosToName.size(); n++)
    {
      machine.currentScope().addVariable(method.argPosToName.get(n), machine.coreTypes().getObjectType(), args.get(n));
    }
//    if (self != null)
//      machine.currentScope().setThis(self);
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
      .add(Rule.RelationalExpressionMore_Retype_AtType_RelationalExpressionMore, lValueInvalid())
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore, lValueInvalid())
      .add(Rule.AndExpressionMore_And_RelationalExpression_AndExpressionMore, lValueInvalid())
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0:
              LValue toReturn = machine.currentScope().lookupLValue(((Token.ParameterToken)node.token).getLookupName());
              if (toReturn.type.isCallable())
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
