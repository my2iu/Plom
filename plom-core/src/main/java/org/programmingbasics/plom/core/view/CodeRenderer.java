package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.programmingbasics.plom.core.UIResources;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor2;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.css.CSSStyleDeclaration.Unit;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;

public class CodeRenderer
{
  private static final int EXPRBLOCK_POS_START = 0;
  private static final int EXPRBLOCK_POS_EXPR = 1;
  private static final int EXPRBLOCK_POS_BLOCK = 2;
//  private static final int EXPRBLOCK_POS_END = 3;
  
  public void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
  {
    renderStatementContainer(codeDiv, codeList, pos, 0, renderedHitBoxes);
  }

  /** Holds data to be returned about how a token is rendered */
  static class TokenRendererReturn
  {
    /** Element which holds the token */
    public Element el;
    /** Before cursor points--when inserting a cursor before this token, cursor should be inserted before this element */
    public Element beforeInsertionPoint;
  }
  
  static class TokenRenderer implements Token.TokenVisitor4<Void, TokenRendererReturn, CodePosition, Integer, RenderedHitBox>
  {
    Document doc;
    TokenRenderer(Document doc)
    {
      this.doc = doc;
    }
    @Override
    public Void visitSimpleToken(SimpleToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      DivElement div = doc.createDivElement();
      div.setClassName("token");
      div.setTextContent(token.contents);
      if (hitBox != null)
        hitBox.el = div;
      toReturn.el = div;
      toReturn.beforeInsertionPoint = div;
      return null;
    }
    @Override
    public Void visitWideToken(WideToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      createWideToken(token.contents, null, null, toReturn, pos, level, hitBox);
//      DivElement div = doc.createDivElement();
//      div.setClassName("blocktoken");
//      SpanElement contentsSpan = doc.createSpanElement();
//      contentsSpan.setTextContent(token.contents);
//      div.appendChild(contentsSpan);
//      if (hitBox != null)
//        hitBox.el = div;
//      toReturn.el = div;
//      toReturn.beforeInsertionPoint = contentsSpan;
      return null;
    }
    @Override
    public Void visitOneBlockToken(OneBlockToken token,
        TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      createWideToken(token.contents, null, token.block, toReturn, pos, level, hitBox);
      return null;
    }
    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      createWideToken(token.contents, token.expression, token.block, toReturn, pos, level, hitBox);
      return null;
    }
    private void createWideToken(String tokenText, TokenContainer exprContainer,
        StatementContainer blockContainer,
        TokenRendererReturn toReturn, CodePosition pos, int level,
        RenderedHitBox hitBox)
    {
      DivElement div = doc.createDivElement();
      div.setClassName("blocktoken");

      RenderedHitBox startTokenHitBox = null;
      if (hitBox != null)
      {
        hitBox.children = new ArrayList<>();
        hitBox.children.add(null);
        hitBox.children.add(null);
        hitBox.children.add(null);
        startTokenHitBox = new RenderedHitBox();
        hitBox.children.set(EXPRBLOCK_POS_START, startTokenHitBox);
      }

      // First line with optional expression
      DivElement startLine = doc.createDivElement();
      SpanElement start = doc.createSpanElement();
      startLine.appendChild(start);
      if (hitBox != null)
        startTokenHitBox.el = start;
      if (exprContainer != null)
      {
        start.setTextContent(tokenText + " (");
        SpanElement expression = doc.createSpanElement();
        RenderedHitBox exprHitBox = (hitBox != null) ? RenderedHitBox.withChildren() : null;
        renderLine(exprContainer, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_EXPR ? pos : null, level + 1, expression, this, exprHitBox);
        SpanElement middle = doc.createSpanElement();
        if (blockContainer == null)
          middle.setTextContent(")");
        else
          middle.setTextContent(") {");
        startLine.appendChild(expression);
        startLine.appendChild(middle);
        if (hitBox != null)
        {
          exprHitBox.el = expression;
          hitBox.children.set(EXPRBLOCK_POS_EXPR, exprHitBox);
        }
      }
      else
      {
        if (blockContainer != null)
          start.setTextContent(tokenText + " {");
        else
          start.setTextContent(tokenText);
      }
      div.appendChild(startLine);

      // Block part
      if (blockContainer != null)
      {
        DivElement block = doc.createDivElement();
        block.getStyle().setPaddingLeft(1, Unit.EM);
        RenderedHitBox blockHitBox = (hitBox != null) ? RenderedHitBox.withChildren() : null;
        renderStatementContainer(block, blockContainer, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK ? pos : null, level + 1, blockHitBox);
        if (hitBox != null)
        {
          blockHitBox.el = block;
          hitBox.children.set(EXPRBLOCK_POS_BLOCK, blockHitBox);
        }
        div.appendChild(block);
        
        // With ending "}"
        DivElement endLine = doc.createDivElement();
        SpanElement end = doc.createSpanElement();
        end.setTextContent("}");
        endLine.appendChild(end);
        div.appendChild(endLine);
      }

      if (hitBox != null)
        hitBox.el = div;
      toReturn.el = div;
      toReturn.beforeInsertionPoint = start;
    }
  }

  static void renderStatementContainer(DivElement codeDiv, StatementContainer codeList, CodePosition pos, int level, RenderedHitBox renderedHitBoxes)
  {
    Document doc = codeDiv.getOwnerDocument();

    TokenRenderer renderer = new TokenRenderer(doc);
    int lineno = 0;
    for (TokenContainer line: codeList.statements)
    {
      DivElement div = doc.createDivElement();
      RenderedHitBox lineHitBox = null;
      if (renderedHitBoxes != null)
      {
        lineHitBox = new RenderedHitBox(div);
        lineHitBox.children = new ArrayList<>();
        renderedHitBoxes.children.add(lineHitBox);
      }
      renderLine(line, pos != null && lineno == pos.getOffset(level) ? pos : null, level + 1, div, renderer, lineHitBox);

      codeDiv.appendChild(div);
      lineno++;
    }
    if (codeList.statements.isEmpty()) 
    {
      DivElement div = doc.createDivElement();
      if (pos != null)
      {
        DivElement toInsert = doc.createDivElement();
        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
        div.appendChild(toInsert.querySelector("div"));
      }
      else
        div.setTextContent("\u00A0");
      codeDiv.appendChild(div);
    }
  }

  static void renderLine(TokenContainer line, CodePosition pos, int level, Element div, TokenRenderer renderer, RenderedHitBox lineHitBox)
  {
    // Check if the line contains some wide tokens
    boolean hasWideTokens = false;
    for (Token tok: line.tokens) 
    {
      if (tok.isWide())
      {
        hasWideTokens = true;
        break;
      }
    }
    
    // Actually render the line
    DivElement subdiv = null;
    Document doc = div.getOwnerDocument();
    int tokenno = 0;
    TokenRendererReturn returnedRenderedToken = new TokenRendererReturn(); 
    for (Token tok: line.tokens)
    {
      RenderedHitBox hitBox = null;
      if (lineHitBox != null)
        hitBox = new RenderedHitBox();
      tok.visit(renderer, returnedRenderedToken, pos != null && pos.getOffset(level) == tokenno && pos.hasOffset(level + 1) ? pos : null, level + 1, hitBox);
      Element el = returnedRenderedToken.el;
      // Put non-wide tokens in a div line
      if (hasWideTokens && !tok.isWide())
      {
        if (subdiv == null)
        {
          subdiv = doc.createDivElement();
          div.appendChild(subdiv);
        }
        subdiv.appendChild(el);
      }
      else
      {
        div.appendChild(el);
        subdiv = null;
      }
      if (pos != null && !pos.hasOffset(level + 1) && tokenno == pos.getOffset(level))
      {
        DivElement toInsert = doc.createDivElement();
        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
        Element beforePoint = returnedRenderedToken.beforeInsertionPoint;
        beforePoint.getParentElement().insertBefore(toInsert.querySelector("div"), beforePoint);
      }
      if (lineHitBox != null)
        lineHitBox.children.add(hitBox);
      tokenno++;
    }
    // If the last token is a wide token, there should be an empty line afterwards
    // where additional content can go
    boolean needEmptyLineAtEnd = false;
    if (!line.tokens.isEmpty() && line.tokens.get(line.tokens.size() - 1).isWide())
      needEmptyLineAtEnd = true;
    if (needEmptyLineAtEnd)
    {
      subdiv = doc.createDivElement();
      div.appendChild(subdiv);
    }
    // Special handling for cursor at the end, or if line is empty with no cursor, put some blank content there
    if (pos != null && !pos.hasOffset(level + 1) && pos.getOffset(level) == line.tokens.size()) 
    {
      DivElement toInsert = doc.createDivElement();
      toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
      if (!line.tokens.isEmpty())
      {
        if (!needEmptyLineAtEnd)
        {
          if (subdiv == null)
            div.appendChild(toInsert.querySelector("div"));
          else
            subdiv.appendChild(toInsert.querySelector("div"));
        }
        else
          subdiv.appendChild(toInsert.querySelector("div"));
      }
      else
        div.appendChild(toInsert.querySelector("div"));
    }
    else if (line.tokens.isEmpty())
        div.setTextContent("\u00A0");
    else if (needEmptyLineAtEnd)
      subdiv.setTextContent("\u00a0");
  }

  public CodePosition renderAndHitDetect(int x, int y, DivElement codeDiv, StatementContainer codeList, CodePosition oldPos)
  {
    RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
    renderedHitBoxes.children = new ArrayList<>();
    codeDiv.setInnerHTML("");
    render(codeDiv, codeList, oldPos, renderedHitBoxes);
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
      tokens.tokens.get(tokenno).visit(new TokenInternalHitDetection(), x, y, renderedLineHitBoxes.children.get(tokenno), newPos, level + 1);
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
  
  // Does hit detection for compound tokens (tokens that contain expressions
  // and statements inside them)
  static class TokenInternalHitDetection implements Token.TokenVisitor5<Void, Integer, Integer, RenderedHitBox, CodePosition, Integer>
  {
    @Override
    public Void visitSimpleToken(SimpleToken token, Integer x,
        Integer y, RenderedHitBox hitBox, CodePosition pos, Integer level)
    {
      return null;
    }
    void hitDetectWideToken(int x, int y, RenderedHitBox hitBox,
        TokenContainer exprContainer, StatementContainer blockContainer,
        CodePosition pos, int level)
    {
      // Check inside the statement block
      if (hitBox.children.get(EXPRBLOCK_POS_BLOCK) != null
        && y > hitBox.children.get(EXPRBLOCK_POS_BLOCK).el.getOffsetTop())
      {
        pos.setOffset(level, EXPRBLOCK_POS_BLOCK);
        hitDetectStatementContainer(x, y, blockContainer, hitBox.children.get(EXPRBLOCK_POS_BLOCK), pos, level + 1);
      }
      // Check if we're inside the expression
      else if (hitBox.children.get(EXPRBLOCK_POS_EXPR) != null
          && (x > hitBox.children.get(EXPRBLOCK_POS_EXPR).el.getOffsetLeft() 
              || y > hitBox.children.get(EXPRBLOCK_POS_START).el.getOffsetTop() + hitBox.children.get(EXPRBLOCK_POS_START).el.getOffsetHeight()))
      {
        pos.setOffset(level, EXPRBLOCK_POS_EXPR);
        hitDetectTokens(x, y, exprContainer, hitBox.children.get(EXPRBLOCK_POS_EXPR), pos, level + 1);
      }
    }
    @Override
    public Void visitWideToken(WideToken token, Integer x,
        Integer y, RenderedHitBox hitBox, CodePosition pos, Integer level)
    {
      return null;
    }
    @Override
    public Void visitOneBlockToken(OneBlockToken token, Integer x,
        Integer y, RenderedHitBox hitBox, CodePosition pos, Integer level)
    {
      hitDetectWideToken(x, y, hitBox, null, token.block, pos, level);
      return null;
    }
    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, Integer x,
        Integer y, RenderedHitBox hitBox, CodePosition pos, Integer level)
    {
      hitDetectWideToken(x, y, hitBox, token.expression, token.block, pos, level);
      return null;
    }
    
  }

  
  // For figuring out which tokens should be used for predicting what
  // the next token should be at the cursor position 
  public static class ParseContextForCursor
  {
    public Symbol baseContext;
    public List<Token> tokens = new ArrayList<>();
  }
  
  public static ParseContextForCursor findPredictiveParseContextForStatements(StatementContainer statements, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return findPredictiveParseContextForLine(line, Symbol.FullStatement, pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = Symbol.FullStatement;
    return toReturn;
  }
  
  static ParseContextForCursor findPredictiveParseContextForLine(TokenContainer line, Symbol baseContext, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      return line.tokens.get(pos.getOffset(level)).visit(new TokenPredictiveParseContext(), pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = baseContext;
    toReturn.tokens.addAll(line.tokens.subList(0, pos.getOffset(level)));
    return toReturn;
  }
  
  static class TokenPredictiveParseContext implements Token.TokenVisitor2<ParseContextForCursor, CodePosition, Integer>
  {
    @Override
    public ParseContextForCursor visitSimpleToken(SimpleToken token,
        CodePosition pos, Integer level)
    {
      throw new IllegalArgumentException();
    }
    ParseContextForCursor handleWideToken(
        TokenContainer exprContainer, StatementContainer blockContainer,
        CodePosition pos, int level)
    {
      if (exprContainer != null && pos.getOffset(level) == EXPRBLOCK_POS_EXPR)
      {
        return findPredictiveParseContextForLine(exprContainer, Symbol.ExpressionOnly, pos, level + 1);
      }
      else if (blockContainer != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
      {
        return findPredictiveParseContextForStatements(blockContainer, pos, level + 1);
      }
      throw new IllegalArgumentException();
      
    }
    @Override
    public ParseContextForCursor visitWideToken(WideToken token,
        CodePosition pos, Integer level)
    {
      return handleWideToken(null, null, pos, level);
    }
    @Override
    public ParseContextForCursor visitOneBlockToken(OneBlockToken token,
        CodePosition pos, Integer level)
    {
      return handleWideToken(null, token.block, pos, level);
    }
    @Override
    public ParseContextForCursor visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, CodePosition pos, Integer level)
    {
      return handleWideToken(token.expression, token.block, pos, level);
    }
    
  }
  
  public static void insertTokenIntoStatementContainer(StatementContainer stmtContainer, Token newToken, CodePosition pos, int level)
  {
    if (stmtContainer.statements.isEmpty()) 
    {
      stmtContainer.statements.add(new TokenContainer(Collections.emptyList()));
    }
    TokenContainer line = stmtContainer.statements.get(pos.getOffset(level));
    if (!pos.hasOffset(level + 2) && !newToken.isWide() 
        && line.tokens.size() > pos.getOffset(level + 1)
        && line.tokens.get(pos.getOffset(level + 1)).isWide())
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
      insertTokenIntoLine(line, newToken, pos, level + 1);
      stmtContainer.statements.add(pos.getOffset(level) + 1, newline);
    }
    else
      insertTokenIntoLine(line, newToken, pos, level + 1);
  }
  
  static void insertTokenIntoLine(TokenContainer line, Token newToken, CodePosition pos, int level)
  {
    if (pos.hasOffset(level + 1))
    {
      Token token = line.tokens.get(pos.getOffset(level));
      token.visit(new TokenVisitor3<Void, Token, CodePosition, Integer>() {
        @Override
        public Void visitSimpleToken(SimpleToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          throw new IllegalArgumentException();
        }
        Void handleWideToken(TokenContainer exprContainer, StatementContainer blockContainer,
            Token newToken, CodePosition pos, int level)
        {
          if (exprContainer != null && pos.getOffset(level) == EXPRBLOCK_POS_EXPR)
          {
            insertTokenIntoLine(exprContainer, newToken, pos, level + 1);
            return null;
          }
          else if (blockContainer != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
          {
            insertTokenIntoStatementContainer(blockContainer, newToken, pos, level + 1);
            return null;
          }
          throw new IllegalArgumentException();
        }
        @Override
        public Void visitWideToken(WideToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(null, null, newToken, pos, level);
        }
        @Override
        public Void visitOneBlockToken(OneBlockToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(null, token.block, newToken, pos, level);
        }
        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(token.expression, token.block, newToken, pos, level);
        }
        
      }, newToken, pos, level + 1);
      return;
    }
    line.tokens.add(pos.getOffset(level), newToken);
    pos.setOffset(level, pos.getOffset(level) + 1);
  }

  public static void insertNewlineIntoStatementContainer(
      StatementContainer codeList, CodePosition pos, int level)
  {
    TokenContainer line = codeList.statements.get(pos.getOffset(level));
    if (pos.hasOffset(level + 2))
    {
      Token token = line.tokens.get(pos.getOffset(level + 1));
      token.visit(new TokenVisitor2<Void, CodePosition, Integer>() {
        @Override
        public Void visitSimpleToken(SimpleToken token, CodePosition pos,
            Integer level)
        {
          throw new IllegalArgumentException();
        }
        void handleWideToken(StatementContainer blockContainer, CodePosition pos,
            int level)
        {
          if (blockContainer != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
          {
            insertNewlineIntoStatementContainer(blockContainer, pos, level + 1);
          }
        }
        @Override
        public Void visitWideToken(WideToken token, CodePosition pos,
            Integer level)
        {
          throw new IllegalArgumentException();
        }
        @Override
        public Void visitOneBlockToken(OneBlockToken token, CodePosition pos,
            Integer level)
        {
          handleWideToken(token.block, pos, level);
          return null;
        }
        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, CodePosition pos,
            Integer level)
        {
          handleWideToken(token.block, pos, level);
          return null;
        }
        
      }, pos, level + 2);
      return;
    }
    
    TokenContainer newline = new TokenContainer(line.tokens.subList(pos.getOffset(level + 1), line.tokens.size()));
    for (int n = line.tokens.size() - 1; n >= pos.getOffset(level + 1); n--)
       line.tokens.remove(n);
    pos.setOffset(level, pos.getOffset(level) + 1);
    codeList.statements.add(pos.getOffset(level), newline);
    pos.setOffset(level + 1, 0);
  }
  

}
