package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.RenderedCursorPosition;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;
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
 * Coding area that's just part of a larger page
 */
@JsType
public class SubCodeArea extends CodeWidgetBase
{
  public SubCodeArea(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding)
  {
    this.useSvg = true;
    
    codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());

    codeDiv = (DivElement)mainDiv;
//    codeDivInterior = (DivElement)codeDiv.querySelector(".scrollable-interior");
    this.codeDivInteriorForScrollPadding = codeDivInteriorForScrollPadding;
    
    this.choicesDiv = choicesDiv;
    cursorOverlayEl = cursorOverlay;
    simpleEntry = new SimpleEntry((DivElement)simpleEntryDiv,
        (DivElement)sideChoices);

    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    
    updateCodeView(true);
    showPredictedTokenInput(choicesDiv);
    CodePanel.hookCodeScroller(codeDiv);
    hookCodeClick((DivElement)codeDiv);
    
//    SvgCodeRenderer.test();
//    hookTestCodeClick();
  }

 
  DivElement codeDiv;
//  DivElement codeDivInterior;
  Element codeDivInteriorForScrollPadding;

  @Override CodePosition hitDetectPointer(double x, double y, CursorHandle cursorHandle)
  {
    double xOffset = 0, yOffset = 0;
    if (cursorHandle != null)
    {
      xOffset = cursorHandle.xOffset;
      yOffset = cursorHandle.yOffset;
    }
//    if (useSvg)
      return HitDetect.detectHitBoxes((int)(x + xOffset - leftPadding), (int)(y + yOffset - topPadding), codeList, svgHitBoxes);
//    else
//      return HitDetect.renderAndHitDetect((int)(x + xOffset), (int)(y + yOffset), codeDivInterior, codeList, cursorPos, codeErrors);
  }

//  void hookTestCodeClick()
//  {
////    div.addEventListener(Event.SCROLL, (evt) -> {
////      cursorOverlayEl.querySelector("g.cursorscrolltransform").setAttribute("transform", "translate(" + (- codeDiv.getScrollLeft()) + " " + (- codeDiv.getScrollTop()) + ")");
////    }, false);
////    hookCursorHandle(div, (Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0), 1);
////    hookCursorHandle(div, (Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1), 2);
//    
//    DivElement div = SvgCodeRenderer.testDiv; 
//    
//    div.addEventListener(Event.CLICK, (evt) -> {
//      PointerEvent pevt = (PointerEvent)evt;
//      double x = pointerToRelativeX(pevt, div);
//      double y = pointerToRelativeY(pevt, div);
//      CodePosition newPos = 
//          HitDetect.testHitDetect(x, y, codeList, SvgCodeRenderer.testHitBox, 0);
//      if (newPos == null)
//        newPos = new CodePosition();
//      cursorPos = newPos;
//      selectionCursorPos = null;
//
//      updateCodeView(false);
//      showPredictedTokenInput(choicesDiv);
//    }, false);
//  }

  @Override void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight)
  {
    simpleEntry.scrollForDoNotCover(codeDiv, codeDivInteriorForScrollPadding, doNotCoverLeft, doNotCoverRight);
  }
  
  @Override void updateCodeView(boolean isCodeChanged)
  {
    if (listener != null)
      listener.onUpdate(isCodeChanged);
    RenderedHitBox renderedHitBoxes = CodePanel.renderTokens((DivElement)codeDivInteriorForScrollPadding, codeSvg, codeList, cursorPos, selectionCursorPos, codeErrors, widthCalculator);
    if (useSvg)
      svgHitBoxes = renderedHitBoxes;
    updateCursor(renderedHitBoxes);
//    addCursorOverlay();
  }
  
