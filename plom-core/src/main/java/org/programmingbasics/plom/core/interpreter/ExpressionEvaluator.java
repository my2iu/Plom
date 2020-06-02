package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.gen.Rule;

public class ExpressionEvaluator
{
  public static class ReturnedValue
  {
    Value val;
  }
  static AstNode.VisitorTriggers<ReturnedValue, Void> triggers = new AstNode.VisitorTriggers<ReturnedValue, Void>()
      .add(Rule.AssignmentExpression_Expression_AssignmentExpressionMore,
          (VisitorTriggers<ReturnedValue, Void> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
                

      })
      .add(Rule.Number, 
          (VisitorTriggers<ReturnedValue, Void> triggers, AstNode node, ReturnedValue returned, Void param2) -> {
            returned.val = new Value();
      });

  
  
  public static Value eval(AstNode parsed)
  {
    ReturnedValue toReturn = new ReturnedValue();
    parsed.recursiveVisit(triggers, parsed, toReturn, null);
    return toReturn.val;
  }
}
