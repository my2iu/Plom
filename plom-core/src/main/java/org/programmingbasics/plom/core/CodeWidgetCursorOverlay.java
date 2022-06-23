package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;
import elemental.html.DivElement;

class CodeWidgetCursorOverlay
{
  Element el;

  CursorMovingCallback codeWidget;
  
  CodeWidgetCursorOverlay(Element cursorOverlayEl)
  {
    this.el = cursorOverlayEl;
    hookCursorHandles();
  }

  static interface CursorMovingCallback
  {
    // Callback from the cursor overlay when the user has stopped dragging a cursor
    void cursorHandleEndMove();
    // Callback from the cursor overlay when the user is dragging a cursor by its handle
    void cursorHandleMoving(double x, double y, int pointerDownHandle, double cursorHandleXOffset, double cursorHandleYOffset);
    // Returns the div that all x and y coordinates should be relative to (i.e. the div that provides the origin for coordinates)
    DivElement getBaseCodeDiv();
  }

  // Sends a notification of the cursor handle ending its move
  private void fireCursorHandleEndMove()
  {
    if (codeWidget != null)
      codeWidget.cursorHandleEndMove();
  }
  // Sends a notification of the cursor handle moving
  private void fireCursorHandleMoving(double x, double y, int pointerDownHandle, double cursorHandleXOffset, double cursorHandleYOffset)
  {
    if (codeWidget != null)
      codeWidget.cursorHandleMoving(x, y, pointerDownHandle, cursorHandleXOffset, cursorHandleYOffset);
  }

  
  /**
   * For on-screen cursor handle that user can drag to move the cursor
   */
  protected static class CursorHandle
  {
    double x, y;
    double xOffset, yOffset;
  }
  CursorHandle cursorHandle1 = new CursorHandle();
  CursorHandle cursorHandle2 = new CursorHandle();
  final static int HANDLE_SIZE = 20;

  int pointerDownHandle = 0;  // 0 - pointer was not pressed on a handle
  double pointerDownX = 0;
  double pointerDownY = 0;

  // When in full-screen, the code div can scroll, but the cursor overlay
  // spans the coding area and does not scroll, so we need to move the cursors
  // to match the scrolling of the coding area
  void adjustForCodeDivScrolling(double dx, double dy)
  {
    String cssScrollTranslate = "translate(" + (dx) + " " + (dy) + ")";
    el.querySelector("g.cursorscrolltransform").setAttribute("transform", cssScrollTranslate);
  }
  
  void hookCursorHandles()
  {
    // The codeDiv should not be a parent of the 
    // cursorOverlayEl (svg element containing the cursor overlay).
    // Otherwise, dragging of the cursors on the cursor overlay will
    // send click events to the codeDiv.
    hookCursorHandle((Element)el.querySelectorAll(".cursorhandle").item(0), 1);
    hookCursorHandle((Element)el.querySelectorAll(".cursorhandle").item(1), 2);
  }
  
