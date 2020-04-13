package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor2;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;

import elemental.client.Browser;
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
  
  void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
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
    /** After cursor points--when inserting a cursor after this token, cursor should be inserted after this element */
    public Element afterInsertionPoint;
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
      toReturn.afterInsertionPoint = div;
      return null;
    }
    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      DivElement div = doc.createDivElement();
      div.setClassName("blocktoken");

      RenderedHitBox startTokenHitBox = null;
      RenderedHitBox exprHitBox = null;
      RenderedHitBox blockHitBox = null;
      RenderedHitBox endHitBox = null;
      if (hitBox != null)
      {
        startTokenHitBox = new RenderedHitBox();
        exprHitBox = new RenderedHitBox();
        exprHitBox.children = new ArrayList<>();
        blockHitBox = new RenderedHitBox();
        blockHitBox.children = new ArrayList<>();
        endHitBox = new RenderedHitBox();
        hitBox.children = new ArrayList<>();
        hitBox.children.add(startTokenHitBox);
        hitBox.children.add(exprHitBox);
        hitBox.children.add(blockHitBox);
      }

      DivElement startLine = doc.createDivElement();
      SpanElement start = doc.createSpanElement();
      start.setTextContent(token.contents + " (");
      SpanElement expression = doc.createSpanElement();
      renderLine(token.expression, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_EXPR ? pos : null, level + 1, expression, this, exprHitBox);
      if (token.expression.tokens.isEmpty())
        expression.setTextContent("\u00A0");
      SpanElement middle = doc.createSpanElement();
      middle.setTextContent(") {");
      startLine.appendChild(start);
      startLine.appendChild(expression);
      startLine.appendChild(middle);

      DivElement block = doc.createDivElement();
      block.getStyle().setPaddingLeft(1, Unit.EM);
      renderStatementContainer(block, token.block, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK ? pos : null, level + 1, blockHitBox);

      DivElement endLine = doc.createDivElement();
      SpanElement end = doc.createSpanElement();
      end.setTextContent("}");
      endLine.appendChild(end);
      if (hitBox != null)
      {
        startTokenHitBox.el = start;
        exprHitBox.el = expression;
        blockHitBox.el = block;
        endHitBox.el = endLine;
        hitBox.children.add(endHitBox);
      }

      div.appendChild(startLine);
      div.appendChild(block);
      div.appendChild(endLine);
      if (hitBox != null)
        hitBox.el = div;
      toReturn.el = div;
      toReturn.beforeInsertionPoint = start;
      toReturn.afterInsertionPoint = end;
      return null;
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
  }

  static void renderLine(TokenContainer line, CodePosition pos, int level, Element div, TokenRenderer renderer, RenderedHitBox lineHitBox)
  {
    Document doc = div.getOwnerDocument();
    int tokenno = 0;
    TokenRendererReturn returnedRenderedToken = new TokenRendererReturn(); 
    for (Token tok: line.tokens)
    {
      RenderedHitBox hitBox = null;
      if (lineHitBox != null)
        hitBox = new RenderedHitBox();
      tok.visit(renderer, returnedRenderedToken, pos != null && pos.hasOffset(level + 1) ? pos : null, level + 1, hitBox);
      Element el = returnedRenderedToken.el;
      div.appendChild(el);
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
    if (pos != null && !pos.hasOffset(level + 1) && pos.getOffset(level) == line.tokens.size()) 
    {
      DivElement toInsert = doc.createDivElement();
      toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
      if (!line.tokens.isEmpty())
      {
        Element afterPoint = returnedRenderedToken.afterInsertionPoint;
        afterPoint.getParentElement().insertBefore(toInsert.querySelector("div"), afterPoint.getNextSibling());
      }
      else
        div.appendChild(toInsert.querySelector("div"));
    }
    else if (line.tokens.isEmpty())
      div.setTextContent("\u00A0");
  }

  CodePosition renderAndHitDetect(int x, int y, DivElement codeDiv, StatementContainer codeList, CodePosition oldPos)
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
        tokenno = n;
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

    @Override
    public TokenHitLocation visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, Integer x,
        Integer y, RenderedHitBox hitBox)
    {
      Element el = hitBox.el;
      if (el.getOffsetTop() + el.getOffsetHeight() < y) return TokenHitLocation.AFTER;
      if (hitBox.children.size() > 3 && hitBox.children.get(3).el.getOffsetTop() < y) return TokenHitLocation.AFTER;
      if (el.getOffsetTop() < y) return TokenHitLocation.ON;
      return TokenHitLocation.NONE;
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

    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, Integer x,
        Integer y, RenderedHitBox hitBox, CodePosition pos, Integer level)
    {
      // Check inside the statement block
      if (y > hitBox.children.get(EXPRBLOCK_POS_BLOCK).el.getOffsetTop())
      {
        pos.setOffset(level, EXPRBLOCK_POS_BLOCK);
        hitDetectStatementContainer(x, y, token.block, hitBox.children.get(EXPRBLOCK_POS_BLOCK), pos, level + 1);
      }
      else if (x > hitBox.children.get(EXPRBLOCK_POS_EXPR).el.getOffsetLeft() 
          || y > hitBox.children.get(EXPRBLOCK_POS_START).el.getOffsetTop() + hitBox.children.get(EXPRBLOCK_POS_START).el.getOffsetHeight())
      {
        pos.setOffset(level, EXPRBLOCK_POS_EXPR);
        hitDetectTokens(x, y, token.expression, hitBox.children.get(EXPRBLOCK_POS_EXPR), pos, level + 1);
      }
      return null;
    }
    
  }

  
  // For figuring out which tokens should be used for predicting what
  // the next token should be at the cursor position 
  static class ParseContextForCursor
  {
    Symbol baseContext;
    List<Token> tokens = new ArrayList<>();
  }
  
  static ParseContextForCursor findPredictiveParseContextForStatements(StatementContainer statements, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < statements.statements.size())
    {
      TokenContainer line = statements.statements.get(pos.getOffset(level));
      return findPredictiveParseContextForLine(line, Symbol.Statement, pos, level + 1);
    }
    ParseContextForCursor toReturn = new ParseContextForCursor();
    toReturn.baseContext = Symbol.Statement;
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

    @Override
    public ParseContextForCursor visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, CodePosition pos, Integer level)
    {
      if (pos.getOffset(level) == EXPRBLOCK_POS_EXPR)
      {
        return findPredictiveParseContextForLine(token.expression, Symbol.ExpressionOnly, pos, level + 1);
      }
      else if (pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
      {
        return findPredictiveParseContextForStatements(token.block, pos, level + 1);
      }
      throw new IllegalArgumentException();
    }
    
  }
  
  static void insertTokenIntoStatementContainer(StatementContainer stmtContainer, Token newToken, CodePosition pos, int level)
  {
    TokenContainer line = stmtContainer.statements.get(pos.getOffset(level));
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

        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          if (pos.getOffset(level) == EXPRBLOCK_POS_EXPR)
          {
            insertTokenIntoLine(token.expression, newToken, pos, level + 1);
          }
          else if (pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
          {
            insertTokenIntoStatementContainer(token.block, newToken, pos, level + 1);
          }
          return null;
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

        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, CodePosition pos,
            Integer level)
        {
          if (pos.getOffset(level) == EXPRBLOCK_POS_BLOCK)
          {
            insertNewlineIntoStatementContainer(token.block, pos, level + 1);
          }
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
