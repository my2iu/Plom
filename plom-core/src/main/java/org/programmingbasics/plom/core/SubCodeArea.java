package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.events.UIEvent;
import elemental.html.DivElement;
import elemental.svg.SVGSVGElement;
import jsinterop.annotations.JsType;

// - loss of focus
// - change in grammar

/**
 * Coding area that's just part of a larger page
 */
@JsType
public class SubCodeArea extends CodeWidgetBase.CodeWidgetBaseSvg
{
  EventRemover docFocusListener;
  
  public SubCodeArea(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    this.widthCalculator = widthCalculator;

    codeDiv = (DivElement)mainDiv;
    divForDeterminingWindowWidth = (DivElement)divForWindowWidth;
    this.codeDivInteriorForScrollPadding = codeDivInteriorForScrollPadding;
    
    this.choicesDiv = choicesDiv;
    cursorOverlayEl = cursorOverlay;
    simpleEntry = new SimpleEntry((DivElement)simpleEntryDiv,
        (DivElement)sideChoices);

    CodePanel.startHookCodeWidget(this, codeDiv, false, false);
    docFocusListener = Browser.getDocument().addEventListener(Event.BLUR, (evt) -> {
      // Listen for what has focus across the document so that if focus
      // is outside the coding area or related input elements
      boolean hasFocus = isFocusInCodingArea((Node)((MouseEvent)evt).getRelatedTarget());
      Browser.getWindow().getConsole().log((Node)((MouseEvent)evt).getTarget());
      Browser.getWindow().getConsole().log((Node)((MouseEvent)evt).getRelatedTarget());
      if (!hasFocus)
      {
        hideChoicesDiv();
        simpleEntry.setVisible(false);
      }
    }, true);
  }

  private boolean isFocusInCodingArea(Node target)
  {
    while (target != null)
    {
      if (target == codeDiv)
        return true;
      if (target == simpleEntry.container)
        return true;
      if (target == choicesDiv)
        return true;
      if (target == simpleEntry.suggestionsContainer)
        return true;
      target = target.getParentNode();
    }
    return false;
  }
  
  @Override void showChoicesDiv()
  {
    super.showChoicesDiv();
    // Also assign focus to the coding area so that focus isn't lost
    choicesDiv.focus();
  }
  
  @Override void updateCodeView(boolean isCodeChanged)
  {
    super.updateCodeView(isCodeChanged);
    // Copy the width and height to the container
    codeDiv.getStyle().setWidth(codeSvg.getStyle().getWidth());
    codeDiv.getStyle().setHeight(codeSvg.getStyle().getHeight());
  }

  @Override public void close()
  {
    docFocusListener.remove();
    super.close();
  }
}
