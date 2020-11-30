package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Figures out where on-screen the cursor should appear
 */
public class RenderedCursorPosition
{
  public static class CursorRect
  {
    public double left;
    public double top;
    public double bottom;
    public CursorRect(double left, double top, double bottom) 
    {
      this.left = left;
      this.top = top;
      this.bottom = bottom;
    }
    public static CursorRect fromHitBoxLeft(RenderedHitBox hitBox)
    {
      return new CursorRect(hitBox.getOffsetLeft(), hitBox.getOffsetTop(), hitBox.getOffsetTop() + hitBox.getOffsetHeight());
    }
    public static CursorRect fromHitBoxRight(RenderedHitBox tokenHitBox)
    {
      return new CursorRect(tokenHitBox.getOffsetLeft() + tokenHitBox.getOffsetWidth(), tokenHitBox.getOffsetTop(), tokenHitBox.getOffsetTop() + tokenHitBox.getOffsetHeight());
    }
  }

//  public static CursorRect renderAndFindCursor(CodePosition toFind, DivElement codeDiv, StatementContainer codeList, CodePosition cursorPos, ErrorList codeErrors)
//  {
//    RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
//    renderedHitBoxes.children = new ArrayList<>();
//    codeDiv.setInnerHTML("");
//    CodeRenderer.render(codeDiv, codeList, cursorPos, null, null, renderedHitBoxes, codeErrors);
//    return RenderedCursorPosition.inStatements(codeList, toFind, 0, renderedHitBoxes);
//  }

  
  public static CursorRect inStatements(StatementContainer statements, CodePosition pos, int level, RenderedHitBox hitBox)
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
      return CursorRect.fromHitBoxLeft(lineHitBox);
    }
    else
      return null;
  }
  
  public static CursorRect inLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level, RenderedHitBox hitBox)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, hitBox);
    }
    if (pos.getOffset(level) < line.tokens.size())
    {
      // Determine cursor size based on the token to the right
      RenderedHitBox tokenHitBox = hitBox.children.get(pos.getOffset(level));
      return CursorRect.fromHitBoxLeft(tokenHitBox);
    }
    else if (pos.getOffset(level) > 0)
    {
      // Determine cursor size based on the token to the left
      RenderedHitBox tokenHitBox = hitBox.children.get(pos.getOffset(level) - 1);
      return CursorRect.fromHitBoxRight(tokenHitBox);
    }
    else
    {
      // Token container is empty, so determine hit box based on the token container's hit box
      return CursorRect.fromHitBoxLeft(hitBox);
    }
  }
  
  static class TokenAtCursor extends RecurseIntoCompoundToken<CursorRect, RenderedHitBox>
  {
    @Override
    CursorRect handleExpression(Token originalToken, TokenContainer exprContainer,
        CodePosition pos, int level, RenderedHitBox hitBox)
    {
      return inLine(exprContainer, Symbol.ExpressionOnly, pos, level, hitBox);
    }
    @Override
    CursorRect handleStatementContainer(Token originalToken,
        StatementContainer blockContainer, CodePosition pos, int level, RenderedHitBox hitBox)
    {
      return inStatements(blockContainer, pos, level, hitBox);
    }
  }
}
