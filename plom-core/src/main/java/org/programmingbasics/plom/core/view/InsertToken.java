package org.programmingbasics.plom.core.view;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class InsertToken
{
  public static void insertTokenIntoStatementContainer(StatementContainer stmtContainer, Token newToken, CodePosition pos, int level, boolean advanceCursorPos)
  {
    if (stmtContainer.statements.isEmpty()) 
    {
      stmtContainer.statements.add(new TokenContainer(Collections.emptyList()));
    }
    TokenContainer line = stmtContainer.statements.get(pos.getOffset(level));
    if (!pos.hasOffset(level + 2) && newToken.isInline() 
        && line.tokens.size() > pos.getOffset(level + 1)
        && !line.tokens.get(pos.getOffset(level + 1)).isInline())
    {
      // Normal tokens cannot have a wide token following them on a line.
      // (It is ok if normal tokens have wide tokens in front of them though.)
      // So if we're inserting a normal token into a line with a wide token,
      // we'll also insert a new line to hold the normal tokens
      // (I've tried changing the grammar to allow wide tokens after
      // normal tokens, but it's too flexible in that it keeps asking
      // you if you want to add if statements in the middle of expressions)
      TokenContainer newline = new TokenContainer(line.tokens.subList(pos.getOffset(level + 1), line.tokens.size()));
      for (int n = line.tokens.size() - 1; n >= pos.getOffset(level + 1); n--)
         line.tokens.remove(n);
      insertTokenIntoLine(line, newToken, pos, level + 1, advanceCursorPos);
      stmtContainer.statements.add(pos.getOffset(level) + 1, newline);
    }
    else if (!pos.hasOffset(level + 2) && !newToken.isInline()
        && pos.getOffset(level + 1) - 1 < line.tokens.size()
        && pos.getOffset(level + 1) - 1 >= 0
        && line.tokens.get(pos.getOffset(level + 1) - 1).isInline())
    {
      // Make sure we aren't inserting a wide token after a non-wide token
      TokenContainer newline = new TokenContainer(line.tokens.subList(pos.getOffset(level + 1), line.tokens.size()));
      for (int n = line.tokens.size() - 1; n >= pos.getOffset(level + 1); n--)
        line.tokens.remove(n);
      stmtContainer.statements.add(pos.getOffset(level) + 1, newline);
      pos.setOffset(level, pos.getOffset(level) + 1);
      pos.setOffset(level + 1, 0);
      insertTokenIntoLine(newline, newToken, pos, level + 1, advanceCursorPos);
    }
    else
      insertTokenIntoLine(line, newToken, pos, level + 1, advanceCursorPos);
  }
  
  static void insertTokenIntoLine(TokenContainer line, Token newToken, CodePosition pos, int level, boolean advanceCursorPos)
  {
    if (pos.hasOffset(level + 1))
    {
      Token token = line.tokens.get(pos.getOffset(level));
      token.visit(new RecurseIntoCompoundToken<Void, Token, RuntimeException>() {
        @Override
        Void handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
            CodePosition pos, int level, Token newToken)
        {
          insertTokenIntoLine(exprContainer, newToken, pos, level, advanceCursorPos);
          return null;
        }
        @Override
        Void handleStatementContainer(TokenWithSymbol originalToken,
            StatementContainer blockContainer, CodePosition pos, int level, Token newToken)
        {
          insertTokenIntoStatementContainer(blockContainer, newToken, pos, level, advanceCursorPos);
          return null;
        }
      }, pos, level + 1, newToken);
      return;
    }
    line.tokens.add(pos.getOffset(level), newToken);
    if (advanceCursorPos)
    {
      pos.setOffset(level, pos.getOffset(level) + 1);
      pos.setMaxOffset(level + 1);
    }
  }

}
