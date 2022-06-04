package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.programmingbasics.plom.core.view.RenderedHitBox.RectangleRenderedHitBox;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Unit;
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
    CodePosition activeHighlightPos;
  }
  
//  static SVGSVGElement testSvgEl;
//  static SVGSVGElement testSvgCursorOverlay;
//  static SVGDocument testDoc;
//  static SvgCodeRenderer.TextWidthCalculator testWidthCalculator;
//  public static DivElement testDiv;
//  public static RenderedHitBox testHitBox;
//  public static double testClientWidth;
//  public static void test()
//  {
//    SVGDocument doc = (SVGDocument)Browser.getDocument();
//    DivElement newDiv = doc.createDivElement();
//    newDiv.setClassName("codesidesplit");
//    doc.getBody().appendChild(newDiv);
//    newDiv.setInnerHTML("<svg style=\"width: 500px; height: 1000px;\"></svg><svg style=\"width: 500px; height: 1000px;\" class=\"cursoroverlay\">" + 
//        "<g class=\"cursorscrolltransform\">" + 
//        "<circle class=\"cursorhandle\" cx=\"0\" cy=\"0\" r=\"20\"/>" + 
//        "<path class=\"cursorhandle\" d=\"M-20,-20 L 20, 0 L -20 20 z\"/>" + 
//        "<line class=\"cursorcaret\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"0\"/>" + 
//        "</g>" + 
//        "</svg>");
//    SVGSVGElement svgEl = (SVGSVGElement)newDiv.querySelectorAll("svg").item(0);
////    svgEl.getStyle().setWidth("500px");
////    svgEl.getStyle().setHeight("1000px");
////    doc.getBody().appendChild(svgEl);
//    testSvgEl = svgEl;
//    testDoc = doc;
//    testSvgCursorOverlay = (SVGSVGElement)newDiv.querySelectorAll("svg").item(1);
//    testWidthCalculator = new SvgTextWidthCalculator(doc);
//    testDiv = newDiv;
//    testClientWidth = newDiv.getClientWidth();
//    
//    StatementContainer codeList = new StatementContainer(
//        new TokenContainer(
//            new Token.WideToken("// Comment", Symbol.DUMMY_COMMENT),
//            Token.ParameterToken.fromContents("@Type", Symbol.AtType),
//            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable,
//                new TokenContainer()),
//            Token.ParameterToken.fromContents(".a:b:c:", Symbol.DotVariable,
//                new TokenContainer(
//                    Token.ParameterToken.fromContents(".d:", Symbol.DotVariable, 
//                        new TokenContainer(new Token.SimpleToken("12", Symbol.Number)))),
//                new TokenContainer(),
//                new TokenContainer(new Token.SimpleToken("32", Symbol.Number))
//                ),
//            new Token.SimpleToken("+", Symbol.Plus),
//            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
//            ),
//        new TokenContainer(
//            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
//                new TokenContainer(
//                    new Token.SimpleToken("true", Symbol.TrueLiteral),
//                    Token.ParameterToken.fromContents(".and:", Symbol.DotVariable, 
//                        new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)))), 
//                new StatementContainer(
//                    new TokenContainer(new Token.SimpleToken("64", Symbol.Number)))),
//            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
//                new StatementContainer(
//                    new TokenContainer(
//                        new Token.OneExpressionOneBlockToken("while", Symbol.COMPOUND_WHILE, 
//                            new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)), 
//                            new StatementContainer(
//                                new TokenContainer(
//                                    new Token.SimpleToken("3", Symbol.Number)
//                                    )))))),
//            new Token.SimpleToken("55", Symbol.Number)
//            )
//        );
//    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
//    supplementalInfo.codeErrors = new ErrorList();
//    supplementalInfo.nesting = new CodeNestingCounter();
//    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(testClientWidth);
//    SvgCodeRenderer.TextWidthCalculator widthCalculator = testWidthCalculator;
//    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
//    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
//    RenderedHitBox hitBox = new RenderedHitBox();
//    CodePosition currentTokenPos = new CodePosition();
//    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
////    SvgCodeRenderer.renderLine(line, returned, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplementalInfo);
////    tok.visit(tokenRenderer, returned, positioning, 0, currentTokenPos, hitBox);
//    
//    svgEl.setInnerHTML(returned.svgString);
//  }
//  
//  public static void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, CodePosition selectionPos1, CodePosition selectionPos2, RenderedHitBox renderedHitBoxes, ErrorList codeErrors)
//  {
//    RenderSupplementalInfo supplement = new RenderSupplementalInfo();
//    supplement.codeErrors = codeErrors;
//    supplement.nesting = new CodeNestingCounter();
//    supplement.nesting.calculateNestingForStatements(codeList);
//    supplement.selectionStart = selectionPos1;
//    supplement.selectionEnd = selectionPos2;
//    CodeRenderer.renderStatementContainer(codeDiv, codeList, pos, 0, new CodePosition(), renderedHitBoxes, supplement);
//    
//    if (testSvgEl != null)
//    {
//      SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
//      supplementalInfo.codeErrors = new ErrorList();
//      supplementalInfo.nesting = new CodeNestingCounter();
//      supplementalInfo.selectionStart = selectionPos1;
//      supplementalInfo.selectionEnd = selectionPos2;
//      SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(testClientWidth);
//      SvgCodeRenderer.TextWidthCalculator widthCalculator = testWidthCalculator;
//      SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
//      SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
//      CodePosition currentTokenPos = new CodePosition();
//      SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
////      SvgCodeRenderer.renderLine(line, returned, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplementalInfo);
////      tok.visit(tokenRenderer, returned, positioning, 0, currentTokenPos, hitBox);
//      
//      testSvgEl.setInnerHTML(returned.svgString);
//      RenderedHitBox hitBox = returned.hitBox;
//      
//      
//      if (pos != null)
//      {
//        CursorRect cursorRect = RenderedCursorPosition.inStatements(codeList, pos, 0, hitBox);
//        // Draw caret for the secondary cursor
//        Element caretCursor = testSvgCursorOverlay.querySelector(".cursorcaret"); 
//        if (cursorRect != null)
//        {
//          caretCursor.getStyle().clearDisplay();
//          caretCursor.setAttribute("x1", "" + cursorRect.left);
//          caretCursor.setAttribute("x2", "" + cursorRect.left);
//          caretCursor.setAttribute("y1", "" + cursorRect.top);
//          caretCursor.setAttribute("y2", "" + cursorRect.bottom);
//        }
//      }
//      testHitBox = hitBox;
//    }
//  }

  // We can't define svg gradients in css, so we need to define them in a separate defs section.
  // If you have multiple code views, the same gradient will be defined more than once, but since
  // it's always the same, it should be okay.
  static final String GRADIENT_DEFS = "<defs>\n" + 
      "<linearGradient id=\"plomMultilineAccent\" gradientTransform=\"rotate(90)\">\n" + 
      "<stop offset=\"0%\" stop-color=\"black\" stop-opacity=\"0.15\"/>\n" + 
      "<stop offset=\"1000%\" stop-color=\"black\" stop-opacity=\"0\"/>\n" + 
      "</linearGradient>\n" + 
      "</defs>\n";
  
  public static RenderedHitBox renderSvgWithHitBoxes(SVGSVGElement svgEl, StatementContainer codeList, CodePosition activeHighlightPos, CodePosition selectionPos1, CodePosition selectionPos2, ErrorList codeErrors, SvgCodeRenderer.TextWidthCalculator widthCalculator, double clientWidth, double leftPadding, double topPadding, double rightPadding, double bottomPadding)
  {
//    RenderedHitBox renderedHitBoxes = RenderedHitBox.withChildren();
//    render(codeDiv, codeList, pos, selectionPos1, selectionPos2, renderedHitBoxes, codeErrors);
    
    final double extraWidth = 0.5; // Slightly larger to accommodate width of lines     
    
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = codeErrors;
    supplementalInfo.nesting = new CodeNestingCounter();
    supplementalInfo.activeHighlightPos = activeHighlightPos;
    supplementalInfo.selectionStart = selectionPos1;
    supplementalInfo.selectionEnd = selectionPos2;
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(clientWidth - extraWidth - leftPadding - rightPadding);
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
    positioning.wrapLineStart = positioning.lineStart + tokenRenderer.WRAP_INDENT;
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    
//    svgEl.setInnerHTML(returned.svgString);
    svgEl.setInnerHTML(GRADIENT_DEFS + "<g transform=\"translate("   + leftPadding + ", " + topPadding + ")\">" + returned.svgString + "</g>");
    svgEl.getStyle().setHeight(returned.height + topPadding + bottomPadding, Unit.PX);
    svgEl.getStyle().setWidth(returned.width + extraWidth + leftPadding + rightPadding, Unit.PX);
    RenderedHitBox hitBox = returned.hitBox;
    return hitBox;
  }

  // Special renderer for expressions or other pieces of code that are only a single line
  public static RenderedHitBox renderSvgSingleLineWithHitBoxes(SVGSVGElement svgEl, StatementContainer codeList, CodePosition activeHighlightPos, CodePosition selectionPos1, CodePosition selectionPos2, ErrorList codeErrors, SvgCodeRenderer.TextWidthCalculator widthCalculator, double clientWidth, double leftPadding, double topPadding, double rightPadding, double bottomPadding)
  {
    // Check for an empty line, in which case, we'll render an empty box that can be filled in
    if (codeList == null || codeList.statements.isEmpty() || codeList.statements.get(0).tokens.isEmpty())
    {
      final double extraWidth = 0.5; // Slightly larger to accommodate width of lines     
      
      SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
      supplementalInfo.codeErrors = codeErrors;
      supplementalInfo.nesting = new CodeNestingCounter();
      supplementalInfo.activeHighlightPos = activeHighlightPos;
      supplementalInfo.selectionStart = selectionPos1;
      supplementalInfo.selectionEnd = selectionPos2;
      SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(clientWidth - extraWidth - leftPadding - rightPadding);
      SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
      positioning.wrapLineStart = positioning.lineStart + tokenRenderer.WRAP_INDENT;
      SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
      CodePosition currentTokenPos = new CodePosition();
      tokenRenderer.renderEmptyFillIn(returned, positioning, 1, currentTokenPos, tokenRenderer, supplementalInfo, 0);
//      void renderEmptyFillIn(TokenRendererReturn toReturn, TokenRendererPositioning positioning, int level, CodePosition currentTokenPos, TokenRenderer renderer, RenderSupplementalInfo supplement, int minPaddingNesting)
//      SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
      
//      svgEl.setInnerHTML(returned.svgString);
      svgEl.setInnerHTML(GRADIENT_DEFS + "<g transform=\"translate("   + leftPadding + ", " + topPadding + ")\">" + returned.svgString + "</g>");
      svgEl.getStyle().setHeight(returned.height + topPadding + bottomPadding, Unit.PX);
      svgEl.getStyle().setWidth(returned.width + extraWidth + leftPadding + rightPadding, Unit.PX);
      RenderedHitBox hitBox = returned.hitBox;
      return hitBox;
      
    }
    else
    {
      return renderSvgWithHitBoxes(svgEl, codeList, activeHighlightPos, selectionPos1, selectionPos2, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
    }
  }
  
  public static RenderedHitBox renderTypeToken(DivElement div, Token type, CodePosition pos, SvgCodeRenderer.TextWidthCalculator widthCalculator, double clientWidth)
  {
    final double leftPadding = 0;
    final double rightPadding = 0;
    final double topPadding = 0;
    final double bottomPadding = 0;

    final double extraWidth = 0.5; // Slightly larger to accommodate width of lines     
    
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    supplementalInfo.nesting.calculateNestingForStatements(type == null ? new StatementContainer(new TokenContainer()) : new StatementContainer(new TokenContainer(type)));
    supplementalInfo.renderTypeFieldStyle = true;
    supplementalInfo.activeHighlightPos = pos;
    supplementalInfo.selectionStart = null;
    supplementalInfo.selectionEnd = null;
    
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(clientWidth - extraWidth - leftPadding - rightPadding);
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
    positioning.wrapLineStart = positioning.lineStart + tokenRenderer.WRAP_INDENT;
    if (type != null)
    {
//      RenderedHitBox hitBox = new RenderedHitBox(null);
//      CodeRenderer.TokenRendererReturn returnedRenderedToken = new CodeRenderer.TokenRendererReturn();
      SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
      CodePosition currentTokenPos = new CodePosition();
      currentTokenPos.setOffset(0, 0);
//      type.visit(tokenRenderer, returned, positioning, 1, currentTokenPos, null);

//      SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);

      TokenContainer dummyLine = new TokenContainer(Arrays.asList(type));
      supplementalInfo.nesting.calculateNestingForLine(dummyLine);
      positioning.maxNestingForLine = supplementalInfo.nesting.expressionNesting.get(dummyLine);
      positioning.currentNestingInLine = 0;
      SvgCodeRenderer.renderLine(dummyLine, returned, positioning, 0, currentTokenPos, false, tokenRenderer, supplementalInfo, 1, false);
//      svgString += toReturn.svgString;
//      hitBox.children.add(toReturn.hitBox);

      
      
      currentTokenPos.setMaxOffset(1);
//      Element el = returnedRenderedToken.el;
      div.setInnerHTML("<svg></svg>");
      SVGSVGElement svgEl = (SVGSVGElement)div.querySelector("svg");
      svgEl.setInnerHTML(GRADIENT_DEFS + "<g transform=\"translate("   + leftPadding + ", " + topPadding + ")\">" + returned.svgString + "</g>");
      svgEl.getStyle().setHeight(returned.height + topPadding + bottomPadding, Unit.PX);
      svgEl.getStyle().setWidth(returned.width + extraWidth + leftPadding + rightPadding, Unit.PX);
//      div.appendChild(el);
//      if (pos != null && !pos.hasOffset(1))
//        el.getClassList().add("typeTokenSelected");
      return returned.hitBox.children.get(0);

    }
    else
    {
//      if (pos != null)
//      {
//        DivElement toInsert = Browser.getDocument().createDivElement();
//        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
//        div.appendChild(toInsert.querySelector("div"));
//      }
//      else
        div.setTextContent("");
      return null;
      
    }
    
//    svgEl.setInnerHTML(returned.svgString);
//    svgEl.setInnerHTML(GRADIENT_DEFS + "<g transform=\"translate("   + leftPadding + ", " + topPadding + ")\">" + returned.svgString + "</g>");
//    svgEl.getStyle().setHeight(returned.height + topPadding + bottomPadding, Unit.PX);
//    svgEl.getStyle().setWidth(returned.width + extraWidth + leftPadding + rightPadding, Unit.PX);
//    RenderedHitBox hitBox = returned.hitBox;
//    return hitBox;

    
    
//    RenderedHitBox hitBox = new RenderedHitBox(null);
////    RenderSupplementalInfo supplement = new RenderSupplementalInfo();
////    supplement.codeErrors = new ErrorList();
////    supplement.nesting = new CodeNestingCounter();
////    supplement.nesting.calculateNestingForStatements(type == null ? new StatementContainer(new TokenContainer()) : new StatementContainer(new TokenContainer(type)));
////    supplement.renderTypeFieldStyle = true;
//    CodeRenderer.TokenRenderer renderer = new CodeRenderer.TokenRenderer(Browser.getDocument(), supplement);
//    if (type != null)
//    {
//    }
//    else
//    {
//      if (pos != null)
//      {
//        DivElement toInsert = Browser.getDocument().createDivElement();
//        toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
//        div.appendChild(toInsert.querySelector("div"));
//      }
//      else
//        div.setTextContent("\u00A0");
//      return null;
//    }
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
    public RenderedHitBox.RectangleRenderedHitBox hitBox;
    public boolean wraps = false;
    public void reset()
    {
      svgString = "";
      width = 0;
      height = 0;
      hitBox = null;
      wraps = false;
    }
  }
  
  static class TokenRendererPositioning
  {
    double lineStart = 0;
    double lineEnd = 100;
    double lineTop = 0;
    double lineBottom = 0;
    double canvasWidth = 0;
    double wrapLineStart = 0;
    double minWidth;  
    double fontSize = 15;
    double x = 0;
    double spaceBetweenLines = 0;
    int maxNestingForLine = 0;
    int currentNestingInLine = 0;
    boolean showNestingAccent = false;
    boolean showMultilineAccent = true;
    boolean showWideExpresionAccent = false;
    
    TokenRendererPositioning(double width)
    {
      this.canvasWidth = width;
      this.minWidth = canvasWidth / 2;
      this.lineEnd = this.canvasWidth;
    }
    void maxBottom(double lineHeight)
    {
      lineBottom = Math.max(lineBottom, lineTop + lineHeight);
    }
    void newline()
    {
      x = lineStart;
      lineTop = lineBottom;
    }
    void newlineWithSpace()
    {
      newline();
      addSpaceBetweenLines();
    }
    void addSpaceBetweenLines()
    {
      lineTop = lineBottom = lineTop + spaceBetweenLines;
    }
//    int getTextHeight()
//    {
//      return (int)Math.ceil(fontSize);
//    }
//    int getDescenderHeight()
//    {
//      return (int)Math.ceil(getTextHeight() * 0.2);
//    }
    
    void copyFrom(TokenRendererPositioning from)
    {
      lineStart = from.lineStart;
      lineEnd = from.lineEnd;
      lineTop = from.lineTop;
      lineBottom = from.lineBottom;
      fontSize = from.fontSize;
      minWidth = from.minWidth;
      wrapLineStart = from.wrapLineStart;
      x = from.x;
      maxNestingForLine = from.maxNestingForLine;
      currentNestingInLine = from.currentNestingInLine;
      showNestingAccent = from.showNestingAccent;
      showMultilineAccent = from.showMultilineAccent;
      showWideExpresionAccent = from.showWideExpresionAccent;
      spaceBetweenLines = from.spaceBetweenLines;
    }
    
    TokenRendererPositioning copy()
    {
      TokenRendererPositioning copy = new TokenRendererPositioning(canvasWidth);
      copy.copyFrom(this);
      return copy;
    }
  }
  
  public static interface TextWidthCalculator
  {
    public double calculateWidth(String text);

    public default List<String> breakLines(String contents, double maxWidth)
    {
      List<String> toReturn = new ArrayList<>();
      RegExp beforeWhitespace = RegExp.compile("^\\s*\\S*");
      RegExp skipWhitespace = RegExp.compile("^\\s*(.*)");
      for (String line: contents.split("\\n", -1))
      {
        // Go through each explicit line
        MatchResult match = beforeWhitespace.exec(line);
        
        // Special case handling of empty lines
        if (line.isEmpty()) toReturn.add("");
        while (!line.isEmpty()) 
        {
          String truncatedLine = "";
          // Keep removing words and adding it until we exceed the max width
          while (calculateWidth(truncatedLine + match.getGroup(0)) < maxWidth && !line.isEmpty())
          {
            truncatedLine += match.getGroup(0);
            line = line.substring(match.getGroup(0).length());
            match = beforeWhitespace.exec(line);
          }
          // If the first word is longer than maxWidth
          if (truncatedLine.isEmpty()) 
          {
            // We'll just arbitrarily split the word at maxWidth (useful for
            // languages where there might not be any spaces, but not optimal
            // for English)
            char[] codePointChars = new char[2];
            
            // Add at least one character, so that don't get stuck in an infinite loop
            int numChars = Character.toChars(line.codePointAt(0), codePointChars, 0);
            truncatedLine = truncatedLine + String.valueOf(codePointChars, 0, numChars);
            line = line.substring(numChars);
            
            // Advance one code point at a time (ideally, should work at the grapheme level) 
            numChars = Character.toChars(line.codePointAt(0), codePointChars, 0);
            while(calculateWidth(truncatedLine + String.valueOf(codePointChars, 0, numChars)) < maxWidth && !line.isEmpty())
            {
              truncatedLine = truncatedLine + String.valueOf(codePointChars, 0, numChars);
              line = line.substring(numChars);
              numChars = Character.toChars(line.codePointAt(0), codePointChars, 0);
            }
          }
          toReturn.add(truncatedLine);
          line = skipWhitespace.exec(line).getGroup(1); 
          match = beforeWhitespace.exec(line);
        }
      }
      return toReturn;
    }
  }
  
  public static class SvgTextWidthCalculator implements TextWidthCalculator
  {
    SVGDocument doc;
    SVGTextElement textEl;
    public SvgTextWidthCalculator(SVGDocument doc)
    {
      // See if there's an existing width calculator element
      // that we can reuse
      textEl = (SVGTextElement) doc.querySelector("svg.widthCalculator text.codetoken");
      if (textEl != null) return;
      // If not, create a new svg element that we can use for computing widths
      SVGSVGElement svgEl = doc.createSVGElement();
      textEl = doc.createSVGTextElement();
      textEl.getClassList().add("codetoken");
      svgEl.appendChild(textEl);
      svgEl.getClassList().add("widthCalculator");
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
    final int horzEndParamPadding = 12;
    final int vertPadding = 3;
    final int descenderHeight;
    final TextWidthCalculator widthCalculator;
    final double INDENT_SIZE;
    final double WRAP_INDENT;
    public double minLineHeight() { return textHeight + descenderHeight + 2 * vertPadding; }
    TokenRenderer(Document doc, RenderSupplementalInfo supplement, int textHeight, TextWidthCalculator widthCalculator)
    {
      this.doc = doc;
      this.supplement = supplement;
      this.textHeight = textHeight;
      this.descenderHeight = (int)Math.ceil(textHeight * 0.2);
      this.widthCalculator = widthCalculator;
      this.INDENT_SIZE = 2 * horzPadding + Math.max(widthCalculator.calculateWidth("}"), widthCalculator.calculateWidth("{"));
      this.WRAP_INDENT = 2 * INDENT_SIZE;
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
    public Void visitSimpleToken(SimpleToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox notUsed)
    {
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      if (supplement.activeHighlightPos != null && currentTokenPos.equalUpToLevel(supplement.activeHighlightPos, level))
        classList += supplement.renderTypeFieldStyle ? " typeTokenSelected" : " tokenactive";
      String text = token.contents;
      double textWidth = widthCalculator.calculateWidth(text);
      toReturn.reset();
      double x = positioning.x;
      double y = positioning.lineTop;
      int totalVertPadding = (positioning.maxNestingForLine - positioning.currentNestingInLine) * vertPadding;
      toReturn.svgString = "<rect x='" + x + "' y='" + (y + positioning.currentNestingInLine * vertPadding) + "' width='" + (textWidth + horzPadding * 2) + "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>"
          + "<text x='" + (x + horzPadding) + "' y='" + (y + textHeight + positioning.maxNestingForLine * vertPadding) + "' class='" + classList + "'>" + text + "</text>";
      if (positioning.showNestingAccent)
        toReturn.svgString += "<path d=\"M" + x + " " + (y + positioning.currentNestingInLine * vertPadding + textHeight + descenderHeight + totalVertPadding * 2) + " l" + (textWidth + horzPadding * 2) + " 0\" class=\"tokenslot\"/>"; 
      else if (positioning.showWideExpresionAccent)
        toReturn.svgString += "<path d=\"M" + x + " " + (y + positioning.currentNestingInLine * vertPadding + textHeight + descenderHeight + totalVertPadding * 2) + " l" + (textWidth + horzPadding * 2) + " 0\" class=\"wideexpressionslot\"/>"; 
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
    public Void visitParameterToken(ParameterToken token, TokenRendererReturn toReturn, TokenRendererPositioning externalPositioning, Integer level, CodePosition currentTokenPos, RenderedHitBox notUsed)
    {
      // We render inside the parameter token with a separate context than what's used for the rest of the line
      TokenRendererPositioning positioning = externalPositioning.copy();
      positioning.lineTop += (positioning.currentNestingInLine) * vertPadding;
      positioning.maxNestingForLine -= positioning.currentNestingInLine;
      positioning.currentNestingInLine -= positioning.currentNestingInLine;
      positioning.lineBottom = positioning.lineTop;
      positioning.lineEnd -= horzPadding;
      double startX = positioning.x;
      double startY = positioning.lineTop;
      positioning.x += horzPadding;
      positioning.lineStart = positioning.x;
      positioning.wrapLineStart = positioning.lineStart + WRAP_INDENT;
      // Use a nesting level of -1 since we're already inside the token now
      positioning.currentNestingInLine--;
      positioning.maxNestingForLine--;
      
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      if (supplement.activeHighlightPos != null && currentTokenPos.equalUpToLevel(supplement.activeHighlightPos, level))
        classList += supplement.renderTypeFieldStyle ? " typeTokenSelected" : " tokenactive";
      toReturn.reset();

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
      boolean isFirstParameterNameOnLine = true;
      double maxX = positioning.x;
      for (int n = 0; n < token.contents.size(); n++)
      {
        boolean isParameterExpressionOnNewLine = false;
        double startParamX = positioning.x;
        TokenRendererPositioning subpositioning = positioning.copy();
        TokenRendererPositioning paramNamePositioning = positioning.copy();

        // Add some extra space at the end to make it easier to put
        // the cursor at the end of the last parameter
        boolean addSpaceToEnd = (n == token.contents.size() - 1)
            && (token.postfix == null || token.postfix.isEmpty());
        
        // Try rendering three different ways
        
        // First placing the parameter to the end of the line
        if (!isFirstParameterNameOnLine)
        {
          paramNamePositioning.copyFrom(positioning);
          layoutParameterTokenParameter(token, level, currentTokenPos, classList, returned, isFirstParameterNameOnLine, n,
              paramNamePositioning, subpositioning, isParameterExpressionOnNewLine, addSpaceToEnd);
          if (returned.wraps)
          {
            // Move the parameter name to the start of the next line
            positioning.newline();
            toReturn.wraps = true;
            startParamX = positioning.x;
            isFirstParameterNameOnLine = true;
          }
        }

        // Then try placing the parameter at the start of a new line
        if (isFirstParameterNameOnLine)
        {
          paramNamePositioning.copyFrom(positioning);
          layoutParameterTokenParameter(token, level, currentTokenPos, classList, returned, isFirstParameterNameOnLine, n,
              paramNamePositioning, subpositioning, isParameterExpressionOnNewLine, addSpaceToEnd);
          if (returned.wraps)
          {
            // It's too wide to put the parameter name and expression on the same line
            isParameterExpressionOnNewLine = true;
          }
        }
        
        // Put the parameter name on one line and the parameter expression on another line
        if (isFirstParameterNameOnLine && isParameterExpressionOnNewLine)
        {
          paramNamePositioning.copyFrom(positioning);
          layoutParameterTokenParameter(token, level, currentTokenPos, classList, returned, isFirstParameterNameOnLine, n,
              paramNamePositioning, subpositioning, isParameterExpressionOnNewLine, addSpaceToEnd);
        }

        maxX = Math.max(maxX, subpositioning.x);
        maxX = Math.max(maxX, startParamX + returned.width);
        
        // Do any wrapping needed for the next line
        if (returned.wraps)
        {
          subpositioning.newline();
          isFirstParameterNameOnLine = true;
        }
        else
        {
          isFirstParameterNameOnLine = false;
        }
        
        // Now that we've worked out where everything goes, we can actually
        // commit and add the finalized layouts for the parameters
        positioning.copyFrom(subpositioning);
        paramsSvg += returned.svgString;
        exprHitBoxes.children.add(returned.hitBox);
        if (returned.wraps || isParameterExpressionOnNewLine)
          toReturn.wraps = true;
      }
      // Handle any postfix for the token
//      SpanElement endSpan = doc.createSpanElement();
      if (token.postfix != null && !token.postfix.isEmpty())
      {
        if (isFirstParameterNameOnLine)
          positioning.lineTop += vertPadding;
        tokenText += layoutParameterTokenParameterName(token.postfix, positioning, isFirstParameterNameOnLine,
            classList);
        isFirstParameterNameOnLine = false;
      }
//      span.appendChild(endSpan);
//      if (hitBox != null)
//      {
//        RenderedHitBox endHitBox = new RenderedHitBox(endSpan);
//        textHitBoxes.children.add(endHitBox);
//      }
//
//      toReturn.el = span;
//      toReturn.beforeInsertionPoint = span;
      
      // It looks strange when we place things to the right of a multi-line
      // parameter token, so we'll just have a multi-line parameter token take
      // the full width of the screen so nothing fits on the right
      if (toReturn.wraps && maxX < positioning.lineEnd)
        maxX = positioning.lineEnd;
      
      // Assemble the final box for the token
      maxX = Math.max(maxX, positioning.x);
      double rectTopY = startY;
      double height = positioning.lineBottom - rectTopY + vertPadding;
      toReturn.svgString = "<rect x='" + startX + "' y='" + (rectTopY) + "' width='" + (maxX - startX + horzPadding) + "' height='" + (height) + "' class='" + classList + "'/>"
          + tokenText + "\n";
      if (positioning.showNestingAccent)
          toReturn.svgString += "<path d=\"M" + startX + " " + (rectTopY + height) + " l" + (maxX - startX + horzPadding) + " 0\" class=\"tokenslot\"/>\n"; 
      else if (positioning.showWideExpresionAccent)
        toReturn.svgString += "<path d=\"M" + startX + " " + (rectTopY + height) + " l" + (maxX - startX + horzPadding) + " 0\" class=\"wideexpressionslot\"/>\n"; 
      if (toReturn.wraps && positioning.showMultilineAccent)
        toReturn.svgString += "<rect x='" + startX + "' y='" + rectTopY + "' width='2' height='" + (height) + "' class=\"plomMultilineAccent\"/>\n";
      toReturn.svgString += paramsSvg;
      toReturn.width = maxX + horzPadding - startX;
      toReturn.height = height;
      toReturn.hitBox = RenderedHitBox.forRectangleWithChildren(startX, startY, toReturn.width, toReturn.height);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.set(SvgCodeRenderer.PARAMTOK_POS_EXPRS, exprHitBoxes);
      externalPositioning.x = startX + toReturn.width; 
      externalPositioning.lineBottom = Math.max(positioning.lineBottom, externalPositioning.lineBottom);
      return null;
    }
    private void layoutParameterTokenParameter(ParameterToken token, Integer level, CodePosition currentTokenPos,
        String classList, TokenRendererReturn returned, boolean isFirstParameterNameOnLine, int paramIdx,
        TokenRendererPositioning paramNamePositioning, TokenRendererPositioning subpositioning, boolean isParameterExpressionOnNewLine,
        boolean addSpaceToEnd) {
      
      // Layout the name part of the parameter
      double startNameX = paramNamePositioning.x;
      if (isFirstParameterNameOnLine)
        paramNamePositioning.lineTop += vertPadding;
      String nameText = layoutParameterTokenParameterName(token.contents.get(paramIdx), paramNamePositioning, isFirstParameterNameOnLine,
          classList) + "\n";
      paramNamePositioning.x += horzPadding;
      double nameMaxX = paramNamePositioning.x;
      
      // Start a newline between name and expression of parameter if expected
      subpositioning.copyFrom(paramNamePositioning);
      if (isParameterExpressionOnNewLine)
      {
        subpositioning.newline();
        subpositioning.x = subpositioning.wrapLineStart;
      }
        
      // Layout the expression part of the parameter
      double startExprX = subpositioning.x;
      layoutParameterTokenParameterExpression(token.parameters.get(paramIdx), subpositioning, currentTokenPos, level, paramIdx, addSpaceToEnd, returned);
      returned.svgString = nameText + returned.svgString;
      
      // Set a width for the parameter
      double maxX = Math.max(subpositioning.x, nameMaxX);
      maxX = Math.max(maxX, returned.width + startExprX);
      returned.width = maxX - startNameX;
      
      // Check if we've overflown and should have wrapped
      if (subpositioning.x > subpositioning.lineEnd)
        returned.wraps = true; 
    }

    private String layoutParameterTokenParameterName(String text, 
        TokenRendererPositioning paramNamePositioning, boolean isFirstParameterNameOnLine, String classList) {
      if (!isFirstParameterNameOnLine) paramNamePositioning.x += horzPadding;
      paramNamePositioning.maxBottom(textHeight + descenderHeight + (paramNamePositioning.maxNestingForLine * 2) * vertPadding);
      String nameText = "<text x='" + (paramNamePositioning.x) + "' y='" + (paramNamePositioning.lineTop + textHeight + paramNamePositioning.maxNestingForLine * vertPadding) + "' class='" + classList + "'>" + text + "</text>";
      paramNamePositioning.x += widthCalculator.calculateWidth(text);
      return nameText;
    }

    private void layoutParameterTokenParameterExpression(TokenContainer line, TokenRendererPositioning subpositioning,
        CodePosition currentTokenPos, Integer level, int paramIdx, boolean addSpaceToEnd, TokenRendererReturn returned) {
      currentTokenPos.setOffset(level, PARAMTOK_POS_EXPRS);
      currentTokenPos.setOffset(level + 1, paramIdx);
      boolean oldNestingAccent = subpositioning.showNestingAccent;
      subpositioning.showNestingAccent = true;
      subpositioning.currentNestingInLine++;
//        subpositioning.lineBottom = subpositioning.lineTop;
      if (!line.tokens.isEmpty())
         renderLine(line, returned, subpositioning, level + 2, currentTokenPos, false, this, supplement, 0, addSpaceToEnd);
      else
         renderEmptyFillIn(returned, subpositioning, level + 2, currentTokenPos, this, supplement, 0);
      subpositioning.currentNestingInLine--;
      subpositioning.showNestingAccent = oldNestingAccent;
      currentTokenPos.setMaxOffset(level + 1);
    }

    @Override
    public Void visitWideToken(WideToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox notUsed)
    {
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      if (supplement.activeHighlightPos != null && currentTokenPos.equalUpToLevel(supplement.activeHighlightPos, level))
        classList += supplement.renderTypeFieldStyle ? " typeTokenSelected" : " tokenactive";
      String textClassList = classList;
      if (token.type == Symbol.DUMMY_COMMENT)
        textClassList += " tokencomment";
      toReturn.reset();
      double x = positioning.x;
      double y = positioning.lineTop;
      double width = positioning.lineEnd - positioning.lineStart;
      List<String> textLines = widthCalculator.breakLines(token.contents, positioning.lineEnd - positioning.lineStart - 2 * horzPadding);
      toReturn.svgString = "<rect x='" + x + "' y='" + y + "' width='" + (width)+ "' height='" + (textLines.size() * (textHeight + descenderHeight) + vertPadding * 2) + "' class='" + classList + "'/>";
      double lineY = y;
      for (String text: textLines)
      {
        toReturn.svgString += "<text x='" + (x + horzPadding) + "' y='" + (lineY + textHeight + vertPadding) + "' class='" + textClassList + "'>" + text + "</text>";
        lineY += textHeight + descenderHeight;
      }
      toReturn.width = width;
      toReturn.height = textLines.size() * (textHeight + descenderHeight) + vertPadding * 2;
      toReturn.hitBox = RenderedHitBox.forRectangleWithChildren(x, y, toReturn.width, toReturn.height);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      toReturn.hitBox.children.add(null);
      positioning.maxBottom(toReturn.height);
      positioning.newlineWithSpace();
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
        TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox notUsed)
    {
      createWideToken(token, token.contents, null, token.block, toReturn, positioning, level, currentTokenPos);
      return null;
    }
    @Override
    public Void visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, TokenRendererReturn toReturn, TokenRendererPositioning positioning, Integer level, CodePosition currentTokenPos, RenderedHitBox notUsed)
    {
      createWideToken(token, token.contents, token.expression, token.block, toReturn, positioning, level, currentTokenPos);
      return null;
    }
    private void createWideToken(Token token, String tokenText, TokenContainer exprContainer,
        StatementContainer blockContainer,
        TokenRendererReturn toReturn, TokenRendererPositioning positioning, int level, CodePosition currentTokenPos)
    {
      final double INDENT = INDENT_SIZE;
      String classList = "codetoken";
      if (supplement.codeErrors.containsToken(token))
        classList += " tokenError";
      if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
        classList += " tokenselected";
      if (supplement.activeHighlightPos != null && currentTokenPos.equalUpToLevel(supplement.activeHighlightPos, level))
        classList += supplement.renderTypeFieldStyle ? " typeTokenSelected" : " tokenactive";

      final double startX = positioning.x;
      final double startY = positioning.lineTop;
      positioning.x += horzPadding;
      String wideSvg = "";
      
      String text = tokenText;
      double textWidth = widthCalculator.calculateWidth(text);
      positioning.x += textWidth;
      int maxNesting = 1;
      int totalVertPadding = maxNesting * vertPadding;
      double xFirstLineExtent = positioning.x + horzPadding;
      double xBlockExtent = startX;
      double firstLineHeight = textHeight + descenderHeight + totalVertPadding * 2;
      double expressionHeight = firstLineHeight;
      double expressionWidth = positioning.x - startX;
      
      RenderedHitBox exprHitBox = null;
      RenderedHitBox blockHitBox = null;
      
      String startBracketSvg = "";
      if (exprContainer != null)
      {
        positioning.x += horzPadding;
        toReturn.reset();
        TokenRendererPositioning subPositioning = positioning.copy();
        subPositioning.lineTop += vertPadding;
        subPositioning.lineEnd -= horzPadding;
        subPositioning.maxNestingForLine = supplement.nesting.expressionNesting.get(exprContainer);
        subPositioning.currentNestingInLine = 0;
        boolean oldExpressionAccent = subPositioning.showNestingAccent;
        subPositioning.showWideExpresionAccent = true;
        maxNesting = subPositioning.maxNestingForLine + 1;
        totalVertPadding = maxNesting * vertPadding;
        currentTokenPos.setOffset(level, EXPRBLOCK_POS_EXPR);
        if (!exprContainer.tokens.isEmpty())
          renderLine(exprContainer, toReturn, subPositioning, level + 1, currentTokenPos, false, this, supplement, 0, false);
        else
          renderEmptyFillIn(toReturn, subPositioning, level + 1, currentTokenPos, this, supplement, 0);
        subPositioning.showWideExpresionAccent = oldExpressionAccent;
        positioning.copyFrom(subPositioning);
        currentTokenPos.setMaxOffset(level + 1);
        wideSvg += toReturn.svgString;
        xFirstLineExtent = Math.max(xFirstLineExtent, startX + toReturn.width);
//        expressionWidth = Math.max(expressionWidth, nextX - startX);
        expressionWidth = Math.max(expressionWidth, toReturn.hitBox.width + toReturn.hitBox.x - startX);
        exprHitBox = toReturn.hitBox;
        expressionHeight = Math.max(expressionHeight, (toReturn.height + 2 * vertPadding));
      }
      if (blockContainer != null)
      {
//        startBracketSvg += "<text x='" + (nextX + horzPadding) + "' y='" + (y + textHeight + totalVertPadding) + "'>{</text>";
        if (positioning.x + horzPadding + widthCalculator.calculateWidth("{") > positioning.lineEnd)
        {
          positioning.newline();
          positioning.maxBottom(textHeight + descenderHeight + 2 * totalVertPadding - 2 * vertPadding);
          positioning.x = positioning.wrapLineStart - horzPadding;
          expressionHeight += textHeight + descenderHeight + 2 * totalVertPadding - 2 * vertPadding;
        }
        double braceY = positioning.lineTop + textHeight + totalVertPadding;
        if (exprContainer != null)
          braceY -= vertPadding; 
        startBracketSvg += "<text x='" + (positioning.x + horzPadding) + "' y='" + (braceY) + "' class='" + classList + "'>{</text>";
        double nextX = positioning.x + horzPadding + widthCalculator.calculateWidth("{");
        expressionWidth = Math.max(expressionWidth, nextX - startX);
        xFirstLineExtent = Math.max(xFirstLineExtent, nextX);
      }
      positioning.lineBottom = startY + expressionHeight;
      
      String endBracketSvg = "";
      if (blockContainer != null)
      {
        positioning.newlineWithSpace();
        toReturn.reset();
//        TokenRendererPositioning subPositioning = positioning.copy();
        double oldLineStart = positioning.lineStart;
        double indentedLineStart = positioning.lineStart + INDENT;
        positioning.x = positioning.lineStart = indentedLineStart;
        positioning.wrapLineStart = positioning.lineStart + WRAP_INDENT;
        currentTokenPos.setOffset(level, EXPRBLOCK_POS_BLOCK);
        renderStatementContainer(blockContainer, toReturn, positioning, new CodePosition(), level + 1, currentTokenPos, this, null, supplement);
        currentTokenPos.setMaxOffset(level + 1);
        wideSvg += toReturn.svgString;
        positioning.lineStart = positioning.x = oldLineStart;
        positioning.wrapLineStart = positioning.lineStart + WRAP_INDENT;
        xBlockExtent = Math.max(xBlockExtent, indentedLineStart + toReturn.width);
//        positioning.maxBottom(toReturn.height);
        
        // Add space for a '}' at the end
        positioning.newline();
        double endBracketY = positioning.lineTop + vertPadding + textHeight; 
        positioning.maxBottom(minLineHeight());
        endBracketSvg = "<text x='" + (startX + horzPadding) + "' y='" + (endBracketY) + "' class='" + classList + "'>}</text>"; 
        positioning.newline();
        blockHitBox = toReturn.hitBox;
      }
      
      
      toReturn.reset();
      double width = expressionWidth + horzPadding;
      toReturn.svgString = "<path d=\'M" + startX + " " + startY + " l " + width + " 0 l 0 "+  (expressionHeight) + " l -" + (width - INDENT) + " 0 L " + (startX + INDENT) + " " + positioning.lineBottom + " L " + (startX) + " " + positioning.lineBottom + " z\' class='" + classList + "'/>"; 
//      toReturn.svgString = "<rect x='" + startX + "' y='" + y + "' width='" + (width)+ "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>";
      toReturn.svgString += "<text x='" + (startX + horzPadding) + "' y='" + (startY + textHeight + totalVertPadding) + "' class='" + classList + "'>" + text + "</text>";
      toReturn.svgString += startBracketSvg;
      toReturn.svgString += wideSvg;
      toReturn.svgString += endBracketSvg;
      toReturn.width = width;
      toReturn.width = Math.max(toReturn.width, xFirstLineExtent - startX);
      toReturn.width = Math.max(toReturn.width, xBlockExtent - startX);
      toReturn.height = firstLineHeight;
      RectangleRenderedHitBox startTokenHitBox = RenderedHitBox.forRectangle(startX, startY, toReturn.width, toReturn.height);
      RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(startX, startY, toReturn.width, positioning.lineBottom - startY);
      toReturn.hitBox = hitBox;
      hitBox.children.add(null);
      hitBox.children.add(null);
      hitBox.children.add(null);
      hitBox.children.set(EXPRBLOCK_POS_START, startTokenHitBox);
      hitBox.children.set(EXPRBLOCK_POS_EXPR, exprHitBox);
      hitBox.children.set(EXPRBLOCK_POS_BLOCK, blockHitBox);
      
      positioning.addSpaceBetweenLines();

      
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
    
    
    void renderEmptyFillIn(TokenRendererReturn toReturn, TokenRendererPositioning positioning, int level, CodePosition currentTokenPos, TokenRenderer renderer, RenderSupplementalInfo supplement, int minPaddingNesting)
    {
       toReturn.reset();
       RenderedHitBox.RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(positioning.x, positioning.lineTop + positioning.currentNestingInLine * renderer.vertPadding, 0, 0);  
       toReturn.hitBox = hitBox;

       final int FILL_IN_WIDTH = 20; 

       
      double startY = positioning.lineTop;
      int paddingNesting = positioning.maxNestingForLine - positioning.currentNestingInLine;
      if (paddingNesting < minPaddingNesting)
        paddingNesting = minPaddingNesting;
      int totalVertPadding = paddingNesting * renderer.vertPadding;
//      double tokenHeight = renderer.textHeight + renderer.descenderHeight + totalVertPadding * 2;
//      toReturn.width = 0;
//      int tokenno = 0;
//      TokenRendererReturn returnedRenderedToken = new TokenRendererReturn();
        currentTokenPos.setOffset(level, 0);
        
        String classList = "fillinblank";
//        if (supplement.codeErrors.containsToken(token))
//          classList += " tokenError";
        if (currentTokenPos.isBetweenNullable(supplement.selectionStart, supplement.selectionEnd))
          classList += " tokenselected";
        double x = positioning.x;
        double y = positioning.lineTop;
        toReturn.svgString = "<rect x='" + x + "' y='" + (y + positioning.currentNestingInLine * vertPadding) + "' width='" + (FILL_IN_WIDTH + horzPadding * 2) + "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) + "' class='" + classList + "'/>\n";
        if (positioning.showNestingAccent)
          toReturn.svgString += "<path d=\"M" + x + " " + (y + positioning.currentNestingInLine * vertPadding + textHeight + descenderHeight + totalVertPadding * 2) + " l" + (FILL_IN_WIDTH + horzPadding * 2) + " 0\" class=\"tokenslot\"/>\n"; 
        else if (positioning.showWideExpresionAccent)
          toReturn.svgString += "<path d=\"M" + x + " " + (y + positioning.currentNestingInLine * vertPadding + textHeight + descenderHeight + totalVertPadding * 2) + " l" + (FILL_IN_WIDTH + horzPadding * 2) + " 0\" class=\"wideexpressionslot\"/>\n"; 

        toReturn.width = horzPadding * 2 + FILL_IN_WIDTH;
        toReturn.height = textHeight + descenderHeight + totalVertPadding * 2;
        hitBox.children.add(RenderedHitBox.forRectangleWithChildren(x, y + positioning.currentNestingInLine * vertPadding, toReturn.width, toReturn.height));
        positioning.x += toReturn.width; 
        positioning.maxBottom(toReturn.height);
        
        currentTokenPos.setMaxOffset(level + 1);

//      positioning.maxBottom(tokenHeight);

      
//      toReturn.height = positioning.lineBottom - startY;
      hitBox.height = toReturn.height;
    }

  }

/*
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
*/
  
  static void renderStatementContainer(StatementContainer codeList, TokenRendererReturn toReturn, TokenRendererPositioning positioning, CodePosition pos, int level, CodePosition currentTokenPos, TokenRenderer renderer, RenderedHitBox renderedHitBoxes, RenderSupplementalInfo supplement)
  {
//    Document doc = codeDiv.getOwnerDocument();

    int lineno = 0;
    String svgString = "";
    double xExtent = positioning.lineStart;
    RenderedHitBox.RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(positioning.lineStart, positioning.lineTop, 0, 0);
    for (TokenContainer line: codeList.statements)
    {
      positioning.maxBottom(renderer.minLineHeight());
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
      SvgCodeRenderer.renderLine(line, toReturn, positioning, level + 1, currentTokenPos, false, renderer, supplement, 1, false);
      svgString += toReturn.svgString;
      hitBox.children.add(toReturn.hitBox);
      currentTokenPos.setMaxOffset(level + 1);
      xExtent = Math.max(xExtent, toReturn.width + positioning.lineStart);
      positioning.newlineWithSpace();
//      codeDiv.appendChild(div);
      lineno++;
    }
    if (codeList.statements.isEmpty()) 
    {
//      DivElement div = doc.createDivElement();
      positioning.maxBottom(renderer.minLineHeight());
      
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
    toReturn.height = hitBox.height;
    toReturn.width = xExtent - positioning.lineStart;
    toReturn.hitBox = hitBox;
  }

  static void renderLine(TokenContainer line, TokenRendererReturn toReturn, TokenRendererPositioning positioning, int level, CodePosition currentTokenPos, boolean isStatement, TokenRenderer renderer, RenderSupplementalInfo supplement, int minPaddingNesting, boolean addSpaceToEnd)
  {
    // Check if the line contains some wide tokens
//    boolean hasWideTokens = false;
//    for (Token tok: line.tokens) 
//    {
//      if (tok.isWide())
//      {
//        hasWideTokens = true;
//        break;
//      }
//    }

    
//    // Mark the parent container if it only contains non-wide tokens
//    if (!hasWideTokens && isStatement)
//      div.getClassList().add("tokenline");
    
    // Actually render the line
//    DivElement subdiv = null;
//    Document doc = div.getOwnerDocument();
    toReturn.reset();
    RenderedHitBox.RectangleRenderedHitBox hitBox = RenderedHitBox.forRectangleWithChildren(positioning.x, positioning.lineTop + positioning.currentNestingInLine * renderer.vertPadding, 0, 0);  
    toReturn.hitBox = hitBox;
//    toReturn.height = positioning.fontSize;  // Minimum height for a line
    double startY = positioning.lineTop;
    int paddingNesting = positioning.maxNestingForLine - positioning.currentNestingInLine;
    if (paddingNesting < minPaddingNesting)
      paddingNesting = minPaddingNesting;
    int totalVertPadding = paddingNesting * renderer.vertPadding;
//     "' y='" + (y + positioning.currentNestingInLine * vertPadding) 
//        + "' height='" + (textHeight + descenderHeight + totalVertPadding * 2) 
    double tokenHeight = renderer.textHeight + renderer.descenderHeight + totalVertPadding * 2;
    toReturn.width = 0;
    int tokenno = 0;
    double minX = positioning.x;
    double maxX = positioning.x;
    TokenRendererReturn returnedRenderedToken = new TokenRendererReturn();
    boolean isStartOfLine = true;
    for (Token tok: line.tokens)
    {
      boolean isLastToken = (tokenno == line.tokens.size() - 1);
      currentTokenPos.setOffset(level, tokenno);
      TokenRendererPositioning subpositioning = positioning.copy();
      // When parameters tokens wrap, they expand to fill the full width of the screen,
      // but in some cases, we want some extra space at the end for a spacer, so let's
      // reserve that space in advance (we'll undo it later)--it might be safe
      // to do this for all non-wide tokens
      if (isLastToken && addSpaceToEnd && tok instanceof Token.ParameterToken)
        subpositioning.lineEnd -= renderer.horzEndParamPadding;
      tok.visit(renderer, returnedRenderedToken, subpositioning, level + 1, currentTokenPos, null);
      currentTokenPos.setMaxOffset(level + 1);
      if (!tok.isWide())
      {
        // If requested, we can add some extra space that sticks to the end of the last token
        // (this extra space should be added here so that we can wrap the token to the next line, and
        // not just this extra space)
        if (isLastToken && addSpaceToEnd)
          subpositioning.x += renderer.horzEndParamPadding;
        // See if we need to wrap 
        if (!isStartOfLine && 
            (subpositioning.x > subpositioning.lineEnd || returnedRenderedToken.wraps))
        {
          // Start a new line and lay out the token again
          positioning.newline();
          positioning.x = positioning.wrapLineStart;
          minX = Math.min(minX, positioning.x);
          subpositioning.copyFrom(positioning);
          toReturn.wraps = true;
          isStartOfLine = true;
          tok.visit(renderer, returnedRenderedToken, subpositioning, level + 1, currentTokenPos, null);
          currentTokenPos.setMaxOffset(level + 1);
          if (isLastToken && addSpaceToEnd)
            subpositioning.x += renderer.horzEndParamPadding;
        }
        isStartOfLine = false;
        RenderedHitBox.RectangleRenderedHitBox rectHitBox = returnedRenderedToken.hitBox;
        subpositioning.maxBottom(rectHitBox.y + rectHitBox.height - subpositioning.lineTop);
        if (returnedRenderedToken.wraps)
          toReturn.wraps = true;
      }
      if (isLastToken && addSpaceToEnd && tok instanceof Token.ParameterToken)
        subpositioning.lineEnd += renderer.horzEndParamPadding;
      positioning.copyFrom(subpositioning);
      if (returnedRenderedToken.svgString != null && !returnedRenderedToken.svgString.isEmpty())
      {
        if (!returnedRenderedToken.svgString.endsWith("\n"))
          toReturn.svgString += returnedRenderedToken.svgString + "\n";
        else
          toReturn.svgString += returnedRenderedToken.svgString;
      }
//      toReturn.width = Math.max(toReturn.width, returnedRenderedToken.width);
      maxX = Math.max(returnedRenderedToken.hitBox.x + returnedRenderedToken.hitBox.width, maxX);
      maxX = Math.max(positioning.x, maxX);
      hitBox.children.add(returnedRenderedToken.hitBox);
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

    positioning.maxBottom(tokenHeight);

    // If the last token is a wide token, there should be an empty line afterwards
    // where additional content can go
    boolean needEmptyLineAtEnd = false;
    if (!line.tokens.isEmpty() && line.endsWithWideToken())
      needEmptyLineAtEnd = true;
    if (needEmptyLineAtEnd)
    {
//      positioning.maxBottom(renderer.minLineHeight());
      hitBox.children.add(RenderedHitBox.forRectangle(
          positioning.lineStart, positioning.lineTop, 
          0, positioning.lineBottom - positioning.lineTop));
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
    
    toReturn.width = maxX - minX;
    toReturn.height = positioning.lineBottom - startY;
    hitBox.x = minX;
    hitBox.height = toReturn.height;
    hitBox.width = toReturn.width;

  }


}
