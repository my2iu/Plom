package org.programmingbasics.plom.core.view;

import java.util.ArrayList;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;

import elemental.dom.Element;
import elemental.html.DivElement;

public class HitDetect
{
  public static CodePosition renderAndHitDetect(int x, int y, DivElement codeDiv, StatementContainer codeList, CodePosition oldPos)
  {
    RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
    renderedHitBoxes.children = new ArrayList<>();
    codeDiv.setInnerHTML("");
    CodeRenderer.render(codeDiv, codeList, oldPos, renderedHitBoxes);
    CodePosition pos = new CodePosition();
    return hitDetectStatementContainer(x, y, codeList, renderedHitBoxes, pos, 0);
  }

  private static CodePosition hitDetectStatementContainer(int x, int y,
      StatementContainer statements,
      RenderedHitBox renderedHitBoxes,
      CodePosition newPos, 
      int level)
  {
    // Find which line matches the mouse position
    int bestMatchY = -1;
    int lineno = -1;
    for (int n = 0; n < renderedHitBoxes.children.size(); n++)
    {
      Element el = renderedHitBoxes.children.get(n).el;
      if (el.getOffsetTop() < y && el.getOffsetTop() > bestMatchY)
      {
        bestMatchY = el.getOffsetTop();
        lineno = n;
      }
    }
    if (lineno >= 0)
    {
      newPos.setOffset(level, lineno);
      return hitDetectTokens(x, y, statements.statements.get(lineno), renderedHitBoxes.children.get(lineno), newPos, level + 1);
    }
    return null;
  }

  private static CodePosition hitDetectTokens(int x, int y, 
      TokenContainer tokens, RenderedHitBox renderedLineHitBoxes,
      CodePosition newPos, int level)
  {
    // Find which token that mouse position is over
    int tokenno = 0;
    TokenHitDetection isClickedOnTokenOrLater = new TokenHitDetection();
    for (int n = 0; n < renderedLineHitBoxes.children.size(); n++)
    {
      Token token = tokens.tokens.get(n);
      TokenHitLocation loc = token.visit(isClickedOnTokenOrLater, x, y, renderedLineHitBoxes.children.get(n));
      if (loc == TokenHitLocation.AFTER)
        tokenno = n + 1;
      if (loc == TokenHitLocation.ON)
      {
        tokenno = n;
        break;
      }
    }
//    // Check if mouse is past the end of the last token
//    if (tokenno == renderedLineHitBoxes.children.size() - 1)
//    {
//      Element el = renderedLineHitBoxes.children.get(tokenno).el;
//      if (el.getOffsetLeft() + el.getOffsetWidth() < x)
//      {
//        tokenno++;
//      }
//    }
    // Update the cursor position
    newPos.setOffset(level, tokenno);
    
    // For compound tokens, check if we're clicking on some element inside the token
    if (tokenno < tokens.tokens.size())
    {
      tokens.tokens.get(tokenno).visit(new TokenInternalHitDetection(), newPos, level + 1, new HitDetectParam(renderedLineHitBoxes.children.get(tokenno), x, y));
    }
    
    return newPos;
  }

  static enum TokenHitLocation
  {
    NONE, ON, AFTER;
  }
  static class TokenHitDetection implements Token.TokenVisitor3<TokenHitLocation, Integer, Integer, RenderedHitBox>
  {
    @Override
    public TokenHitLocation visitSimpleToken(SimpleToken token, Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      Element el = hitBox.el;
      if (el.getOffsetLeft() + el.getOffsetWidth() < x) return TokenHitLocation.AFTER;
      if (el.getOffsetLeft() < x) return TokenHitLocation.ON;
      return TokenHitLocation.NONE;
    }
    TokenHitLocation hitDetectWideToken(int x, int y, RenderedHitBox hitBox)
    {
      Element el = hitBox.el;
      if (el.getOffsetTop() + el.getOffsetHeight() < y) return TokenHitLocation.AFTER;
      if (el.getOffsetTop() < y) return TokenHitLocation.ON;
      return TokenHitLocation.NONE;
    }
    @Override
    public TokenHitLocation visitWideToken(WideToken token, Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      return hitDetectWideToken(x, y, hitBox);
    }
    @Override
    public TokenHitLocation visitOneBlockToken(OneBlockToken token,
        Integer x, Integer y, RenderedHitBox hitBox)
    {
      return hitDetectWideToken(x, y, hitBox);
    }
    @Override
    public TokenHitLocation visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      return hitDetectWideToken(x, y, hitBox);
    }
    
  }
  
  static class HitDetectParam
  {
    public HitDetectParam(RenderedHitBox hitBox, int x, int y)
    {
      this.hitBox = hitBox;
      this.x = x;
      this.y = y;
    }
    RenderedHitBox hitBox;
    int x, y;
  }
  
  // Does hit detection for compound tokens (tokens that contain expressions
  // and statements inside them)
  static class TokenInternalHitDetection extends RecurseIntoCompoundToken<Void, HitDetectParam>
  {
    @Override
    public Void visitSimpleToken(SimpleToken token, CodePosition pos,
        Integer level, HitDetectParam param)
    {
      return null;
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition pos, int level,
        HitDetectParam param)
    {
      RenderedHitBox hitBox = param.hitBox;
      int x = param.x;
      int y = param.y;
      // Check inside the statement block
      if (hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK) != null
        && y > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK).el.getOffsetTop())
      {
        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
        hitDetectStatementContainer(x, y, blockContainer, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK), pos, level + 1);
      }
      // Check if we're inside the expression
      else if (hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR) != null
          && (x > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR).el.getOffsetLeft() 
              || y > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_START).el.getOffsetTop() + hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_START).el.getOffsetHeight()))
      {
        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_EXPR);
        hitDetectTokens(x, y, exprContainer, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR), pos, level + 1);
      }
      return null;
    }
  }

}
