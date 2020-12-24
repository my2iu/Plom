package org.programmingbasics.plom.core.view;

import java.util.ArrayList;

import org.programmingbasics.plom.core.UIResources;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.FontStyle;
import elemental.css.CSSStyleDeclaration.WhiteSpace;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.svg.SVGDocument;
import elemental.svg.SVGSVGElement;
import elemental.svg.SVGTextElement;

public class SvgCodeRenderer
{
  static final int EXPRBLOCK_POS_START = CodeRenderer.EXPRBLOCK_POS_START;
  static final int EXPRBLOCK_POS_EXPR = CodeRenderer.EXPRBLOCK_POS_EXPR;
  static final int EXPRBLOCK_POS_BLOCK = CodeRenderer.EXPRBLOCK_POS_BLOCK;
  
  static final int PARAMTOK_POS_TEXTS = CodeRenderer.PARAMTOK_POS_TEXTS;
  static final int PARAMTOK_POS_EXPRS = CodeRenderer.PARAMTOK_POS_EXPRS;
  
  static class RenderSupplementalInfo extends CodeRenderer.RenderSupplementalInfo
  {
    
  }
  
  public static void test()
  {
    SVGDocument doc = (SVGDocument)Browser.getDocument();
    SVGSVGElement svgEl = doc.createSVGElement();
    doc.getBody().appendChild(svgEl);
    
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SvgTextWidthCalculator(doc);
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, 15, widthCalculator);
    Token tok = new Token.SimpleToken("22 adf df", Symbol.Number);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    tok.visit(tokenRenderer, returned, positioning, 0, null, hitBox);
    
