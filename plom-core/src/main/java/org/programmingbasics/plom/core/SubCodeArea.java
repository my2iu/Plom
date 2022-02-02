package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.events.MouseEvent;
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
  
  public static SubCodeArea forVariableDeclaration(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    SubCodeArea codeArea = new SubCodeArea(mainDiv, choicesDiv,
        cursorOverlay, simpleEntryDiv, sideChoices,
        codeDivInteriorForScrollPadding, divForWindowWidth,
        widthCalculator);
    codeArea.defaultParseContext = Symbol.FullVariableDeclaration;
    
    CodePanel.startHookCodeWidget(codeArea, codeArea.codeDiv, false, false);
    codeArea.docFocusListener = Browser.getDocument().addEventListener(Event.BLUR, (evt) -> {
      // Listen for what has focus across the document so that if focus
      // is outside the coding area or related input elements
      boolean hasFocus = codeArea.isFocusInCodingArea((Node)((MouseEvent)evt).getRelatedTarget());
      if (!hasFocus)
      {
        codeArea.hideChoicesDiv();
        codeArea.simpleEntry.setVisible(false);
      }
    }, true);
    return codeArea;
  }
  
  public SubCodeArea(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    this.codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    this.widthCalculator = widthCalculator;

    this.codeDiv = (DivElement)mainDiv;
    this.divForDeterminingWindowWidth = (DivElement)divForWindowWidth;
    this.codeDivInteriorForScrollPadding = codeDivInteriorForScrollPadding;
    
    this.choicesDiv = choicesDiv;
    this.cursorOverlayEl = cursorOverlay;
    this.simpleEntry = new SimpleEntry((DivElement)simpleEntryDiv,
        (DivElement)sideChoices);
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
