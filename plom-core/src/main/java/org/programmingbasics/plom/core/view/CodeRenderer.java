package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.programmingbasics.plom.core.UIResources;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor2;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.css.CSSStyleDeclaration.FontStyle;
import elemental.css.CSSStyleDeclaration.Unit;
import elemental.css.CSSStyleDeclaration.WhiteSpace;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;

public class CodeRenderer
{
  static final int EXPRBLOCK_POS_START = 0;
  static final int EXPRBLOCK_POS_EXPR = 1;
  static final int EXPRBLOCK_POS_BLOCK = 2;
  
  static final int PARAMTOK_POS_TEXTS = 0;
  static final int PARAMTOK_POS_EXPRS = 1;
  
//  private static final int EXPRBLOCK_POS_END = 3;
  
  public static void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
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
    public Void visitParameterToken(ParameterToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      Element span = doc.createSpanElement();
      span.setClassName("token");
      RenderedHitBox textHitBoxes = null;
      RenderedHitBox exprHitBoxes = null;
      if (hitBox != null)
      {
        hitBox.children = new ArrayList<>();
        hitBox.children.add(null);
        hitBox.children.add(null);
        textHitBoxes = RenderedHitBox.withChildren();
        exprHitBoxes = RenderedHitBox.withChildren();
        hitBox.children.set(PARAMTOK_POS_TEXTS, textHitBoxes);
        hitBox.children.set(PARAMTOK_POS_EXPRS, exprHitBoxes);
        hitBox.el = span;
      }

      // Render out each parameter
      for (int n = 0; n < token.contents.size(); n++)
      {
        SpanElement textSpan = doc.createSpanElement();
        textSpan.setTextContent((n > 0 ? " " : "") + token.contents.get(n));
        SpanElement exprSpan = doc.createSpanElement();
        span.appendChild(textSpan);
        span.appendChild(exprSpan);
        RenderedHitBox exprHitBox = null;
        RenderedHitBox textHitBox = null; 
        if (hitBox != null)
        {
          exprHitBox = RenderedHitBox.withChildren(exprSpan); 
          exprHitBoxes.children.add(exprHitBox);
          textHitBox = new RenderedHitBox(textSpan);
          textHitBoxes.children.add(textHitBox);
        }
        boolean posInExpr = pos != null && pos.getOffset(level) == PARAMTOK_POS_EXPRS && pos.getOffset(level + 1) == n;
        renderLine(token.parameters.get(n), posInExpr ? pos : null, level + 2, exprSpan, this, exprHitBox);
      }
      // Handle any postfix for the token
      SpanElement endSpan = doc.createSpanElement();
      if (token.postfix != null)
        endSpan.setTextContent(token.postfix);
      span.appendChild(endSpan);
      if (hitBox != null)
      {
        RenderedHitBox endHitBox = new RenderedHitBox(endSpan);
        textHitBoxes.children.add(endHitBox);
      }

      toReturn.el = span;
      toReturn.beforeInsertionPoint = span;
      return null;
    }
    @Override
    public Void visitWideToken(WideToken token, TokenRendererReturn toReturn, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      createWideToken(token.contents, null, null, toReturn, pos, level, hitBox);
      if (token.type == Symbol.DUMMY_COMMENT)
      {
        toReturn.el.getStyle().setWhiteSpace(WhiteSpace.PRE_WRAP);
        toReturn.el.getStyle().setFontStyle(FontStyle.ITALIC);
      }
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
        subdiv.appendChild(doc.createTextNode("\u200B"));  // Need a zero-width space afterwards so that the line will wrap between tokens
        subdiv.appendChild(el);
      }
      else
      {
        div.appendChild(doc.createTextNode("\u200B"));  // Need a zero-width space afterwards so that the line will wrap between tokens
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

  

}