    svgEl.setInnerHTML(returned.svgString);
  }
  
  public static void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, CodePosition selectionPos1, CodePosition selectionPos2, RenderedHitBox renderedHitBoxes, ErrorList codeErrors)
  {
    RenderSupplementalInfo supplement = new RenderSupplementalInfo();
    supplement.codeErrors = codeErrors;
    supplement.nesting = new CodeNestingCounter();
    supplement.nesting.calculateNestingForStatements(codeList);
    supplement.selectionStart = selectionPos1;
    supplement.selectionEnd = selectionPos2;
    renderStatementContainer(codeDiv, codeList, pos, 0, new CodePosition(), renderedHitBoxes, supplement);
  }

  public static RenderedHitBox renderWithHitBoxes(DivElement codeDiv, StatementContainer codeList, CodePosition pos, CodePosition selectionPos1, CodePosition selectionPos2, ErrorList codeErrors)
  {
    RenderedHitBox renderedHitBoxes = RenderedHitBox.withChildren();
    render(codeDiv, codeList, pos, selectionPos1, selectionPos2, renderedHitBoxes, codeErrors);
    return renderedHitBoxes;
  }

  public static RenderedHitBox renderTypeToken(DivElement div, Token type, CodePosition pos)
  {
    RenderedHitBox hitBox = new RenderedHitBox(null);
    RenderSupplementalInfo supplement = new RenderSupplementalInfo();
    supplement.codeErrors = new ErrorList();
    supplement.nesting = new CodeNestingCounter();
    supplement.nesting.calculateNestingForStatements(type == null ? new StatementContainer(new TokenContainer()) : new StatementContainer(new TokenContainer(type)));
    supplement.renderTypeFieldStyle = true;
    CodeRenderer.TokenRenderer renderer = new CodeRenderer.TokenRenderer(Browser.getDocument(), supplement);
    if (type != null)
    {
      CodeRenderer.TokenRendererReturn returnedRenderedToken = new CodeRenderer.TokenRendererReturn();
      CodePosition currentTokenPos = new CodePosition();
      currentTokenPos.setOffset(0, 0);
      type.visit(renderer, returnedRenderedToken, pos, 1, currentTokenPos, hitBox);
      currentTokenPos.setMaxOffset(1);
      Element el = returnedRenderedToken.el;
      div.setInnerHTML("");
      div.appendChild(el);
      if (pos != null && !pos.hasOffset(1))
        el.getClassList().add("typeTokenSelected");
      return hitBox;
    }
    else
    {
      if (pos != null)
      {
        DivElement toInsert = Browser.getDocument().createDivElement();
        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
        div.appendChild(toInsert.querySelector("div"));
      }
      else
        div.setTextContent("\u00A0");
      return null;
    }
  }
  
  /** Holds data to be returned about how a token is rendered */
  static class TokenRendererReturn
  {
    /** Element which holds the token */
    public Element el;
    /** Before cursor points--when inserting a cursor before this token, cursor should be inserted before this element */
    public Element beforeInsertionPoint;
    public String svgString;
    public double width;
  }
  
  static class TokenRendererPositioning
  {
    
  }
  
  static interface TextWidthCalculator
  {
    public double calculateWidth(String text);
  }
  
  static class SvgTextWidthCalculator implements TextWidthCalculator
  {
    SVGDocument doc;
    SVGTextElement textEl;
    SvgTextWidthCalculator(SVGDocument doc)
    {
      SVGSVGElement svgEl = doc.createSVGElement();
      textEl = doc.createSVGTextElement();
      textEl.getClassList().add("codetoken");
      svgEl.appendChild(textEl);
      doc.getBody().appendChild(svgEl);
    }
    @Override public double calculateWidth(String text)
    {
      textEl.setTextContent(text);
      return textEl.getComputedTextLength();
    }
  }
  
  static class TokenRenderer implements Token.TokenVisitor5<Void, TokenRendererReturn, TokenRendererPositioning, Integer, CodePosition, RenderedHitBox>
  {
    Document doc;
    RenderSupplementalInfo supplement;
    final int textHeight;
    final int horzPadding = 5;
    final int vertPadding = 3;
    final int descenderHeight;
    final TextWidthCalculator widthCalculator;
    TokenRenderer(Document doc, RenderSupplementalInfo supplement, int textHeight, TextWidthCalculator widthCalculator)
    {
      this.doc = doc;
      this.supplement = supplement;
      this.textHeight = textHeight;
      this.descenderHeight = (int)Math.ceil(textHeight * 0.2);
      this.widthCalculator = widthCalculator;
    }
    private void adjustTokenHeightForDepth(Element el, Token token)
    {
      int nesting = supplement.nesting.tokenNesting.getOrDefault(token, 1) - 1;
      if (nesting < 0) nesting = 0;
      el.getStyle().setProperty("line-height", (1.3 + nesting * 0.5) + "em");
      el.getStyle().setPaddingTop((nesting * 0.25) + "em");
      el.getStyle().setPaddingBottom((nesting * 0.25) + "em");
    }
    @Override
    public Void visitSimpleToken(SimpleToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      String text = token.contents;
      toReturn.svgString = "<rect width='" + (widthCalculator.calculateWidth(text) + horzPadding * 2)+ "' height='" + (textHeight + descenderHeight + vertPadding * 2) + "' class='codetoken'/>"
          + "<text x='" + horzPadding + "' y='" + (textHeight + vertPadding) + "' class='codetoken'>" + text + "</text>";
//      DivElement div = doc.createDivElement();
//      div.setClassName("token");
//      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
//        div.getClassList().add("tokenselected");
//      adjustTokenHeightForDepth(div, token);
//      div.setTextContent(token.contents);
//      if (hitBox != null)
//        hitBox.el = div;
//      toReturn.el = div;
//      toReturn.beforeInsertionPoint = div;
//      if (supplement.codeErrors.containsToken(token))
//        div.getClassList().add("tokenError");
      return null;
    }
    @Override
    public Void visitParameterToken(ParameterToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
//      Element span = doc.createSpanElement();
//      span.setClassName("token");
//      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
//        span.getClassList().add("tokenselected");
//      adjustTokenHeightForDepth(span, token);
//      RenderedHitBox textHitBoxes = null;
//      RenderedHitBox exprHitBoxes = null;
//      if (hitBox != null)
//      {
//        hitBox.children = new ArrayList<>();
//        hitBox.children.add(null);
//        hitBox.children.add(null);
//        textHitBoxes = RenderedHitBox.withChildren();
//        exprHitBoxes = RenderedHitBox.withChildren();
//        hitBox.children.set(PARAMTOK_POS_TEXTS, textHitBoxes);
//        hitBox.children.set(PARAMTOK_POS_EXPRS, exprHitBoxes);
//        hitBox.el = span;
//      }
//
//      // Render out each parameter
//      for (int n = 0; n < token.contents.size(); n++)
//      {
//        SpanElement textSpan = doc.createSpanElement();
//        textSpan.setTextContent((n > 0 ? " " : "") + token.contents.get(n) + "\u00a0");
//        if (supplement.codeErrors.containsToken(token))
//          textSpan.getClassList().add("tokenError");
//        SpanElement exprSpan = doc.createSpanElement();
//        span.appendChild(textSpan);
//        span.appendChild(exprSpan);
//        RenderedHitBox exprHitBox = null;
//        RenderedHitBox textHitBox = null; 
//        if (hitBox != null)
//        {
//          exprHitBox = RenderedHitBox.withChildren(exprSpan); 
//          exprHitBoxes.children.add(exprHitBox);
//          textHitBox = new RenderedHitBox(textSpan);
//          textHitBoxes.children.add(textHitBox);
//        }
//        boolean posInExpr = pos != null && pos.getOffset(level) == PARAMTOK_POS_EXPRS && pos.getOffset(level + 1) == n;
//        currentTokenPos.setOffset(level, PARAMTOK_POS_EXPRS);
//        currentTokenPos.setOffset(level + 1, n);
//        renderLine(token.parameters.get(n), posInExpr ? pos : null, level + 2, currentTokenPos, exprSpan, false, this, exprHitBox, supplement);
//        currentTokenPos.setMaxOffset(level + 1);
//      }
//      // Handle any postfix for the token
//      SpanElement endSpan = doc.createSpanElement();
//      if (token.postfix != null && !token.postfix.isEmpty())
//      {
//        endSpan.setTextContent(token.postfix);
//        if (supplement.codeErrors.containsToken(token))
//          endSpan.getClassList().add("tokenError");
//      }
//      else
//        endSpan.setTextContent("\u00a0\u00a0");
//      span.appendChild(endSpan);
//      if (hitBox != null)
//      {
//        RenderedHitBox endHitBox = new RenderedHitBox(endSpan);
//        textHitBoxes.children.add(endHitBox);
//      }
//
//      toReturn.el = span;
//      toReturn.beforeInsertionPoint = span;
      return null;
    }
    @Override
    public Void visitWideToken(WideToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      createWideToken(token, token.contents, null, null, toReturn, positioning, level, currentTokenPos, hitBox);
      if (token.type == Symbol.DUMMY_COMMENT)
      {
        toReturn.el.getStyle().setWhiteSpace(WhiteSpace.PRE_WRAP);
        toReturn.el.getStyle().setFontStyle(FontStyle.ITALIC);
      }
      return null;
    }
    @Override
    public Void visitOneBlockToken(OneBlockToken token,
        TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      createWideToken(token, token.contents, null, token.block, toReturn, positioning, level, currentTokenPos, hitBox);
      return null;
    }
    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      createWideToken(token, token.contents, token.expression, token.block, toReturn, positioning, level, currentTokenPos, hitBox);
      return null;
    }
    private void createWideToken(Token token, String tokenText, TokenContainer exprContainer,
        StatementContainer blockContainer,
        TokenRendererReturn toReturn, TokenRendererPositioning positioning, int level, CodePosition currentTokenPos,
        RenderedHitBox hitBox)
    {
//      DivElement div = doc.createDivElement();
//      div.setClassName("blocktoken");
//      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
//        div.getClassList().add("tokenselected");
//
//      RenderedHitBox startTokenHitBox = null;
//      if (hitBox != null)
//      {
//        hitBox.children = new ArrayList<>();
//        hitBox.children.add(null);
//        hitBox.children.add(null);
//        hitBox.children.add(null);
//        startTokenHitBox = new RenderedHitBox();
//        hitBox.children.set(EXPRBLOCK_POS_START, startTokenHitBox);
//      }
//
//      // First line with optional expression
//      DivElement startLine = doc.createDivElement();
//      startLine.getClassList().add("tokenline");
//      SpanElement start = doc.createSpanElement();
//      startLine.appendChild(start);
//      if (supplement.codeErrors.containsToken(token))
//        start.getClassList().add("tokenError");
//      if (hitBox != null)
//        startTokenHitBox.el = start;
//      if (exprContainer != null)
//      {
//        start.setTextContent(tokenText + " (");
//        SpanElement expression = doc.createSpanElement();
//        RenderedHitBox exprHitBox = (hitBox != null) ? RenderedHitBox.withChildren() : null;
//        currentTokenPos.setOffset(level, EXPRBLOCK_POS_EXPR);
//        renderLine(exprContainer, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_EXPR ? pos : null, level + 1, currentTokenPos, expression, false, this, exprHitBox, supplement);
//        currentTokenPos.setMaxOffset(level + 1);
//        SpanElement middle = doc.createSpanElement();
//        if (blockContainer == null)
//          middle.setTextContent(")");
//        else
//          middle.setTextContent(") {");
//        startLine.appendChild(expression);
//        startLine.appendChild(middle);
//        if (hitBox != null)
//        {
//          exprHitBox.el = expression;
//          hitBox.children.set(EXPRBLOCK_POS_EXPR, exprHitBox);
//        }
//      }
//      else
//      {
//        if (blockContainer != null)
//          start.setTextContent(tokenText + " {");
//        else
//          start.setTextContent(tokenText);
//      }
//      div.appendChild(startLine);
//
//      // Block part
//      if (blockContainer != null)
//      {
//        DivElement block = doc.createDivElement();
//        block.getStyle().setPaddingLeft(1, Unit.EM);
//        RenderedHitBox blockHitBox = (hitBox != null) ? RenderedHitBox.withChildren() : null;
//        currentTokenPos.setOffset(level, EXPRBLOCK_POS_BLOCK);
//        renderStatementContainer(block, blockContainer, pos != null && pos.getOffset(level) == EXPRBLOCK_POS_BLOCK ? pos : null, level + 1, currentTokenPos, blockHitBox, supplement);
//        currentTokenPos.setMaxOffset(level + 1);
//        if (hitBox != null)
//        {
//          blockHitBox.el = block;
//          hitBox.children.set(EXPRBLOCK_POS_BLOCK, blockHitBox);
//        }
//        div.appendChild(block);
//        
//        // With ending "}"
//        DivElement endLine = doc.createDivElement();
//        SpanElement end = doc.createSpanElement();
//        end.setTextContent("}");
//        endLine.appendChild(end);
//        div.appendChild(endLine);
//      }
//
//      if (hitBox != null)
//        hitBox.el = div;
//      toReturn.el = div;
//      toReturn.beforeInsertionPoint = start;
    }
  }

  static void renderStatementContainer(DivElement codeDiv, StatementContainer codeList, CodePosition pos, int level, CodePosition currentTokenPos, RenderedHitBox renderedHitBoxes, RenderSupplementalInfo supplement)
  {
    Document doc = codeDiv.getOwnerDocument();

    CodeRenderer.TokenRenderer renderer = new CodeRenderer.TokenRenderer(doc, supplement);
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
      currentTokenPos.setOffset(level, lineno);
      renderLine(line, pos != null && lineno == pos.getOffset(level) ? pos : null, level + 1, currentTokenPos, div, true, renderer, lineHitBox, supplement);
      currentTokenPos.setMaxOffset(level + 1);
      
      codeDiv.appendChild(div);
      lineno++;
    }
    if (codeList.statements.isEmpty()) 
    {
      DivElement div = doc.createDivElement();
      if (renderedHitBoxes != null)
      {
        // Insert an empty hitbox for a blank line even though there's no corresponding tokencontainer
        RenderedHitBox lineHitBox = new RenderedHitBox(div);
        lineHitBox.children = new ArrayList<>();
        renderedHitBoxes.children.add(lineHitBox);
      }
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

  static void renderLine(TokenContainer line, CodePosition pos, int level, CodePosition currentTokenPos, Element div, boolean isStatement, CodeRenderer.TokenRenderer renderer, RenderedHitBox lineHitBox, RenderSupplementalInfo supplement)
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

    // Mark the parent container if it only contains non-wide tokens
    if (!hasWideTokens && isStatement)
      div.getClassList().add("tokenline");
    
    // Actually render the line
    DivElement subdiv = null;
    Document doc = div.getOwnerDocument();
    int tokenno = 0;
    CodeRenderer.TokenRendererReturn returnedRenderedToken = new CodeRenderer.TokenRendererReturn(); 
    for (Token tok: line.tokens)
    {
      RenderedHitBox hitBox = null;
      if (lineHitBox != null)
        hitBox = new RenderedHitBox();
      currentTokenPos.setOffset(level, tokenno);
      tok.visit(renderer, returnedRenderedToken, pos != null && pos.getOffset(level) == tokenno && pos.hasOffset(level + 1) ? pos : null, level + 1, currentTokenPos, hitBox);
      currentTokenPos.setMaxOffset(level + 1);
      Element el = returnedRenderedToken.el;
      if (supplement.renderTypeFieldStyle && pos != null && !pos.hasOffset(level + 1))
        el.getClassList().add("typeTokenSelected");
      // Put non-wide tokens in a div line
      if (hasWideTokens && !tok.isWide())
      {
        if (subdiv == null)
        {
          subdiv = doc.createDivElement();
          subdiv.getClassList().add("tokenline");
          div.appendChild(subdiv);
        }
        subdiv.appendChild(doc.createTextNode("\u200B"));  // Need a zero-width space afterwards so that the line will wrap between tokens
        subdiv.appendChild(el);
      }
      else
      {
        // No zero-width space between wide tokens
//        div.appendChild(doc.createTextNode("\u200B"));  // Need a zero-width space afterwards so that the line will wrap between tokens
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
    if (!line.tokens.isEmpty() && line.endsWithWideToken())
      needEmptyLineAtEnd = true;
    if (needEmptyLineAtEnd)
    {
      subdiv = doc.createDivElement();
      div.appendChild(subdiv);
      if (lineHitBox != null)
        lineHitBox.children.add(new RenderedHitBox(subdiv));
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
