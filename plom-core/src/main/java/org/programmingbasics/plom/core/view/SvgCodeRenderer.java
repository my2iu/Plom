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
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;
import org.programmingbasics.plom.core.view.RenderedHitBox.RectangleRenderedHitBox;

import elemental.client.Browser;
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
  
  static SVGSVGElement testSvgEl;
  static SVGSVGElement testSvgCursorOverlay;
  static SVGDocument testDoc;
  static SvgCodeRenderer.TextWidthCalculator testWidthCalculator;
  public static DivElement testDiv;
  public static RenderedHitBox testHitBox;
  public static void test()
  {
    SVGDocument doc = (SVGDocument)Browser.getDocument();
    DivElement newDiv = doc.createDivElement();
    newDiv.setClassName("codesidesplit");
    doc.getBody().appendChild(newDiv);
    newDiv.setInnerHTML("<svg style=\"width: 500px; height: 1000px;\"></svg><svg style=\"width: 500px; height: 1000px;\" class=\"cursoroverlay\">" + 
        "<g class=\"cursorscrolltransform\">" + 
        "<circle class=\"cursorhandle\" cx=\"0\" cy=\"0\" r=\"20\"/>" + 
        "<path class=\"cursorhandle\" d=\"M-20,-20 L 20, 0 L -20 20 z\"/>" + 
        "<line class=\"cursorcaret\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"0\"/>" + 
        "</g>" + 
        "</svg>");
    SVGSVGElement svgEl = (SVGSVGElement)newDiv.querySelectorAll("svg").item(0);
//    svgEl.getStyle().setWidth("500px");
//    svgEl.getStyle().setHeight("1000px");
//    doc.getBody().appendChild(svgEl);
    testSvgEl = svgEl;
    testDoc = doc;
    testSvgCursorOverlay = (SVGSVGElement)newDiv.querySelectorAll("svg").item(1);
    testWidthCalculator = new SvgTextWidthCalculator(doc);
    testDiv = newDiv;
    
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.WideToken("// Comment", Symbol.DUMMY_COMMENT),
            Token.ParameterToken.fromContents("@Type", Symbol.AtType),
            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable,
                new TokenContainer()),
            Token.ParameterToken.fromContents(".a:b:c:", Symbol.DotVariable,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".d:", Symbol.DotVariable, 
                        new TokenContainer(new Token.SimpleToken("12", Symbol.Number)))),
                new TokenContainer(),
                new TokenContainer(new Token.SimpleToken("32", Symbol.Number))
                ),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    Token.ParameterToken.fromContents(".and:", Symbol.DotVariable, 
                        new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)))), 
                new StatementContainer(
                    new TokenContainer(new Token.SimpleToken("64", Symbol.Number)))),
            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                new StatementContainer(
                    new TokenContainer(
                        new Token.OneExpressionOneBlockToken("while", Symbol.COMPOUND_WHILE, 
                            new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)), 
                            new StatementContainer(
                                new TokenContainer(
                                    new Token.SimpleToken("3", Symbol.Number)
                                    )))))),
            new Token.SimpleToken("55", Symbol.Number)
            )
        );
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    SvgCodeRenderer.TextWidthCalculator widthCalculator = testWidthCalculator;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
//    SvgCodeRenderer.renderLine(line, returned, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplementalInfo);
//    tok.visit(tokenRenderer, returned, positioning, 0, currentTokenPos, hitBox);
    
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
    
    if (testSvgEl != null)
    {
      SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
      supplementalInfo.codeErrors = new ErrorList();
      supplementalInfo.nesting = new CodeNestingCounter();
      SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
      SvgCodeRenderer.TextWidthCalculator widthCalculator = testWidthCalculator;
      SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
      SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
      CodePosition currentTokenPos = new CodePosition();
      SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
//      SvgCodeRenderer.renderLine(line, returned, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplementalInfo);
//      tok.visit(tokenRenderer, returned, positioning, 0, currentTokenPos, hitBox);
      
      testSvgEl.setInnerHTML(returned.svgString);
      RenderedHitBox hitBox = returned.hitBox;
      
      
      if (pos != null)
      {
        CursorRect cursorRect = RenderedCursorPosition.inStatements(codeList, pos, 0, hitBox);
        // Draw caret for the secondary cursor
        Element caretCursor = testSvgCursorOverlay.querySelector(".cursorcaret"); 
        if (cursorRect != null)
        {
          caretCursor.getStyle().clearDisplay();
          caretCursor.setAttribute("x1", "" + cursorRect.left);
          caretCursor.setAttribute("x2", "" + cursorRect.left);
          caretCursor.setAttribute("y1", "" + cursorRect.top);
          caretCursor.setAttribute("y2", "" + cursorRect.bottom);
        }
      }
      testHitBox = hitBox;
    }
  }

  public static RenderedHitBox renderSvgWithHitBoxes(SVGSVGElement svgEl, StatementContainer codeList, CodePosition pos, CodePosition selectionPos1, CodePosition selectionPos2, ErrorList codeErrors, SvgCodeRenderer.TextWidthCalculator widthCalculator)
  {
//    RenderedHitBox renderedHitBoxes = RenderedHitBox.withChildren();
//    render(codeDiv, codeList, pos, selectionPos1, selectionPos2, renderedHitBoxes, codeErrors);
    
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    
    svgEl.setInnerHTML(returned.svgString);
    RenderedHitBox hitBox = returned.hitBox;
    return hitBox;
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
    public double height;
    public RenderedHitBox hitBox;
    public void reset()
    {
      svgString = "";
      width = 0;
      height = 0;
      hitBox = null;
    }
  }
  
  static class TokenRendererPositioning
  {
    double lineStart = 0;
    double lineEnd = 100;
    double lineTop = 0;
    double lineBottom = 0;
    double fontSize = 15;
    double x = 0;
    int maxNestingForLine = 0;
    int currentNestingInLine = 0;
    void maxBottom(double lineHeight)
    {
      lineBottom = Math.max(lineBottom, lineTop + lineHeight);
    }
    void newline()
    {
      x = lineStart;
      lineTop = lineBottom;
    }
//    int getTextHeight()
//    {
//      return (int)Math.ceil(fontSize);
//    }
//    int getDescenderHeight()
//    {
//      return (int)Math.ceil(getTextHeight() * 0.2);
//    }
    TokenRendererPositioning copy()
    {
      TokenRendererPositioning copy = new TokenRendererPositioning();
      copy.lineStart = lineStart;
      copy.lineEnd = lineEnd;
      copy.lineTop = lineTop;
      copy.lineBottom = lineBottom;
      copy.fontSize = fontSize;
      copy.x = x;
      copy.maxNestingForLine = maxNestingForLine;
      copy.currentNestingInLine = currentNestingInLine;
      return copy;
    }
  }
  
  public static interface TextWidthCalculator
  {
    public double calculateWidth(String text);
  }
  
  public static class SvgTextWidthCalculator implements TextWidthCalculator
  {
    SVGDocument doc;
    SVGTextElement textEl;
    public SvgTextWidthCalculator(SVGDocument doc)
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
    final double INDENT_SIZE;
    TokenRenderer(Document doc, RenderSupplementalInfo supplement, int textHeight, TextWidthCalculator widthCalculator)
    {
      this.doc = doc;
      this.supplement = supplement;
      this.textHeight = textHeight;
      this.descenderHeight = (int)Math.ceil(textHeight * 0.2);
      this.widthCalculator = widthCalculator;
      this.INDENT_SIZE = 2 * horzPadding + Math.max(widthCalculator.calculateWidth("}"), widthCalculator.calculateWidth("{"));
    }
//    private void adjustTokenHeightForDepth(Element el, Token token)
//    {
//      int nesting = supplement.nesting.tokenNesting.getOrDefault(token, 1) - 1;
//      if (nesting < 0) nesting = 0;
//      el.getStyle().setProperty("line-height", (1.3 + nesting * 0.5) + "em");
//      el.getStyle().setPaddingTop((nesting * 0.25) + "em");
//      el.getStyle().setPaddingBottom((nesting * 0.25) + "em");
//    }
    @Override
    public Void visitSimpleToken(SimpleToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      String text = token.contents;
      double textWidth = widthCalculator.calculateWidth(text);
      toReturn.reset();
      double x = positioning.x;
      double y = positioning.lineTop;
      int totalVertPadding = (positioning.maxNestingForLine - positioning.currentNestingInLine) * vertPadding;
      toReturn.svgString = "<rect x='" + x + "' y='" + (y + positioning.currentNestingInLine * vertPadding) + "' width='" + (textWidth + horzPadding * 2)+ "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>"
          + "<text x='" + (x + horzPadding) + "' y='" + (y + textHeight + positioning.maxNestingForLine * vertPadding) + "' class='" + classList + "'>" + text + "</text>";
      toReturn.width = horzPadding * 2 + textWidth;
      toReturn.height = textHeight + descenderHeight + totalVertPadding * 2;
      toReturn.hitBox = RenderedHitBox.forRectangleWithChildren(x, y + positioning.currentNestingInLine * vertPadding, toReturn.width, toReturn.height);
      positioning.x += toReturn.width; 
//      DivElement div = doc.createDivElement();
//      div.setClassName("token");
//      adjustTokenHeightForDepth(div, token);
//      div.setTextContent(token.contents);
//      if (hitBox != null)
//        hitBox.el = div;
//      toReturn.el = div;
//      toReturn.beforeInsertionPoint = div;
      return null;
    }
    @Override
    public Void visitParameterToken(ParameterToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      toReturn.reset();
      double startX = positioning.x;
      double y = positioning.lineTop;
      double nextX = startX + horzPadding;
      int totalVertPadding = (positioning.maxNestingForLine - positioning.currentNestingInLine) * vertPadding;

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
      RenderedHitBox exprHitBoxes = RenderedHitBox.withChildren();
      
      String tokenText = "";
      String paramsSvg = "";
      // Render out each parameter
      TokenRendererReturn returned = new TokenRendererReturn();
      for (int n = 0; n < token.contents.size(); n++)
      {
//        SpanElement textSpan = doc.createSpanElement();
        if (n > 0) nextX += horzPadding;
        tokenText += 
            "<text x='" + (nextX) + "' y='" + (y + textHeight + positioning.maxNestingForLine * vertPadding) + "' class='" + classList + "'>" + token.contents.get(n) + "</text>";
        nextX += widthCalculator.calculateWidth(token.contents.get(n));
        nextX += horzPadding;
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
        currentTokenPos.setOffset(level, PARAMTOK_POS_EXPRS);
        currentTokenPos.setOffset(level + 1, n);
        positioning.x = nextX;
        TokenContainer line = token.parameters.get(n); 
//        supplement.nesting.calculateNestingForLine(line);
//        positioning.maxNestingForLine = supplement.nesting.expressionNesting.get(line);
        positioning.currentNestingInLine++;
        renderLine(line, returned, positioning, null, level + 2, currentTokenPos, null, false, this, null, supplement, 0);
        positioning.currentNestingInLine--;
        paramsSvg += returned.svgString;
        nextX = positioning.x;
        currentTokenPos.setMaxOffset(level + 1);
        exprHitBoxes.children.add(returned.hitBox);
      }
      // Handle any postfix for the token
//      SpanElement endSpan = doc.createSpanElement();
      if (token.postfix != null && !token.postfix.isEmpty())
      {
        tokenText += 
            "<text x='" + (nextX) + "' y='" + (y + textHeight + positioning.maxNestingForLine * vertPadding) + "' class='" + classList + "'>" + token.postfix + "</text>";
        nextX += widthCalculator.calculateWidth(token.postfix);
//        endSpan.setTextContent(token.postfix);
//        if (supplement.codeErrors.containsToken(token))
//          endSpan.getClassList().add("tokenError");
      }
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
      toReturn.svgString = "<rect x='" + startX + "' y='" + (y + positioning.currentNestingInLine * vertPadding) + "' width='" + (nextX - startX + horzPadding)+ "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>"
          + tokenText + "\n";
      toReturn.svgString += paramsSvg;
      toReturn.width = nextX + horzPadding - startX;
      toReturn.height = textHeight + descenderHeight + totalVertPadding * 2;
      toReturn.hitBox = RenderedHitBox.forRectangleWithChildren(startX, y + positioning.currentNestingInLine * vertPadding, toReturn.width, toReturn.height);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.set(SvgCodeRenderer.PARAMTOK_POS_EXPRS, exprHitBoxes);
      positioning.x = startX + toReturn.width; 
      return null;
    }
    @Override
    public Void visitWideToken(WideToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox hitBox)
    {
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      String textClassList = classList;
      if (token.type == Symbol.DUMMY_COMMENT)
        textClassList += " tokencomment";
      toReturn.reset();
      double x = positioning.x;
      double y = positioning.lineTop;
      double width = positioning.lineEnd - positioning.lineStart;
      String text = token.contents;
      toReturn.svgString = "<rect x='" + x + "' y='" + y + "' width='" + (width)+ "' height='" + (textHeight + descenderHeight + vertPadding * 2) + "' class='" + classList + "'/>";
      toReturn.svgString += "<text x='" + (x + horzPadding) + "' y='" + (y + textHeight + vertPadding) + "' class='" + textClassList + "'>" + text + "</text>";
      toReturn.width = width;
      toReturn.height = textHeight + descenderHeight + vertPadding * 2;
      toReturn.hitBox = RenderedHitBox.forRectangleWithChildren(x, y, toReturn.width, toReturn.height);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      positioning.maxBottom(toReturn.height);
      positioning.newline();
//      createWideToken(token, token.contents, null, null, toReturn, positioning, level, currentTokenPos, hitBox);
//      if (token.type == Symbol.DUMMY_COMMENT)
//      {
//        toReturn.el.getStyle().setWhiteSpace(WhiteSpace.PRE_WRAP);
//        toReturn.el.getStyle().setFontStyle(FontStyle.ITALIC);
//      }
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
        RenderedHitBox _hitBox)
    {
      final double INDENT = INDENT_SIZE;
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";

      double startX = positioning.x;
      double y = positioning.lineTop;
      double nextX = startX + horzPadding;
      String wideSvg = "";
      
      String text = tokenText;
      double textWidth = widthCalculator.calculateWidth(text);
      nextX += textWidth;
      int maxNesting = 1;
      int totalVertPadding = maxNesting * vertPadding;
      
      RenderedHitBox exprHitBox = null;
      RenderedHitBox blockHitBox = null;
      
      String startBracketSvg = "";
      if (exprContainer != null)
      {
        positioning.x = nextX + horzPadding;
        toReturn.reset();
        TokenRendererPositioning subPositioning = positioning.copy();
        subPositioning.lineTop += vertPadding;
        subPositioning.maxNestingForLine = supplement.nesting.expressionNesting.get(exprContainer);
        subPositioning.currentNestingInLine = 0;
        maxNesting = subPositioning.maxNestingForLine + 1;
        totalVertPadding = maxNesting * vertPadding;
        currentTokenPos.setOffset(level, EXPRBLOCK_POS_EXPR);
        renderLine(exprContainer, toReturn, subPositioning, null, level + 2, currentTokenPos, null, false, this, null, supplement, 0);
        currentTokenPos.setMaxOffset(level + 1);
        positioning.maxBottom(toReturn.height);
        wideSvg += toReturn.svgString;
        nextX = subPositioning.x;
        exprHitBox = toReturn.hitBox;
      }
      if (blockContainer != null)
      {
        startBracketSvg += "<text x='" + (nextX + horzPadding) + "' y='" + (y + textHeight + totalVertPadding) + "'>{</text>";
        nextX += horzPadding + widthCalculator.calculateWidth("{");
      }

      double firstLineHeight = textHeight + descenderHeight + totalVertPadding * 2;
      positioning.maxBottom(firstLineHeight);
      
      String endBracketSvg = "";
      if (blockContainer != null)
      {
        positioning.newline();
        toReturn.reset();
//        TokenRendererPositioning subPositioning = positioning.copy();
        double oldLineStart = positioning.lineStart; 
        positioning.x = positioning.lineStart = positioning.lineStart + INDENT;
        currentTokenPos.setOffset(level, EXPRBLOCK_POS_BLOCK);
        renderStatementContainer(blockContainer, toReturn, positioning, new CodePosition(), level + 2, currentTokenPos, this, null, supplement);
        currentTokenPos.setMaxOffset(level + 1);
        wideSvg += toReturn.svgString;
        positioning.lineStart = positioning.x = oldLineStart;
//        positioning.maxBottom(toReturn.height);
        
        // Add space for a '}' at the end
        positioning.newline();
        double endBracketY = positioning.lineTop + vertPadding + textHeight; 
        positioning.maxBottom(textHeight + descenderHeight + vertPadding * 2);
        endBracketSvg = "<text x='" + (startX + horzPadding) + "' y='" + (endBracketY) + "'>}</text>"; 
        positioning.newline();
        blockHitBox = toReturn.hitBox;
      }
      
      
      toReturn.reset();
      double width = nextX + horzPadding - startX;
      toReturn.svgString = "<path d=\'M" + startX + " " + y + " l " + width + " 0 l 0 "+  (textHeight + descenderHeight + totalVertPadding * 2) + " l -" + (width - INDENT) + " 0 L " + (startX + INDENT) + " " + positioning.lineBottom + " L " + (startX) + " " + positioning.lineBottom + " z\' class='" + classList + "'/>"; 
//      toReturn.svgString = "<rect x='" + startX + "' y='" + y + "' width='" + (width)+ "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>";
      toReturn.svgString += "<text x='" + (startX + horzPadding) + "' y='" + (y + textHeight + totalVertPadding) + "'>" + text + "</text>";
      toReturn.svgString += startBracketSvg;
      toReturn.svgString += wideSvg;
      toReturn.svgString += endBracketSvg;
      toReturn.width = width;
      toReturn.height = firstLineHeight;
      RectangleRenderedHitBox startTokenHitBox = RenderedHitBox.forRectangle(startX, y, toReturn.width, toReturn.height);
      RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(startX, y, toReturn.width, positioning.lineBottom - y);
      toReturn.hitBox = hitBox;
      hitBox.children.add(null);
      hitBox.children.add(null);
      hitBox.children.add(null);
      hitBox.children.set(EXPRBLOCK_POS_START, startTokenHitBox);
      hitBox.children.set(EXPRBLOCK_POS_EXPR, exprHitBox);
      hitBox.children.set(EXPRBLOCK_POS_BLOCK, blockHitBox);
      

      
//      positioning.newline();

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

  static void renderStatementContainer(StatementContainer codeList, TokenRendererReturn toReturn, TokenRendererPositioning positioning, CodePosition pos, int level, CodePosition currentTokenPos, TokenRenderer renderer, RenderedHitBox renderedHitBoxes, RenderSupplementalInfo supplement)
  {
//    Document doc = codeDiv.getOwnerDocument();

    int lineno = 0;
    String svgString = "";
    RenderedHitBox.RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(positioning.lineStart, positioning.lineTop, 0, 0);
    for (TokenContainer line: codeList.statements)
    {
      positioning.maxBottom(renderer.textHeight + renderer.vertPadding * 2 + renderer.descenderHeight);
//      DivElement div = doc.createDivElement();
//      RenderedHitBox lineHitBox = null;
//      if (renderedHitBoxes != null)
//      {
////        lineHitBox = new RenderedHitBox(div);
//        lineHitBox = RenderedHitBox.withChildren();
//        renderedHitBoxes.children.add(lineHitBox);
//      }
      currentTokenPos.setOffset(level, lineno);
      supplement.nesting.calculateNestingForLine(line);
      positioning.maxNestingForLine = supplement.nesting.expressionNesting.get(line);
      positioning.currentNestingInLine = 0;
      SvgCodeRenderer.renderLine(line, toReturn, positioning, new CodePosition(), 0, currentTokenPos, null, false, renderer, null, supplement, 1);
      svgString += toReturn.svgString;
      hitBox.children.add(toReturn.hitBox);
      currentTokenPos.setMaxOffset(level + 1);
      positioning.maxBottom(toReturn.height);
      positioning.newline();
//      codeDiv.appendChild(div);
      lineno++;
    }
    if (codeList.statements.isEmpty()) 
    {
//      DivElement div = doc.createDivElement();
      positioning.maxBottom(renderer.textHeight + renderer.vertPadding * 2 + renderer.descenderHeight);
      
      // Insert an empty hitbox for a blank line even though there's no corresponding tokencontainer
      RenderedHitBox lineHitBox = RenderedHitBox.forRectangleWithChildren(positioning.lineStart, positioning.lineTop, 0, positioning.lineBottom - positioning.lineTop);
      hitBox.children.add(lineHitBox);
      
//      if (pos != null)
//      {
//        DivElement toInsert = doc.createDivElement();
//        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
//        div.appendChild(toInsert.querySelector("div"));
//      }
//      else
//        div.setTextContent("\u00A0");
//      codeDiv.appendChild(div);
      positioning.newline();
    }
    // TODO: hitBox.width is not set
    hitBox.height = positioning.lineBottom - hitBox.y;
    toReturn.reset();
    toReturn.svgString = svgString;
    toReturn.hitBox = hitBox;
  }

  static void renderLine(TokenContainer line, TokenRendererReturn toReturn, TokenRendererPositioning positioning, CodePosition pos, int level, CodePosition currentTokenPos, Element div, boolean isStatement, TokenRenderer renderer, RenderedHitBox lineHitBox, RenderSupplementalInfo supplement, int minPaddingNesting)
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

    
//    // Mark the parent container if it only contains non-wide tokens
//    if (!hasWideTokens && isStatement)
//      div.getClassList().add("tokenline");
    
    // Actually render the line
//    DivElement subdiv = null;
//    Document doc = div.getOwnerDocument();
    toReturn.reset();
    RenderedHitBox.RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(positioning.x, positioning.lineTop + positioning.currentNestingInLine * renderer.vertPadding, 0, 0);  
    toReturn.hitBox = hitBox;
    toReturn.height = positioning.fontSize;  // Minimum height for a line
    double y = positioning.lineTop;
    int paddingNesting = positioning.maxNestingForLine - positioning.currentNestingInLine;
    if (paddingNesting < minPaddingNesting)
      paddingNesting = minPaddingNesting;
    int totalVertPadding = paddingNesting * renderer.vertPadding;
//     "' y='" + (y + positioning.currentNestingInLine * vertPadding) 
//        + "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) 
    toReturn.height = renderer.textHeight + renderer.descenderHeight + totalVertPadding * 2;
    int tokenno = 0;
    TokenRendererReturn returnedRenderedToken = new TokenRendererReturn();
    for (Token tok: line.tokens)
    {
      currentTokenPos.setOffset(level, tokenno);
      tok.visit(renderer, returnedRenderedToken, positioning, level + 1, currentTokenPos, null);
      if (returnedRenderedToken.svgString != null && !returnedRenderedToken.svgString.isEmpty())
      {
        if (!returnedRenderedToken.svgString.endsWith("\n"))
          toReturn.svgString += returnedRenderedToken.svgString + "\n";
        else
          toReturn.svgString += returnedRenderedToken.svgString;
      }
      toReturn.height = Math.max(toReturn.height, returnedRenderedToken.height);
      hitBox.children.add(returnedRenderedToken.hitBox);
      currentTokenPos.setMaxOffset(level + 1);
//      Element el = returnedRenderedToken.el;
//      if (supplement.renderTypeFieldStyle && pos != null && !pos.hasOffset(level + 1))
//        el.getClassList().add("typeTokenSelected");
//      // Put non-wide tokens in a div line
//      if (hasWideTokens && !tok.isWide())
//      {
//        if (subdiv == null)
//        {
//          subdiv = doc.createDivElement();
//          subdiv.getClassList().add("tokenline");
//          div.appendChild(subdiv);
//        }
//        subdiv.appendChild(doc.createTextNode("\u200B"));  // Need a zero-width space afterwards so that the line will wrap between tokens
//        subdiv.appendChild(el);
//      }
//      else
//      {
//        // No zero-width space between wide tokens
//        div.appendChild(el);
//        subdiv = null;
//      }
//      if (pos != null && !pos.hasOffset(level + 1) && tokenno == pos.getOffset(level))
//      {
//        DivElement toInsert = doc.createDivElement();
//        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
//        Element beforePoint = returnedRenderedToken.beforeInsertionPoint;
//        beforePoint.getParentElement().insertBefore(toInsert.querySelector("div"), beforePoint);
//      }
      tokenno++;
    }

    hitBox.height = toReturn.height;

    // If the last token is a wide token, there should be an empty line afterwards
    // where additional content can go
    boolean needEmptyLineAtEnd = false;
    if (!line.tokens.isEmpty() && line.endsWithWideToken())
      needEmptyLineAtEnd = true;
    if (needEmptyLineAtEnd)
    {
      hitBox.children.add(RenderedHitBox.forRectangle(
          positioning.lineStart, positioning.lineTop, 
          0, Math.max(positioning.lineBottom - positioning.lineTop, toReturn.height)));
//      subdiv = doc.createDivElement();
//      div.appendChild(subdiv);
//      if (lineHitBox != null)
//        lineHitBox.children.add(new RenderedHitBox(subdiv));
    }
//    // Special handling for cursor at the end, or if line is empty with no cursor, put some blank content there
//    if (pos != null && !pos.hasOffset(level + 1) && pos.getOffset(level) == line.tokens.size()) 
//    {
//      DivElement toInsert = doc.createDivElement();
//      toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
//      if (!line.tokens.isEmpty())
//      {
//        if (!needEmptyLineAtEnd)
//        {
//          if (subdiv == null)
//            div.appendChild(toInsert.querySelector("div"));
//          else
//            subdiv.appendChild(toInsert.querySelector("div"));
//        }
//        else
//          subdiv.appendChild(toInsert.querySelector("div"));
//      }
//      else
//        div.appendChild(toInsert.querySelector("div"));
//    }
//    else if (line.tokens.isEmpty())
//        div.setTextContent("\u00A0");
//    else if (needEmptyLineAtEnd)
//      subdiv.setTextContent("\u00a0");
  }

}
