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

/**
 * Coding area that's just part of a larger page
 */
@JsType
public class SubCodeArea extends CodeWidgetBase.CodeWidgetBaseSvg
{
  EventRemover docBlurListener;

  public static SubCodeArea forMethodParameterField(Element mainDiv, 
      CodeWidgetInputPanels inputPanels,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    SubCodeArea codeArea = create(Symbol.ParameterFieldOnly, true,
        mainDiv, inputPanels, 
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        20, 3, 3, 3,
        divForWindowWidth, widthCalculator);
    return codeArea;
  }
  
  public static SubCodeArea forTypeField(Element mainDiv,
      CodeWidgetInputPanels inputPanels,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    SubCodeArea codeArea = create(Symbol.ReturnTypeFieldOnly, true,
        mainDiv, inputPanels, 
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        20, 3, 3, 3,
        divForWindowWidth, widthCalculator);
    return codeArea;
  }

  public static SubCodeArea forVariableDeclaration(Element mainDiv, 
      CodeWidgetInputPanels inputPanels,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    return create(Symbol.FullVariableDeclaration, false, 
        mainDiv, 
        inputPanels,
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        10, 10, 10, 10,
        divForWindowWidth, widthCalculator);
  }
  
  private static SubCodeArea create(
      Symbol grammar, boolean isSingleLineMode,
      Element mainDiv, 
      CodeWidgetInputPanels inputPanels,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      double leftPadding, double rightPadding, double topPadding, double bottomPadding,
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    SubCodeArea codeArea = new SubCodeArea(mainDiv,
        inputPanels,
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover,
        divForWindowWidth,
        widthCalculator);
    codeArea.defaultParseContext = grammar;
    codeArea.leftPadding = leftPadding;
    codeArea.rightPadding = rightPadding;
    codeArea.topPadding = topPadding;
    codeArea.bottomPadding = bottomPadding;
    codeArea.isSingleLineMode = isSingleLineMode;
    codeArea.codeAreaScrolls = false;
    
    CodePanel.startHookCodeWidget(codeArea, codeArea.codeDiv, false);
    codeArea.hookScrollUpdateCursor(scrollingDivForDoNotCover);
    codeArea.docBlurListener = Browser.getDocument().addEventListener(Event.BLUR, (evt) -> {
      // Listen for what has focus across the document so that if focus
      // is outside the coding area or related input elements
      boolean hasFocus = codeArea.isFocusInCodingArea((Node)((MouseEvent)evt).getRelatedTarget());
//      Browser.getWindow().getConsole().log(((MouseEvent)evt).getRelatedTarget());
      if (!hasFocus)
      {
        codeArea.focus.hideChoicesDiv();
        // Careful here. On losing focus, the simple entry will close
        // but won't fire an event saying that text entry has completed
        codeArea.focus.hideSimpleEntry();
        codeArea.focus.setCurrent(null);
        codeArea.focus.updateCursorVisibilityIfFocused(hasFocus);
      }
    }, true);
    
    return codeArea;
  }
  
  private SubCodeArea(Element mainDiv, CodeWidgetInputPanels inputPanels, 
      Element codeDivInteriorForScrollPadding, 
      Element scrollingDivForDoNotCover, Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    this.codeSvg = (SVGSVGElement)mainDiv.querySelector("svg.codeareasvg");
    this.widthCalculator = widthCalculator;

    this.codeDiv = (DivElement)mainDiv;
    this.divForDeterminingWindowWidth = (DivElement)divForWindowWidth;
    this.codeDivInteriorForScrollPadding = codeDivInteriorForScrollPadding;
    this.scrollingDivForDoNotCover = scrollingDivForDoNotCover;
    
    this.focus = inputPanels;
  }

  private boolean isFocusInCodingArea(Node originalTarget)
  {
    Node target = originalTarget;
    while (target != null)
    {
      if (target == codeDiv)
        return true;
      target = target.getParentNode();
    }
    return focus.isFocusInInputPanels(originalTarget);
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
    docBlurListener.remove();
    super.close();
  }
}
