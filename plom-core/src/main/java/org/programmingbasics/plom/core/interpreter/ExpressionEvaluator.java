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
  static AstNode.VisitorTriggers<ReturnedValue, Void> triggers = new AstNode.VisitorTriggers<ReturnedValue, Void>()
      .add(Rule.AdditiveExpression_MultiplicativeExpression_AdditiveExpressionMore,
          (VisitorTriggers<ReturnedValue, Void> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
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
              else
                throw new IllegalArgumentException();
            }
            else if (more.matchesRule(Rule.AdditiveExpressionMore_Minus_AdditiveExpression))
            {
              more.recursiveVisit(triggers, returned, param2);
              Value right = returned.val;
              if (left.type == Type.NUMBER && right.type == Type.NUMBER)
                returned.val.val = left.getNumberValue() - right.getNumberValue();
              else
                throw new IllegalArgumentException();
            }
      })
      .add(Rule.Number, 
          (VisitorTriggers<ReturnedValue, Void> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            returned.val = new Value();
            returned.val.type = Type.NUMBER;
            returned.val.val = Double.parseDouble(((Token.SimpleToken)node.token).contents);
      });

  
  
  public static Value eval(AstNode parsed)
  {
    ReturnedValue toReturn = new ReturnedValue();
    parsed.recursiveVisit(triggers, toReturn, null);
    return toReturn.val;
  }
}
