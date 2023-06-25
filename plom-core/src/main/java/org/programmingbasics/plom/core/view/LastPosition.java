package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

/**
 * Sets the code position to the last position possible
 */
public class LastPosition
{
  static void lastPositionOfStatements(StatementContainer code, CodePosition pos, int level)
  {
    if (code.statements.isEmpty())
    {
      pos.setOffset(level, 0);
      pos.setMaxOffset(level + 1);
      return;
    }
    pos.setOffset(level, code.statements.size() - 1);
    TokenContainer line = code.statements.get(pos.getOffset(level));
    lastPositionOfLine(line, pos, level + 1);
  }
  
  static void lastPositionOfLine(TokenContainer line, CodePosition pos, int level)
  {
    pos.setOffset(level, line.tokens.size());
    pos.setMaxOffset(level + 1);
  }
  
  /**
   * Returns true and modifies the code position 
   *   iff
   * position can be placed inside the token 
   */
  static boolean lastPositionInsideToken(Token token, CodePosition pos, int level)
  {
    return token.visit(new TokenVisitor<Boolean>() {
      @Override
      public Boolean visitSimpleToken(SimpleToken token)
      {
        return false;
      }

      @Override
      public Boolean visitParameterToken(ParameterToken token)
      {
        if (token.parameters.isEmpty())
          return false;
        pos.setOffset(level, CodePosition.PARAMTOK_POS_EXPRS);
        pos.setOffset(level + 1, token.parameters.size() - 1);
        lastPositionOfLine(token.parameters.get(token.parameters.size() - 1), pos, level + 2);
        return true;
      }
      
      @Override
      public Boolean visitWideToken(WideToken token)
      {
        return false;
      }

      @Override
      public Boolean visitOneBlockToken(OneBlockToken token)
      {
        pos.setOffset(level, CodePosition.EXPRBLOCK_POS_BLOCK);
        lastPositionOfStatements(token.block, pos, level + 1);
        return true;
      }

      @Override
      public Boolean visitOneExpressionOneBlockToken(
          OneExpressionOneBlockToken token)
      {
        pos.setOffset(level, CodePosition.EXPRBLOCK_POS_BLOCK);
        lastPositionOfStatements(token.block, pos, level + 1);
        return true;
      }});
  }
}
