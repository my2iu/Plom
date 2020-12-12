package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;

/**
 * Type entry fields in the UI allow people to type in the name of
 * a type using tokens, and we need special handling for setting/replacing
 * parameters of a type.
 */
public class SetTypeToken
{
  /**
   * Returns the token representing the type (usually the same token
   * as before, but it may be newToken if it replaces the current
   * type entirely).
   */
  public static Token set(Token type, Token newType, CodePosition pos)
  {
    if (!pos.hasOffset(1))
      return newType;
    type.visit(new SetTokenAtCursor(), pos, 1, newType);
    return type;
  }
  
  private static Token inLine(TokenContainer line, Token newToken, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      line.tokens.get(pos.getOffset(level)).visit(new SetTokenAtCursor(), pos, level + 1, newToken);
      return null;
    }
    if (pos.getOffset(level) < line.tokens.size())
      line.tokens.set(pos.getOffset(level), newToken);
    else
      line.tokens.add(newToken);
    return null;
  }
  
  static class SetTokenAtCursor extends RecurseIntoCompoundToken<Token, Token, RuntimeException>
  {
    @Override
    Token handleExpression(Token originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, Token newToken)
    {
      return inLine(exprContainer, newToken, pos, level);
    }
    @Override
    Token handleStatementContainer(Token originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, Token newToken)
    {
      throw new IllegalArgumentException();
    }
  }
}
