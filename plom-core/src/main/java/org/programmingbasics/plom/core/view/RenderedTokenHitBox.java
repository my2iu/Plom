package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Returns a hitbox surrounding a token in the code (it might also
 * generate a fake hitbox if the given position refers to a location
 * *after* a token). For control flow tokens, it will only return a 
 * hitbox for the first line of the token (not for the entire 
 * token) because that's what's useful for cursors.
 * 
 */
public class RenderedTokenHitBox
{
  public static RenderedHitBox inStatements(StatementContainer statements, CodePosition pos, int level, RenderedHitBox hitBox)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      RenderedHitBox childHitBox = hitBox.children.get(pos.getOffset(level));
      return inLine(line, Symbol.FullStatement, pos, level + 1, childHitBox);
    }
    else if (pos.getOffset(level) == 0 && hitBox.children != null && !hitBox.children.isEmpty())
    {
      RenderedHitBox lineHitBox = hitBox.children.get(0);
      return lineHitBox;
    }
    else
      return null;
  }
  
  public static RenderedHitBox inLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level, RenderedHitBox hitBox)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, hitBox.children.get(pos.getOffset(level)));
    }
    if (pos.getOffset(level) < line.tokens.size())
    {
      // Returns the hitbox of the token to the right
      RenderedHitBox tokenHitBox = hitBox.children.get(pos.getOffset(level));
      // Special handling for wide/block tokens
      if (line.tokens.get(pos.getOffset(level)).isWide())
      {
        if (hitBox.children.get(pos.getOffset(level)).children.get(CodeRenderer.EXPRBLOCK_POS_START) != null)
          tokenHitBox = hitBox.children.get(pos.getOffset(level)).children.get(CodeRenderer.EXPRBLOCK_POS_START);
      }
      return tokenHitBox;
    }
    else if (pos.getOffset(level) > 0)
    {
      // No token at the cursor position, but there is a token to the left,
      // so generate a zero-width hitbox based on that token
      if (!line.tokens.get(pos.getOffset(level) - 1).isWide())
      {
        RenderedHitBox tokenHitBox = hitBox.children.get(pos.getOffset(level) - 1);
        return new RenderedHitBox.RectangleRenderedHitBox(tokenHitBox.getOffsetLeft() + tokenHitBox.getOffsetWidth(), tokenHitBox.getOffsetTop(), 0, tokenHitBox.getOffsetHeight());
      }
      else
      {
        // If the last token is a wide token, we have an extra hit box for the empty line at the end
        RenderedHitBox tokenHitBox = hitBox.children.get(pos.getOffset(level));
        return tokenHitBox;
      }
    }
    else
    {
      // Token container is empty, so determine hit box based on the token container's hit box
      return hitBox;
    }
  }
  
  public static RenderedHitBox inTypeField(Token type, RenderedHitBox hitBox,
      CodePosition pos)
  {
    if (pos.hasOffset(1))
    {
      return type.visit(new TokenAtCursor(), pos, 1, hitBox);
    }
    return hitBox;
  }

  
  static class TokenAtCursor extends RecurseIntoCompoundToken<RenderedHitBox, RenderedHitBox, RuntimeException>
  {
    @Override
    public RenderedHitBox visitParameterToken(ParameterToken token, CodePosition pos, Integer level, 
        RenderedHitBox hitBox)
    {
      if (pos.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
      {
        return inLine(token.parameters.get(pos.getOffset(level + 1)), Symbol.ExpressionOnly, pos, level + 2, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR).children.get(pos.getOffset(level + 1)));
      }
      throw new IllegalArgumentException();
    }
    
    @Override
    RenderedHitBox handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, RenderedHitBox hitBox)
    {
      if (originalToken.getType() == Symbol.COMPOUND_FOR)
        return inLine(exprContainer, Symbol.ForExpressionOnly, pos, level, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR));
      else if (originalToken.getType() == Symbol.FunctionLiteral)
        return inLine(exprContainer, Symbol.FunctionLiteralExpressionOnly, pos, level, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR));
      else
        return inLine(exprContainer, Symbol.ExpressionOnly, pos, level, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR));
    }
    @Override
    RenderedHitBox handleStatementContainer(TokenWithSymbol originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, RenderedHitBox hitBox)
    {
      return inStatements(blockContainer, pos, level, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK));
    }
  }
}
