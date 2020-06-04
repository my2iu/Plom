package org.programmingbasics.plom.core.interpreter;

import java.util.Arrays;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;

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

  public void run()
  {
    for (TokenContainer line: code.statements)
    {
      ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
      try {
        AstNode parsed = parser.parse(Symbol.Statement);
        parsed.recursiveVisit(new AstNode.VisitorTriggers<Void, Void, RunException>()
            .add(Rule.AssignmentExpression_Expression_AssignmentExpressionMore, 
                (VisitorTriggers<Void, Void, RunException> triggers, AstNode node, Void param1, Void param2) -> {
                  
                }), 
            null, null);
        
        System.out.println(parsed);
      } 
      catch (ParseException e)
      {
        
      }
      catch (RunException e)
      {
        
      }
    }
  }
}
