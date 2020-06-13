package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.AstNode.RecursiveWalkerVisitor;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class ExpressionEvaluator
{
  VariableScope scope;

  // Makes it easy to chain together different handlers
  static class EvalHandlerChainer implements RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException> 
  {
    List<RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException>> chain = new ArrayList<>();
    @Override public boolean visit(
        VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers,
        AstNode node, ReturnedValue param1, MachineContext param2)
        throws RunException
    {
      for (RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException> handler: chain)
      {
        if (handler.visit(triggers, node, param1, param2))
          return true;
      }
      return false;
    }
    EvalHandlerChainer chainTo(RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException> next)
    {
      chain.add(next);
      return this;
    }
  }
  
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

  
  // The pattern of binary operators expressed in an LL1 grammar with a More rule is pretty common,
  // so we have a function for making handlers for the case where there is no more
  static RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException> createBinaryOperatorTestEmptyMore(
      List<Symbol> rightEmptyRule)
  {
    return (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
      if (node.children.get(1).matchesRule(rightEmptyRule))
      {
        node.recursiveVisitChildren(triggers, returned, context);
        return true;
      }
      return false;
    };
  }
  
  // The pattern of binary operators expressed in an LL1 grammar with a More rule is pretty common,
  // so we have a function for making handlers for the case where there is a binary operator to be handled
  static RecursiveWalkerVisitor<ReturnedValue, MachineContext, RunException> createBinaryOperatorTestMore(
      List<Symbol> matchRule, BinaryOperatorHandler doOp)
  {
    return (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
      if (node.children.get(1).matchesRule(matchRule))
      {
        node.children.get(0).recursiveVisit(triggers, returned, context);
        Value left = returned.val;
        AstNode more = node.children.get(1); 
        more.recursiveVisit(triggers, returned, context);
        Value right = returned.val;
        returned.val = doOp.apply(left, right);
        return true;
      }
      return false;
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

  
  public static class ReturnedValue
  {
    Value val;
  }
  static AstNode.VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers = new AstNode.VisitorTriggers<ReturnedValue, MachineContext, RunException>()
      .add(Rule.AdditiveExpression_MultiplicativeExpression_AdditiveExpressionMore, new EvalHandlerChainer()
          .chainTo(createBinaryOperatorTestEmptyMore(Rule.AdditiveExpressionMore))
          .chainTo(createBinaryOperatorTestMore(Rule.AdditiveExpressionMore_Plus_AdditiveExpression, (Value left, Value right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() + right.getNumberValue());
            else if (left.type == Type.STRING && right.type == Type.STRING)
              return Value.createStringValue(left.getStringValue() + right.getStringValue());
            else
              throw new RunException();
          }))
          .chainTo(createBinaryOperatorTestMore(Rule.AdditiveExpressionMore_Minus_AdditiveExpression, (Value left, Value right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() - right.getNumberValue());
            else
              throw new RunException();
          })))
      .add(Rule.MultiplicativeExpression_MemberExpression_MultiplicativeExpressionMore, new EvalHandlerChainer()
          .chainTo(createBinaryOperatorTestEmptyMore(Rule.MultiplicativeExpressionMore))
          .chainTo(createBinaryOperatorTestMore(Rule.MultiplicativeExpressionMore_Multiply_MultiplicativeExpression, (Value left, Value right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() * right.getNumberValue());
            else
              throw new RunException();
          }))
          .chainTo(createBinaryOperatorTestMore(Rule.MultiplicativeExpressionMore_Divide_MultiplicativeExpression, (Value left, Value right) -> {
            if (left.type == Type.NUMBER && right.type == Type.NUMBER)
              return Value.createNumberValue(left.getNumberValue() / right.getNumberValue());
            else
              throw new RunException();
          })))
      .add(Rule.AssignmentExpression_Expression_AssignmentExpressionMore, new EvalHandlerChainer()
          .chainTo(createBinaryOperatorTestEmptyMore(Rule.AssignmentExpressionMore))
          .chainTo(createBinaryOperatorTestMore(Rule.AssignmentExpressionMore_Assignment_Expression, (Value left, Value right) -> {
            if (left.sourceScope == null)
              throw new RunException();
            left.sourceScope.assignTo(left.sourceBinding, right);
            return null;
          })))
      .add(Rule.String, 
          (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
            returned.val = new Value();
            returned.val.type = Type.STRING;
            String rawStr = ((Token.SimpleToken)node.token).contents;
            returned.val.val = rawStr.substring(1, rawStr.length() - 1);
            return true;
      })
      .add(Rule.Number, 
          (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
            returned.val = new Value();
            returned.val.type = Type.NUMBER;
            returned.val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
            return true;
      })
      .add(Rule.TrueLiteral, 
          (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
            returned.val = new Value();
            returned.val.type = Type.BOOLEAN;
            returned.val.val = Boolean.TRUE;
            return true;
      })
      .add(Rule.FalseLiteral, 
          (triggers, node, returned, context) -> {
            returned.val = new Value();
            returned.val.type = Type.BOOLEAN;
            returned.val.val = Boolean.FALSE;
            return true;
      })
      .add(Rule.DotVariable, 
          (VisitorTriggers<ReturnedValue, MachineContext, RunException> triggers, AstNode node, ReturnedValue returned, MachineContext context) -> {
            returned.val = context.scope.lookup(((Token.ParameterToken)node.token).getLookupName());
            if (returned.val.type.isFunction())
            {
              List<Value> args = new ArrayList<>();
              for (AstNode argNode: node.internalChildren)
              {
                ReturnedValue argReturn = new ReturnedValue();
                argNode.recursiveVisit(triggers, argReturn, context);
                args.add(argReturn.val);
              }
              returned.val = ((PrimitiveFunction)returned.val.val).call(args);
            }
            return true;
      });

  public static Value eval(AstNode parsed, VariableScope scope) throws RunException
  {
    MachineContext context = new MachineContext();
    context.scope = scope;
    return eval(parsed, context);
  }
  
  public static Value eval(AstNode parsed, MachineContext context) throws RunException
  {
    ReturnedValue toReturn = new ReturnedValue();
    parsed.recursiveVisit(triggers, toReturn, context);
    return toReturn.val;
  }

}
