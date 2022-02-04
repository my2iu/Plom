package org.programmingbasics.plom.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.svg.SVGDocument;
import elemental.svg.SVGSVGElement;
import jsinterop.annotations.JsType;

/**
 * Coding area that takes up the full screen
 */
@JsType
public class CodePanel extends CodeWidgetBase.CodeWidgetBaseSvg
{
  /**
   * Creates a code widget that takes up a full window area and includes
   * all extra UI elements like the choices keyboard, side options,
   * and simple entry area
   */
  public static CodeWidgetBase forFullScreen(DivElement mainDiv, boolean useSvg)
  {
    if (useSvg)
      return forFullScreenSvg(mainDiv);
    else
      return forFullScreenDom(mainDiv);
  }
  
  public static CodeWidgetBase forFullScreenSvg(DivElement mainDiv)
  {
    CodeWidgetBase toReturn = new CodePanel(mainDiv);
    startHookCodeWidget(toReturn, (DivElement)mainDiv.querySelector("div.code"), true, true);
    return toReturn;
  }
  
  public static CodeWidgetBase forFullScreenDom(DivElement mainDiv)
  {
    CodeWidgetBase toReturn = new CodePanelDom(mainDiv);
    startHookCodeWidget(toReturn, (DivElement)mainDiv.querySelector("div.code"), true, true);
    return toReturn;
  }

  static void startHookCodeWidget(CodeWidgetBase codePanel, DivElement codeDiv, boolean handleTouchScrolling, boolean hasFocus)
  {
    codePanel.hasFocus = hasFocus;
    if (hasFocus)
      codePanel.showChoicesDiv();
    codePanel.simpleEntry.setVisible(false);
    
    codePanel.updateCodeView(true);
    if (hasFocus)
      codePanel.showPredictedTokenInput();
    codePanel.hookCodeScroller(codeDiv, handleTouchScrolling);
    codePanel.hookCodeClick(codeDiv);
    
//    SvgCodeRenderer.test();
//    hookTestCodeClick();
  }
  
  public CodePanel(DivElement mainDiv)
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getSvgCodePanelHtml().getText());
    codeSvg = (SVGSVGElement)mainDiv.querySelector("div.code svg");
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());

    codeDiv = (DivElement)mainDiv.querySelector("div.code");
    divForDeterminingWindowWidth = (DivElement)mainDiv.querySelector("div.code .scrollable-interior");
    codeDivInteriorForScrollPadding = (Element)mainDiv.querySelector("div.code .scrollable-interior svg");
    choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
    cursorOverlayEl = (Element)mainDiv.querySelector("svg.cursoroverlay");
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));
  }
  
  // DOM Variant of the CodePanel
  public static class CodePanelDom extends CodeWidgetBase
  {
    DivElement codeDiv;
    DivElement codeDivInterior;
    Element codeDivInteriorForScrollPadding;
    
    public CodePanelDom(DivElement mainDiv)
    {
      mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());
      
      codeDiv = (DivElement)mainDiv.querySelector("div.code");
      codeDivInterior = (DivElement)mainDiv.querySelector("div.code .scrollable-interior");
      codeDivInteriorForScrollPadding = (Element)mainDiv.querySelector("div.code .scrollable-interior");
      choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
      cursorOverlayEl = (Element)mainDiv.querySelector("svg.cursoroverlay");
      simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
          (DivElement)mainDiv.querySelector("div.sidechoices"));
    }
    
    @Override void getExtentOfCurrentToken(CodePosition pos, AtomicInteger doNotCoverLeftRef, AtomicInteger doNotCoverRightRef)
    {
      // Not used in Dom version of code
    }

    @Override CodePosition hitDetectPointer(double x, double y, CursorHandle cursorHandle)
    {
      double xOffset = 0, yOffset = 0;
      if (cursorHandle != null)
      {
        xOffset = cursorHandle.xOffset;
        yOffset = cursorHandle.yOffset;
      }
      return HitDetect.renderAndHitDetect((int)(x + xOffset), (int)(y + yOffset), codeDivInterior, codeList, cursorPos, codeErrors);
    }

    @Override void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight)
    {
      simpleEntry.scrollForDoNotCover(codeDiv, codeDivInteriorForScrollPadding, doNotCoverLeft, doNotCoverRight);
    }
    
    @Override void updateCodeView(boolean isCodeChanged)
    {
      if (listener != null)
        listener.onUpdate(isCodeChanged);
      codeDivInterior.setInnerHTML("");
      RenderedHitBox renderedHitBoxes = renderTokensDom(codeDivInterior, codeList, cursorPos, selectionCursorPos, codeErrors);
      updateCursor(renderedHitBoxes);
    }

    /**
     * Returns a mapping of divs for each line and their line numbers
     */
    private static RenderedHitBox renderTokensDom(DivElement codeDiv, StatementContainer codeList,
        CodePosition pos, CodePosition selectionPos, ErrorList codeErrors)
    {
      if (selectionPos != null)
        return CodeRenderer.renderWithHitBoxes(codeDiv, codeList, pos, pos, selectionPos, codeErrors);
      else
        return CodeRenderer.renderWithHitBoxes(codeDiv, codeList, pos, null, null, codeErrors);
    }

    @Override void updateForScroll()
    {
       String cssScrollTranslate = "translate(" + (- codeDiv.getScrollLeft()) + " " + (- codeDiv.getScrollTop()) + ")";
       cursorOverlayEl.querySelector("g.cursorscrolltransform").setAttribute("transform", cssScrollTranslate);
    }
    
    // We need the renderedhitboxes of the code to figure out where
    // the cursor is
    void updateCursor(RenderedHitBox renderedHitBoxes)
    {
      DivElement cursorDiv = null;
      cursorDiv = (DivElement)codeDiv.querySelector(".codecursor");
      if (cursorDiv == null) return;
      double x = 0, y = 0;
      for (Element el = cursorDiv; el != codeDiv; el = el.getOffsetParent())
      {
        x += el.getOffsetLeft();
        y += el.getOffsetTop();
      }
      
      // Handle scrolling
      updateForScroll();
      
      // Draw cursors
      updateCursorVisibilityIfFocused();
      final int caretYOffset = cursorDiv.getOffsetHeight();
      final double caretOriginXOffset = (double)cursorDiv.getOffsetWidth() / 2;
      final double caretOriginYOffset = (double)cursorDiv.getOffsetHeight() * 0.8;
      updatePrimaryCursor(x, y, caretYOffset, caretOriginXOffset,
          caretOriginYOffset);
      updateSecondaryCursor(renderedHitBoxes, x, y, caretYOffset);
    }
  }
}
