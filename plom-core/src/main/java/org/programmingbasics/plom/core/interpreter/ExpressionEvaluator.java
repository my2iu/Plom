package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Rule;

public class ExpressionEvaluator
{
  public static class ReturnedValue
  {
    Value val;
  }
  static AstNode.VisitorTriggers<ReturnedValue, Void, RunException> triggers = new AstNode.VisitorTriggers<ReturnedValue, Void, RunException>()
      .add(Rule.AdditiveExpression_MultiplicativeExpression_AdditiveExpressionMore,
          (VisitorTriggers<ReturnedValue, Void, RunException> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            // Use hard-coded handling of operators initially
            if (node.children.get(1).matchesRule(Rule.AdditiveExpressionMore))
            {
              node.recursiveVisitChildren(triggers, returned, param2);
              return;
            }

            node.children.get(0).recursiveVisit(triggers, returned, param2);
            Value left = returned.val;
            AstNode more = node.children.get(1); 
            if (more.matchesRule(Rule.AdditiveExpressionMore_Plus_AdditiveExpression))
            {
              more.recursiveVisit(triggers, returned, param2);
              Value right = returned.val;
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                returned.val.val = left.getNumberValue() + right.getNumberValue();
              else if (left.type == Type.STRING && right.type == Type.STRING)
                returned.val.val = left.getStringValue() + right.getStringValue();
              else
                throw new RunException();
            }
            else if (more.matchesRule(Rule.AdditiveExpressionMore_Minus_AdditiveExpression))
            {
              more.recursiveVisit(triggers, returned, param2);
              Value right = returned.val;
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                returned.val.val = left.getNumberValue() - right.getNumberValue();
              else
                throw new RunException();
            }
      })
      .add(Rule.MultiplicativeExpression_MemberExpression_MultiplicativeExpressionMore,
          (VisitorTriggers<ReturnedValue, Void, RunException> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            // Use hard-coded handling of operators initially
            if (node.children.get(1).matchesRule(Rule.MultiplicativeExpressionMore))
            {
              node.recursiveVisitChildren(triggers, returned, param2);
              return;
            }

            node.children.get(0).recursiveVisit(triggers, returned, param2);
            Value left = returned.val;
            AstNode more = node.children.get(1); 
            if (more.matchesRule(Rule.MultiplicativeExpressionMore_Multiply_MultiplicativeExpression))
            {
              more.recursiveVisit(triggers, returned, param2);
              Value right = returned.val;
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                returned.val.val = left.getNumberValue() * right.getNumberValue();
              else
                throw new RunException();
            }
            else if (more.matchesRule(Rule.MultiplicativeExpressionMore_Divide_MultiplicativeExpression))
            {
              more.recursiveVisit(triggers, returned, param2);
              Value right = returned.val;
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                returned.val.val = left.getNumberValue() / right.getNumberValue();
              else
                throw new RunException();
            }
      })
      .add(Rule.String, 
          (VisitorTriggers<ReturnedValue, Void, RunException> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            returned.val = new Value();
            returned.val.type = Type.STRING;
            String rawStr = ((Token.SimpleToken)node.token).contents;
            returned.val.val = rawStr.substring(1, rawStr.length() - 1);
      })
      .add(Rule.Number, 
          (VisitorTriggers<ReturnedValue, Void, RunException> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            returned.val = new Value();
            returned.val.type = Type.NUMBER;
            returned.val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
      });

  
  
  public static Value eval(AstNode parsed) throws RunException
  {
    ReturnedValue toReturn = new ReturnedValue();
    parsed.recursiveVisit(triggers, toReturn, null);
    return toReturn.val;
  }
}
