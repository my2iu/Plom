package org.programmingbasics.plom.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.RenderedCursorPosition;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;
import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
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
    startHookCodeWidget(toReturn, (DivElement)mainDiv.querySelector("div.code"), true);
    toReturn.hookScrollUpdateCursor((DivElement)mainDiv.querySelector("div.code"));
    return toReturn;
  }
  
  public static CodeWidgetBase forFullScreenDom(DivElement mainDiv)
  {
    CodeWidgetBase toReturn = new CodePanelDom(mainDiv);
    startHookCodeWidget(toReturn, (DivElement)mainDiv.querySelector("div.code"), true);
    toReturn.hookScrollUpdateCursor((DivElement)mainDiv.querySelector("div.code"));
    return toReturn;
  }

  static void startHookCodeWidget(CodeWidgetBase codePanel, DivElement codeDiv, boolean hasFocus)
  {
    if (hasFocus)
      codePanel.focus.showChoicesDivAndTakeFocus(codePanel);
    else
    {
      codePanel.focus.hideChoicesDiv();
      codePanel.focus.hideSimpleEntry();
    }
    
    codePanel.updateCodeView(true);
    if (hasFocus)
      codePanel.showPredictedTokenInput();
    if (codePanel.codeAreaScrolls)
      codePanel.provideCustomDivScrolling(codeDiv);
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
    scrollingDivForDoNotCover = codeDiv;
    divForDeterminingWindowWidth = (DivElement)mainDiv.querySelector("div.code .scrollable-interior");
    codeDivInteriorForScrollPadding = (Element)mainDiv.querySelector("div.code .scrollable-interior svg");
    focus = new CodeWidgetInputPanels(
        (DivElement)mainDiv.querySelector("div.choices"),
        new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
            (DivElement)mainDiv.querySelector("div.sidechoices"),
            (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent")),
        new NumberEntry((DivElement)mainDiv.querySelector("div.numberentry")),
        new CodeWidgetCursorOverlay((Element)mainDiv.querySelector("svg.cursoroverlay")),
        false);
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
      focus = new CodeWidgetInputPanels(
          (DivElement)mainDiv.querySelector("div.choices"),
          new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
              (DivElement)mainDiv.querySelector("div.sidechoices"),
              (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent")),
          new NumberEntry((DivElement)mainDiv.querySelector("div.numberentry")),
          new CodeWidgetCursorOverlay((Element)mainDiv.querySelector("svg.cursoroverlay")),
          false);
    }
    
    @Override public DivElement getBaseCodeDiv()
    {
      return codeDiv;
    }

    @Override void getExtentOfCurrentToken(CodePosition pos, AtomicInteger doNotCoverLeftRef, AtomicInteger doNotCoverRightRef)
    {
      // Not used in Dom version of code
    }

    @Override CodePosition hitDetectPointer(double x, double y, double cursorHandleXOffset, double cursorHandleYOffset)
    {
      double xOffset = cursorHandleXOffset, yOffset = cursorHandleYOffset;
      return HitDetect.renderAndHitDetect((int)(x + xOffset), (int)(y + yOffset), codeDivInterior, codeList, cursorPos, codeErrors);
    }

    @Override void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight)
    {
      focus.simpleEntry.scrollForDoNotCover(codeDiv, codeDivInteriorForScrollPadding, doNotCoverLeft, doNotCoverRight);
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

    @Override void updateForScroll(CodeWidgetCursorOverlay cursorOverlay)
    {
      cursorOverlay.adjustForCodeDivScrolling((- codeDiv.getScrollLeft()), (- codeDiv.getScrollTop()));
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
      focus.updateCursorForScroll();
      
      // Draw cursors
      focus.updateCursorVisibilityIfFocused();
      final int caretYOffset = cursorDiv.getOffsetHeight();
      final double caretOriginXOffset = (double)cursorDiv.getOffsetWidth() / 2;
      final double caretOriginYOffset = (double)cursorDiv.getOffsetHeight() * 0.8;
      focus.cursorOverlay.updatePrimaryCursor(x, y, caretYOffset, caretOriginXOffset,
          caretOriginYOffset);
      // Secondary cursor
      CursorRect selectionCursorRect = null;
      if (selectionCursorPos != null)
      {
        selectionCursorRect = RenderedCursorPosition.inStatements(codeList, selectionCursorPos, 0, renderedHitBoxes);
      }
      focus.cursorOverlay.updateSecondaryCursor(selectionCursorRect, selectionCursorPos, x, y, caretYOffset);
    }
  }
}
