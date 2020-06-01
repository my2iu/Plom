package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
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
        AstNode node = parser.parse(Symbol.Statement);
        System.out.println(node);
      } 
      catch (ParseException e)
      {
        
      }
    }
  }
}
