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
            throw RunException.withLocationFromNode("Cannot find method @" + left.type.name + " ." + methodName, node, machine);
          Value self = left;
          machine.ip.advanceIdx();
          machine.pushValue(self);
          machine.pushValue(right);
          callMethodOrFunction(machine, self, method, false, null, false, null);
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
      .add(Rule.MemberExpressionMore_As_Type_MemberExpressionMore,
          (machine, node, idx) -> {
            switch(idx)
            {
            case 0:
              machine.ip.pushAndAdvanceIdx(node.children.get(2), expressionHandlers);
              break;
            case 1:
              machine.ip.pop();
              break;
            }
          }
      )
      .add(Rule.RelationalExpressionMore_Is_Type_RelationalExpressionMore,
          (machine, node, idx) -> {
            switch(idx)
            {
            case 0:
            {
              Type isType = machine.currentScope().typeFromUnboundTypeFromScope(VariableDeclarationInterpreter.gatherUnboundTypeInfo(node.children.get(1)));
              Value left = machine.popValue();
              machine.pushValue(Value.createBooleanValue(machine.coreTypes(), left.type.isInstanceOf(isType)));
              // Handle the "...More" part 
              machine.ip.pushAndAdvanceIdx(node.children.get(1), expressionHandlers);
              break;
            }
            case 1:
              machine.ip.pop();
              break;
            }
          }
      )
      .add(Rule.RelationalExpressionMore_Retype_Type_RelationalExpressionMore, 
          (machine, node, idx) -> {
            Type retypeType = machine.currentScope().typeFromUnboundTypeFromScope(VariableDeclarationInterpreter.gatherUnboundTypeInfo(node.children.get(1)));
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
            try {
              machine.pushValue(machine.currentScope().lookupThis());
            }
            catch (RunException e)
            {
              throw e.addProgramLocationFromNodeIfNeeded(node, machine);
            }
            machine.ip.pop();
      })
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx < node.internalChildren.size())
            {
              machine.ip.pushAndAdvanceIdx(node.internalChildren.get(idx), expressionHandlers);
              return;
            }
            Value toReturn;
            try {
              toReturn = machine.currentScope().lookup(((Token.ParameterToken)node.token).getLookupName());
            }
            catch (RunException e)
            {
              throw e.addProgramLocationFromNodeIfNeeded(node, machine);
            }
            if (toReturn.type.isCallable())
            {
              if (toReturn.type.isNormalFunction())
              {
                ExecutableFunction fn = (ExecutableFunction)toReturn.val;
                machine.ip.pop();
                callMethodOrFunction(machine, null, fn, false, null, false, null);
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
              if (self.type instanceof Type.LambdaFunctionType)
              {
                // We're calling a lambda function
                Type.LambdaFunctionType lambdaType = (Type.LambdaFunctionType)self.type;
                if (!((Token.ParameterToken)methodNode.token).getLookupName().equals(lambdaType.name))
                  throw RunException.withLocationFromNode("Lambda function does not match", methodNode, machine);
                LambdaFunction lambda = (LambdaFunction)self.val;
                machine.ip.pop();
                callMethodOrFunction(machine, null, lambda.toExecutableFunction(), false, null, true, lambda.closureScope);
                return;
              }
              String lookupName = ((Token.ParameterToken)methodNode.token).getLookupName();
              ExecutableFunction method = self.type.lookupMethod(lookupName);
              if (method != null)
              {
                machine.ip.pop();
                callMethodOrFunction(machine, self, method, false, null, false, null);
                return;
              }
              else
              {
                PrimitiveFunction.PrimitiveMethod primitiveMethod = self.type.lookupPrimitiveMethod(((Token.ParameterToken)methodNode.token).getLookupName());
                if (primitiveMethod == null)
                  throw RunException.withLocationFromNode("Cannot find method @" + self.type.name + " ." + lookupName, methodNode, machine);
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
              Type calleeType = machine.currentScope().typeFromUnboundTypeFromScope(VariableDeclarationInterpreter.gatherUnboundTypeInfo(node.children.get(0)));
              String lookupName = ((Token.ParameterToken)methodNode.token).getLookupName();
              ExecutableFunction method = calleeType.lookupStaticMethod(lookupName);
              if (method != null)
              {
                if (method.codeUnit.isStatic)
                {
                  machine.ip.pop();
                  callMethodOrFunction(machine, null, method, false, null, false, null);
                }
                else if (method.codeUnit.isConstructor)
                {
                  machine.ip.pop();
                  // Call constructor on the empty object to configure it
                  callMethodOrFunction(machine, null, method, true, calleeType, false, null);
                }
                return;
              }
              else
              {
                throw RunException.withLocationFromNode("Cannot find static method with the name ." + lookupName, methodNode, machine);
              }
            }
            else  // idx == methodNode.internalChildren.size() + 1
            {
              throw new IllegalArgumentException("This should be unreachable");
            }
      })
      .add(Rule.DotSuperMember_DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            // Currently we only support constructor chaining, so check if we're in a constructor
            if (machine.getTopStackFrame().constructorConcreteType != null)
            {
              // TODO: Don't use getClassFromStackFrame(). Instead, we should
              //   do a lookup through to the ObjectScope to get it
              Type constructorType = getClassFromStackFrame(machine).parent;
              AstNode methodNode = node.children.get(0);
              if (idx < methodNode.internalChildren.size())
              {
                machine.ip.pushAndAdvanceIdx(methodNode.internalChildren.get(idx), expressionHandlers);
                return;
              }
              else if (idx == methodNode.internalChildren.size()) 
              {
                ExecutableFunction method = constructorType.lookupStaticMethod(((Token.ParameterToken)methodNode.token).getLookupName());
                if (method != null)
                {
                  if (method.codeUnit.isConstructor)
                  {
                    machine.ip.advanceIdx();
                    callMethodOrFunction(machine, null, method, true, machine.getTopStackFrame().constructorConcreteType, false, null);
                  }
                  else
                    throw RunException.withLocationFromNode("Can only use super to call other constructors from within a constructor", methodNode, machine);
                  return;
                }
                else
                {
                  throw RunException.withLocationFromNode("Cannot find super method with that name", methodNode, machine);
                }
              }
              else  // idx == methodNode.internalChildren.size() + 1
              {
                ExecutableFunction method = constructorType.lookupStaticMethod(((Token.ParameterToken)methodNode.token).getLookupName());
                if (method != null)
                {
                  if (method.codeUnit.isConstructor)
                  {
                    // Upon returning from calling a chained constructor, we should use the
                    // returned value to set the current "this" object
                    Value thisValue = machine.popValue(); 
                    machine.currentScope().overwriteThis(thisValue);
                    machine.pushValue(Value.createVoidValue(machine.coreTypes()));
                    machine.ip.pop();
                  }
                }
              }
              return;
            }
            throw new IllegalArgumentException("super is only supported for use in constructor chaining at the moment");
            
//            AstNode methodNode = node.children.get(1).children.get(0);
//            if (idx < methodNode.internalChildren.size())
//            {
//              machine.ip.pushAndAdvanceIdx(methodNode.internalChildren.get(idx), expressionHandlers);
//              return;
//            }
//            else if (idx == methodNode.internalChildren.size()) 
//            {
//              GatheredTypeInfo typeInfo = new GatheredTypeInfo();
//              node.children.get(0).recursiveVisit(SimpleInterpreter.typeParsingHandlers, typeInfo, machine);
//              Type calleeType = typeInfo.type;
////              Value self = machine.readValue(methodNode.internalChildren.size());
//              ExecutableFunction method = calleeType.lookupStaticMethod(((Token.ParameterToken)methodNode.token).getLookupName());
//              if (method != null)
//              {
//                if (method.codeUnit.isStatic)
//                {
//                  machine.ip.pop();
//                  callMethodOrFunction(machine, null, method, false, null);
//                }
//                else if (method.codeUnit.isConstructor)
//                {
//                  // Create empty object
////                  Value self = Value.createEmptyObject(machine.coreTypes(), calleeType);
//                  machine.ip.pop();
//                  // Call constructor on the empty object to configure it
//                  callMethodOrFunction(machine, null, method, true, calleeType);
//                }
//                return;
//              }
//              else
//              {
//                throw new RunException();
//              }
//            }
//            else  // idx == methodNode.internalChildren.size() + 1
//            {
//              throw new IllegalArgumentException("This should be unreachable");
//            }
      })
      .add(Rule.FunctionLiteral, 
          (MachineContext machine, AstNode node, int idx) -> {
            AstNode functionTypeAst = node.internalChildren.get(0);
            UnboundType unboundFunctionType = VariableDeclarationInterpreter.gatherUnboundTypeInfo(functionTypeAst);
            Type.LambdaFunctionType functionType = (Type.LambdaFunctionType)machine.currentScope().typeFromUnboundTypeFromScope(unboundFunctionType);
            AstNode contentsAst = node.internalChildren.get(1);
            // Not sure if using the codeUnit for the whole method is the right choice for the codeUnit for a lambda inside that method
            CodeUnitLocation codeUnit = machine.getTopStackFrame().codeUnit;
            List<String> argPosToName = new ArrayList<>();
            for (String argName: functionType.optionalArgNames)
            {
              if (argName == null)
                argPosToName.add("");
              else 
                argPosToName.add(argName);
            }
            LambdaFunction fun = new LambdaFunction();
            fun.functionBody = contentsAst;
            fun.codeUnit = codeUnit;
            fun.sourceLookup = machine.getTopStackFrame().sourceLookup;
            fun.argPosToName = argPosToName;
            fun.closureScope = machine.currentScope();
//            fun.self = machine.currentScope().lookupThisOrNull();
            Value val = Value.create(fun, functionType);
            machine.pushValue(val);
            machine.ip.pop();
          })
      ;
  }


  private static Type getClassFromStackFrame(MachineContext machine) throws RunException
  {
    return machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(machine.getTopStackFrame().codeUnit.className));
  }


  /**
   * @param constructorType when the method is a constructor, this parameter
   *   gives the concrete type that should be constructed by the constructor
   * @param closureScope TODO
   */
  static void callMethodOrFunction(MachineContext machine, Value self,
      ExecutableFunction method, boolean isConstructor, Type constructorType, boolean isLambda, VariableScope closureScope)
  {
    // Transfer arguments off of the value stack 
    int numArgs = method.argPosToName.size();
    List<Value> args = new ArrayList<>();
    for (int n = 0; n < numArgs; n++)
    {
      args.add(machine.readValue(numArgs - n - 1));
    }
    if (isLambda || (self != null && !isConstructor))
      machine.popValues(numArgs + 1);
    else
      machine.popValues(numArgs);

    callMethodOrFunctionNoStack(machine, self, method, isConstructor, constructorType, isLambda,
        closureScope, args);
  }

  /**
   * Calls a method or function, but with all the necessary function arguments
   * already removed from the stack and put into a List (this is useful because
   * we sometimes want to call directly into a function/method from outside (like
   * from JavaScript) and we don't want to create a dummy stack frame to
   * store arguments and other data just to pop them off again) 
   */
  static void callMethodOrFunctionNoStack(MachineContext machine, Value self,
      ExecutableFunction method, boolean isConstructor, Type constructorType,
      boolean isLambda, VariableScope closureScope, List<Value> args)
  {
    // Push a new stack frame for the function and set up other variable scope
    if (!isLambda)
    {
      machine.pushStackFrame(method.code, method.codeUnit, method.sourceLookup, constructorType, SimpleInterpreter.statementHandlers);
      if (isConstructor)
        machine.pushConstructorScope(method.owningClass);
      else if (self != null)
        machine.pushObjectScope(self);
    }
    else
      machine.pushStackFrame(method.code, method.codeUnit, method.sourceLookup, closureScope, constructorType, SimpleInterpreter.statementHandlers);

    // Push arguments onto the scope stack
    machine.pushNewScope();
    for (int n = 0; n < method.argPosToName.size(); n++)
    {
      machine.currentScope().addVariable(method.argPosToName.get(n), machine.coreTypes().getObjectType(), args.get(n));
    }
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
      .add(Rule.MemberExpressionMore_As_Type_MemberExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Is_Type_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Eq_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Ne_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Gt_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Ge_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Lt_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Le_AdditiveExpression_RelationalExpressionMore, lValueInvalid())
      .add(Rule.RelationalExpressionMore_Retype_Type_RelationalExpressionMore, lValueInvalid())
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore, lValueInvalid())
      .add(Rule.AndExpressionMore_And_RelationalExpression_AndExpressionMore, lValueInvalid())
      .add(Rule.DotVariable, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0:
              LValue toReturn;
              try {
                toReturn = machine.currentScope().lookupLValue(((Token.ParameterToken)node.token).getLookupName());
              } 
              catch (RunException e)
              {
                throw e.addProgramLocationFromNodeIfNeeded(node, machine);
              }
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
