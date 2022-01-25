package org.programmingbasics.plom.core;

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
public class SubCodeArea extends CodeWidgetBase.CodeWidgetBaseSvg
{
  public SubCodeArea(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding)
  {
    codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());

    codeDiv = (DivElement)mainDiv;
    divForDeterminingWindowWidth = (DivElement)mainDiv;
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
  }

}