  private void hookCursorHandle(Element cursorHandleEl, int cursorId)
  {
    CodeWidgetBase.addActiveEventListenerTo(cursorHandleEl, "pointerdown", (evt) -> {
      if (codeWidget == null) return;
      PointerEvent pevt = (PointerEvent)evt;
      CodeWidgetBase.setPointerCapture(cursorHandleEl, pevt.getPointerId());
//        setPointerCapture(Browser.getDocument().querySelector(".asd"),
//            pevt.getPointerId());
      evt.preventDefault();
      evt.stopPropagation();
      pointerDownHandle = cursorId;
      DivElement div = codeWidget.getBaseCodeDiv();
      double x = CodeWidgetBase.pointerToRelativeX(pevt, div);
      double y = CodeWidgetBase.pointerToRelativeY(pevt, div);
      pointerDownX = x;
      pointerDownY = y;
    }, false);
    CodeWidgetBase.addActiveEventListenerTo(cursorHandleEl, "pointermove", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      if (pointerDownHandle != 0 && codeWidget != null)
      {
        DivElement div = codeWidget.getBaseCodeDiv();
        double x = CodeWidgetBase.pointerToRelativeX(pevt, div);
        double y = CodeWidgetBase.pointerToRelativeY(pevt, div);
        pointerDownX = x;
        pointerDownY = y;
        if (pointerDownHandle == 1)
        {
          double xOffset = 0, yOffset = 0;
          if (cursorHandle1 != null)
          {
            xOffset = cursorHandle1.xOffset;
            yOffset = cursorHandle1.yOffset;
          }
          fireCursorHandleMoving(x, y, pointerDownHandle, xOffset, yOffset);
        }
        else if (pointerDownHandle == 2)
        {
          double xOffset = 0, yOffset = 0;
          if (cursorHandle2 != null)
          {
            xOffset = cursorHandle2.xOffset;
            yOffset = cursorHandle2.yOffset;
          }
          fireCursorHandleMoving(x, y, pointerDownHandle, xOffset, yOffset);

        }
      }
      evt.preventDefault();
      evt.stopPropagation();
    }, false);
    cursorHandleEl.addEventListener("pointerup", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      CodeWidgetBase.releasePointerCapture(cursorHandleEl, pevt.getPointerId());
      evt.preventDefault();
      evt.stopPropagation();
      pointerDownHandle = 0;
      fireCursorHandleEndMove();
    }, false);
    cursorHandleEl.addEventListener("pointercancel", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      pointerDownHandle = 0;
      CodeWidgetBase.releasePointerCapture(cursorHandleEl, pevt.getPointerId());
      fireCursorHandleEndMove();
    }, false);
    
  }

  void updatePrimaryCursor(double x, double y, final int caretYOffset,
      final double caretOriginXOffset, final double caretOriginYOffset)
  {
    if (pointerDownHandle == 1)
    {
      ((Element)el.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
    }
    else
    {
      ((Element)el.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (x) + " " + (y + caretYOffset + HANDLE_SIZE) + ")");
    }
    cursorHandle1.x = x;
    cursorHandle1.y = y + caretYOffset + HANDLE_SIZE + 2;
    cursorHandle1.xOffset = (x + caretOriginXOffset) - cursorHandle1.x; 
    cursorHandle1.yOffset = (y + caretOriginYOffset) - cursorHandle1.y;
  }

  void updateSecondaryCursor(CursorRect selectionCursorRect, CodePosition selectionCursorPos, double x, double y, int caretYOffset)
  {
    // Draw caret for the secondary cursor
    Element caretCursor = el.querySelector(".cursorcaret"); 
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
      selectionCursorRect = new CursorRect(x, y, y + caretYOffset);
    }
    x = selectionCursorRect.left;
    y = selectionCursorRect.top;
    if (pointerDownHandle == 2)
    {
      ((Element)el.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
//        cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
//        cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
    else
    {
      if (selectionCursorPos != null)
      {
        ((Element)el.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      else
      {
        ((Element)el.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x + 2.5 * HANDLE_SIZE) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      cursorHandle2.x = x;
      cursorHandle2.y = selectionCursorRect.bottom + HANDLE_SIZE + 2;
      cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
      cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
  }

  void updateCursorVisibilityIfFocused(boolean hasFocus)
  {
    if (hasFocus)
      el.getStyle().clearDisplay();
    else
      el.getStyle().setDisplay(Display.NONE);
  }

  /** During SVG rendering, the caret is shown in the overlay and
   * it's position needs to be updated when the cursor moves 
   * (when DOM rendering, the caret in part of the DOM for the
   * code and does not need to be updated)
   */
  void updateSvgCaret(CursorRect cursorRect)
  {
    Element caretCursor = el.querySelector(".maincursorcaret"); 
    if (cursorRect != null)
    {
      caretCursor.getStyle().clearDisplay();
      caretCursor.setAttribute("x1", "" + cursorRect.left);
      caretCursor.setAttribute("x2", "" + cursorRect.left);
      caretCursor.setAttribute("y1", "" + cursorRect.top);
      caretCursor.setAttribute("y2", "" + cursorRect.bottom);
    }
  }
}