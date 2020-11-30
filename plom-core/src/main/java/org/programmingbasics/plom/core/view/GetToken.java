package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/** Gets the token at the given cursor position */ 
public class GetToken
{
  
  public static Token inStatements(StatementContainer statements, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return inLine(line, Symbol.FullStatement, pos, level + 1);
    }
    return null;
  }
  
  public static Token inLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, null);
    }
    if (pos.getOffset(level) < line.tokens.size())
      return line.tokens.get(pos.getOffset(level));
    return null;
  }
  
  static class TokenAtCursor extends RecurseIntoCompoundToken<Token, Void>
  {
    @Override
    Token handleExpression(Token originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, Void param)
    {
      return inLine(exprContainer, Symbol.ExpressionOnly, pos, level);
    }
    @Override
    Token handleStatementContainer(Token originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, Void param)
    {
      return inStatements(blockContainer, pos, level);
    }
  }
}
