package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class LineForPosition
{
  public static int inCode(StatementContainer statements, CodePosition pos, LineNumberTracker lineTracker)
  {
    int line = inStatements(statements, pos, 0, lineTracker);
    if (line < 0) line = 1;
    return line;
  }
  
  public static int inStatements(StatementContainer statements, CodePosition pos, int level, LineNumberTracker lineTracker)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return inLine(line, Symbol.FullStatement, pos, level + 1, lineTracker);
    }
    return -1;
  }
  
  static int inLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level, LineNumberTracker lineTracker)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, lineTracker);
    }
    if (pos.getOffset(level) < line.tokens.size())
      return lineTracker.tokenLine.get(line.tokens.get(pos.getOffset(level)));
    else if (pos.getOffset(level) > 0)
      return lineTracker.endContainerLine.get(line);
    else
      return lineTracker.containerLine.get(line);
  }
  
  static class TokenAtCursor extends RecurseIntoCompoundToken<Integer, LineNumberTracker>
  {
    @Override
    Integer handleExpression(Token originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, LineNumberTracker lineTracker)
    {
      return inLine(exprContainer, Symbol.ExpressionOnly, pos, level, lineTracker);
    }
    @Override
    Integer handleStatementContainer(Token originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, LineNumberTracker lineTracker)
    {
      int line = inStatements(blockContainer, pos, level, lineTracker);
      if (line < 0)
        return lineTracker.tokenLine.get(originalToken) + 1;
      return line;
    }
  }
}