//  void addCursorOverlay()
//  {
//    // Add an svg overlay
//    SVGSVGElement svg = Browser.getDocument().createSVGElement();
//    svg.getStyle().setLeft("0");
//    svg.getStyle().setTop("0");
//    svg.getStyle().setWidth(codeDiv.getScrollWidth(), Unit.PX);
////    svg.getStyle().setRight("0");
////    svg.getStyle().setBottom("0");
//    svg.getStyle().setHeight(codeDiv.getScrollHeight(), Unit.PX);
//    svg.setAttribute("width", "100%");
//    svg.setAttribute("height", "100%");
//    svg.getStyle().setPosition(Position.ABSOLUTE);
//    svg.getStyle().setProperty("pointer-events", "none");
//    codeDiv.appendChild(svg);
//    
//    // Find where the cursor should be placed
//    DivElement cursorDiv = (DivElement)codeDiv.querySelector(".codecursor");
//    if (cursorDiv == null) return;
//    double x = 0, y = 0;
//    for (Element el = cursorDiv; el != codeDiv; el = el.getOffsetParent())
//    {
//      x += el.getOffsetLeft();
//      y += el.getOffsetTop();
//    }
//    
//    // Draw a handle under the cursor
////    updateCursor();
//
////    createCursorHandleSvg(svg, cursorHandle1);
//  }

  @Override void updateForScroll()
  {
     String cssScrollTranslate = "translate(" + (- codeDiv.getScrollLeft()) + " " + (- codeDiv.getScrollTop()) + ")";
     if (useSvg)
         cssScrollTranslate += " translate(" + leftPadding + ", " + topPadding + ")";
     cursorOverlayEl.querySelector("g.cursorscrolltransform").setAttribute("transform", cssScrollTranslate);
  }
  
  // We need the renderedhitboxes of the code to figure out where
  // the cursor is
  void updateCursor(RenderedHitBox renderedHitBoxes)
  {
    DivElement cursorDiv = null;
    double x = 0, y = 0;
    if (useSvg)
    {
      CursorRect cursorRect = RenderedCursorPosition.inStatements(codeList, cursorPos, 0, renderedHitBoxes);
      // Draw caret for the secondary cursor
      Element caretCursor = cursorOverlayEl.querySelector(".maincursorcaret"); 
      if (cursorRect != null)
      {
        caretCursor.getStyle().clearDisplay();
        caretCursor.setAttribute("x1", "" + cursorRect.left);
        caretCursor.setAttribute("x2", "" + cursorRect.left);
        caretCursor.setAttribute("y1", "" + cursorRect.top);
        caretCursor.setAttribute("y2", "" + cursorRect.bottom);
      }
      x = cursorRect.left;
      y = cursorRect.bottom;
    }
    else
    {
      cursorDiv = (DivElement)codeDiv.querySelector(".codecursor");
      if (cursorDiv == null) return;
      for (Element el = cursorDiv; el != codeDiv; el = el.getOffsetParent())
      {
        x += el.getOffsetLeft();
        y += el.getOffsetTop();
      }
    }
    // Handle scrolling
    updateForScroll();

    // Main cursor
    if (pointerDownHandle == 1)
    {
      ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
    }
    else
    {
      if (!useSvg)
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (x) + " " + (y + cursorDiv.getOffsetHeight() + HANDLE_SIZE) + ")");
      else
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (x) + " " + (y /*+ cursorDiv.getOffsetHeight()*/ + HANDLE_SIZE) + ")");
    }
    if (!useSvg)
    {
      cursorHandle1.x = x;
      cursorHandle1.y = y + cursorDiv.getOffsetHeight() + HANDLE_SIZE + 2;
      cursorHandle1.xOffset = (x + (double)cursorDiv.getOffsetWidth() / 2) - cursorHandle1.x; 
      cursorHandle1.yOffset = (y + (double)cursorDiv.getOffsetHeight() * 0.8) - cursorHandle1.y;
    }
    else
    {
      cursorHandle1.x = x;
      cursorHandle1.y = y /*+ cursorDiv.getOffsetHeight()*/ + HANDLE_SIZE + 2;
      cursorHandle1.xOffset = (x /*+ (double)cursorDiv.getOffsetWidth() / 2*/) - cursorHandle1.x; 
      cursorHandle1.yOffset = (y /*+ (double)cursorDiv.getOffsetHeight() * 0.8*/) - cursorHandle1.y;
    }
    // Secondary cursor
    CursorRect selectionCursorRect = null;
    if (selectionCursorPos != null)
    {
      selectionCursorRect = RenderedCursorPosition.inStatements(codeList, selectionCursorPos, 0, renderedHitBoxes);
    }
    // Draw caret for the secondary cursor
    Element caretCursor = cursorOverlayEl.querySelector(".cursorcaret"); 
    if (selectionCursorRect != null)
    {
      caretCursor.getStyle().clearDisplay();
      caretCursor.setAttribute("x1", "" + selectionCursorRect.left);
      caretCursor.setAttribute("x2", "" + selectionCursorRect.left);
      caretCursor.setAttribute("y1", "" + selectionCursorRect.top);
      caretCursor.setAttribute("y2", "" + selectionCursorRect.bottom);
    }
    else
    {
        caretCursor.getStyle().setDisplay(Display.NONE);
    }
    // Secondary cursor handle
    if (selectionCursorRect == null)
    {
      // If there is no secondary cursor to draw a handle around, draw
      // the handle relative to the main cursor instead
      if (!useSvg)
        selectionCursorRect = new CursorRect(x, y, y + cursorDiv.getOffsetHeight());
      else
        selectionCursorRect = new CursorRect(x, y, y);
    }
    x = selectionCursorRect.left;
    y = selectionCursorRect.top;
    if (pointerDownHandle == 2)
    {
      ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
//      cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
//      cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
    else
    {
      if (selectionCursorPos != null)
      {
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      else
      {
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x + 2.5 * HANDLE_SIZE) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      cursorHandle2.x = x;
      cursorHandle2.y = selectionCursorRect.bottom + HANDLE_SIZE + 2;
      cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
      cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
  }
  
//  void createCursorHandleSvg(SVGElement parent, CursorHandle handle)
//  {
//    SVGCircleElement handleSvg = Browser.getDocument().createSVGCircleElement();
//    parent.appendChild(handleSvg);
//    handleSvg.setAttribute("cx", "" + handle.x);
//    handleSvg.setAttribute("cy", "" + handle.y);
//    handleSvg.setAttribute("r", "" + HANDLE_SIZE);
//    handleSvg.getStyle().setProperty("fill", "none");
//    handleSvg.getStyle().setProperty("stroke", "#000");
//    handleSvg.getStyle().setProperty("stroke-width", "1px");
//    
//  }
  
}
