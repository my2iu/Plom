package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.ParseContext.ParseContextForCursor;
import org.programmingbasics.plom.core.view.ParseContext.TokenPredictiveParseContext;

public class EraseLeft
{
  public static void eraseLeftFromStatementContainer(
      StatementContainer code, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < code.statements.size())
    {
      TokenContainer line = code.statements.get(pos.getOffset(level));
      eraseLeftFromLine(line, pos, level + 1);
    }
    return;

  }
  
  public static void eraseLeftFromLine(
      TokenContainer line, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
//      return line.tokens.get(pos.getOffset(level)).visit(new TokenPredictiveParseContext(), pos, level + 1, null);
    }
//    ParseContextForCursor toReturn = new ParseContextForCursor();
//    toReturn.baseContext = baseContext;
//    toReturn.tokens.addAll(line.tokens.subList(0, pos.getOffset(level)));
//    return toReturn;
    
    return;

  }
}
