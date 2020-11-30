package org.programmingbasics.plom.core.view;

import java.util.ArrayList;

import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

import elemental.html.DivElement;

public class HitDetect
{
  public static CodePosition renderAndHitDetect(int x, int y, DivElement codeDiv, StatementContainer codeList, CodePosition oldPos, ErrorList codeErrors)
  {
    RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
    renderedHitBoxes.children = new ArrayList<>();
    codeDiv.setInnerHTML("");
    CodeRenderer.render(codeDiv, codeList, oldPos, null, null, renderedHitBoxes, codeErrors);
    CodePosition pos = new CodePosition();
    return hitDetectStatementContainer(x, y, codeList, renderedHitBoxes, pos, 0);
  }

  static CodePosition hitDetectStatementContainer(int x, int y,
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
      RenderedHitBox child = renderedHitBoxes.children.get(n);
      if (child.getOffsetTop() < y && child.getOffsetTop() > bestMatchY)
      {
        bestMatchY = child.getOffsetTop();
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
    if (renderedLineHitBoxes.children == null)
    {
      newPos.setOffset(level, 0);
      return newPos;
    }
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
  
  private static TokenHitLocation checkHitMultilineSpan(int x, int y,
      RenderedHitBox hitBox)
  {
    RenderedHitBox.Rect topRect = null, bottomRect = null;
    for (RenderedHitBox.Rect rect: hitBox.getClientRects())
    {
      if (topRect == null || rect.getTop() < topRect.getTop()) topRect = rect;
      if (bottomRect == null || rect.getBottom() > bottomRect.getBottom()) bottomRect = rect;
    }
//    if (y > topRect.getBottom() && y < bottomRect.getTop()) return TokenHitLocation.ON;
    if (y > bottomRect.getBottom() || (y > bottomRect.getTop() && x > bottomRect.getRight()))
      return TokenHitLocation.AFTER;
    if (y < topRect.getBottom() && x < topRect.getLeft()) return TokenHitLocation.NONE;
    return TokenHitLocation.ON;
  }

  static class TokenHitDetection implements Token.TokenVisitor3<TokenHitLocation, Integer, Integer, RenderedHitBox>
  {
    @Override
    public TokenHitLocation visitSimpleToken(SimpleToken token, Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      if (hitBox.getOffsetLeft() + hitBox.getOffsetWidth() < x) return TokenHitLocation.AFTER;
      if (hitBox.getOffsetLeft() < x) return TokenHitLocation.ON;
      return TokenHitLocation.NONE;
    }
    @Override
    public TokenHitLocation visitParameterToken(ParameterToken token,
        Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      // Early escape for if it definitely isn't on the token 
      if (hitBox.getOffsetTop() + hitBox.getOffsetHeight() < y) return TokenHitLocation.AFTER;
      if (y < hitBox.getOffsetTop()) return TokenHitLocation.NONE;
      
      // Parameter tokens can span multiple lines, so we need to check each bounding rectangle
      return checkHitMultilineSpan(x, y, hitBox);
    }
    TokenHitLocation hitDetectWideToken(int x, int y, RenderedHitBox hitBox)
    {
      if (hitBox.getOffsetTop() + hitBox.getOffsetHeight() < y) return TokenHitLocation.AFTER;
      if (hitBox.getOffsetTop() < y) return TokenHitLocation.ON;
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
    public Void visitParameterToken(ParameterToken token, CodePosition pos,
        Integer level, HitDetectParam param)
    {
      RenderedHitBox hitBox = param.hitBox;
      int x = param.x;
      int y = param.y;
      // Check each parameter to see if we're clicking in there
      RenderedHitBox paramHitBox = hitBox.children.get(CodeRenderer.PARAMTOK_POS_EXPRS); 
      if (paramHitBox == null)
        return null;
      for (int n = paramHitBox.children.size() - 1; n >= 0; n--)
      {
        if (checkHitMultilineSpan(x, y, paramHitBox.children.get(n)) != TokenHitLocation.NONE)
        {
          pos.setOffset(level, CodeRenderer.PARAMTOK_POS_EXPRS);
          pos.setOffset(level + 1, n);
          hitDetectTokens(x, y, token.parameters.get(n), paramHitBox.children.get(n), pos, level + 2);
          return null;
        }
      }
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
        && y > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK).getOffsetTop())
      {
        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
        hitDetectStatementContainer(x, y, blockContainer, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_BLOCK), pos, level + 1);
      }
      // Check if we're inside the expression
      else if (hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR) != null
          && (x > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR).getOffsetLeft() 
              || y > hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_START).getOffsetTop() + hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_START).getOffsetHeight()))
      {
        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_EXPR);
        hitDetectTokens(x, y, exprContainer, hitBox.children.get(CodeRenderer.EXPRBLOCK_POS_EXPR), pos, level + 1);
      }
      return null;
    }
  }

  /**
   * Type fields are just a single token, so we have special type
   * detection for them.
   */
  public static CodePosition hitDetectTypeField(int x, int y, 
      Token type, RenderedHitBox hitBox,
      CodePosition newPos, int level)
  {
    // Find which token that mouse position is over
    TokenHitDetection isClickedOnTokenOrLater = new TokenHitDetection();
    TokenHitLocation loc = type.visit(isClickedOnTokenOrLater, x, y, hitBox);
    // Update the cursor position
    newPos.setOffset(level, 0);
    
    // For compound tokens, check if we're clicking on some element inside the token
    if (loc == TokenHitLocation.ON)
    {
      type.visit(new TypeInternalHitDetection(), newPos, level + 1, new HitDetectParam(hitBox, x, y));
    }
    
    return newPos;
  }

  /** Although a parameter token can hold an entire expression inside,
   * for types, we only expect a single token at most, so we can
   * simplify the logic.
   */
  private static CodePosition hitDetectTypeTokens(int x, int y, 
      TokenContainer tokens, RenderedHitBox renderedLineHitBoxes,
      CodePosition newPos, int level)
  {
    newPos.setOffset(level, 0);
    // Find which token that mouse position is over
    int tokenno = 0;
//    TokenHitDetection isClickedOnTokenOrLater = new TokenHitDetection();
    if (renderedLineHitBoxes.children == null)
    {
      return newPos;
    }
    tokenno = 0;
//    for (int n = 0; n < renderedLineHitBoxes.children.size(); n++)
//    {
//      Token token = tokens.tokens.get(n);
//      TokenHitLocation loc = token.visit(isClickedOnTokenOrLater, x, y, renderedLineHitBoxes.children.get(n));
//      if (loc == TokenHitLocation.AFTER)
//        tokenno = n;
//      if (loc == TokenHitLocation.ON)
//      {
//        tokenno = n;
//        break;
//      }
//    }
    // Update the cursor position
    newPos.setOffset(level, tokenno);
    
    // For compound tokens, check if we're clicking on some element inside the token
    if (tokenno < tokens.tokens.size())
    {
      tokens.tokens.get(tokenno).visit(new TypeInternalHitDetection(), newPos, level + 1, new HitDetectParam(renderedLineHitBoxes.children.get(tokenno), x, y));
    }
    
    return newPos;
  }

  static class TypeInternalHitDetection extends RecurseIntoCompoundToken<Void, HitDetectParam>
  {
    @Override
    public Void visitSimpleToken(SimpleToken token, CodePosition pos,
        Integer level, HitDetectParam param)
    {
      return null;
    }
    @Override
    public Void visitParameterToken(ParameterToken token, CodePosition pos,
        Integer level, HitDetectParam param)
    {
      RenderedHitBox hitBox = param.hitBox;
      int x = param.x;
      int y = param.y;
      // Check each parameter to see if we're clicking in there
      RenderedHitBox paramHitBox = hitBox.children.get(CodeRenderer.PARAMTOK_POS_EXPRS); 
      if (paramHitBox == null)
        return null;
      for (int n = paramHitBox.children.size() - 1; n >= 0; n--)
      {
        if (checkHitMultilineSpan(x, y, paramHitBox.children.get(n)) != TokenHitLocation.NONE)
        {
          pos.setOffset(level, CodeRenderer.PARAMTOK_POS_EXPRS);
          pos.setOffset(level + 1, n);
          hitDetectTypeTokens(x, y, token.parameters.get(n), paramHitBox.children.get(n), pos, level + 2);
          return null;
        }
      }
      return null;
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition pos, int level,
        HitDetectParam param)
    {
      throw new IllegalArgumentException();
    }
  }
  
  
}
