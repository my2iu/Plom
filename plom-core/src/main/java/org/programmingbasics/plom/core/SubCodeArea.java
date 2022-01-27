package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.css.CSSStyleDeclaration.Unit;
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
      Element codeDivInteriorForScrollPadding, Element divForWindowWidth)
  {
    codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());

    codeDiv = (DivElement)mainDiv;
    divForDeterminingWindowWidth = (DivElement)divForWindowWidth;
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

  @Override void updateCodeView(boolean isCodeChanged)
  {
    super.updateCodeView(isCodeChanged);
    // Copy the width and height to the container
    codeDiv.getStyle().setWidth(codeSvg.getStyle().getWidth());
    codeDiv.getStyle().setHeight(codeSvg.getStyle().getHeight());
  }

}
