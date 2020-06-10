package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

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
  static class EvalHandlerChainer implements RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException> 
  {
    List<RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException>> chain = new ArrayList<>();
    @Override public boolean visit(
        VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers,
        AstNode node, ReturnedValue param1, ExpressionEvaluator param2)
        throws RunException
    {
      for (RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException> handler: chain)
      {
        if (handler.visit(triggers, node, param1, param2))
          return true;
      }
      return false;
    }
    EvalHandlerChainer chainTo(RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException> next)
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
  // so we have a function for making handlers for the case where there is no more
  static RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException> createBinaryOperatorTestEmptyMore(
      List<Symbol> rightEmptyRule)
  {
    return (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
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
  static RecursiveWalkerVisitor<ReturnedValue, ExpressionEvaluator, RunException> createBinaryOperatorTestMore(
      List<Symbol> matchRule, BinaryOperatorHandler doOp)
  {
    return (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
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
  
  
  public static class ReturnedValue
  {
    Value val;
  }
  static AstNode.VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers = new AstNode.VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException>()
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
          (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
            returned.val = new Value();
            returned.val.type = Type.STRING;
            String rawStr = ((Token.SimpleToken)node.token).contents;
            returned.val.val = rawStr.substring(1, rawStr.length() - 1);
            return true;
      })
      .add(Rule.Number, 
          (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
            returned.val = new Value();
            returned.val.type = Type.NUMBER;
            returned.val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
            return true;
      })
      .add(Rule.TrueLiteral, 
          (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
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
          (VisitorTriggers<ReturnedValue, ExpressionEvaluator, RunException> triggers, AstNode node, ReturnedValue returned, ExpressionEvaluator context) -> {
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
    ExpressionEvaluator context = new ExpressionEvaluator();
    context.scope = scope;
    ReturnedValue toReturn = new ReturnedValue();
    parsed.recursiveVisit(triggers, toReturn, context);
    return toReturn.val;
  }
}
