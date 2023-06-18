package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;

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
    public double[] getTestDimensions() 
    {
      return new double[] { left, top, bottom};
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
    RenderedHitBox matchedHitBox = RenderedTokenHitBox.inStatements(statements, pos, level, hitBox);
    if (matchedHitBox == null) return null;
    return CursorRect.fromHitBoxLeft(matchedHitBox);
  }

}
