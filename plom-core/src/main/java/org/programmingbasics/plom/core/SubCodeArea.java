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

  public static SubCodeArea forMethodParameterField(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, SimpleEntry simpleEntry, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    SubCodeArea codeArea = create(Symbol.ParameterFieldOnly, true,
        mainDiv, choicesDiv, cursorOverlay, simpleEntry, sideChoices, 
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        20, 3, 3, 3,
        divForWindowWidth, widthCalculator);
    return codeArea;
  }

  public static SubCodeArea forTypeField(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, SimpleEntry simpleEntry, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    SubCodeArea codeArea = create(Symbol.ReturnTypeFieldOnly, true,
        mainDiv, choicesDiv, cursorOverlay, simpleEntry, sideChoices, 
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        20, 3, 3, 3,
        divForWindowWidth, widthCalculator);
    return codeArea;
  }

  public static SubCodeArea forVariableDeclaration(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, Element simpleEntryDiv, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator) 
  {
    return create(Symbol.FullVariableDeclaration, false, 
        mainDiv, choicesDiv, cursorOverlay, 
        new SimpleEntry((DivElement)simpleEntryDiv,
            (DivElement)sideChoices), 
        sideChoices, 
        codeDivInteriorForScrollPadding, scrollingDivForDoNotCover, 
        10, 10, 10, 10,
        divForWindowWidth, widthCalculator);
  }
  
  private static SubCodeArea create(
      Symbol grammar, boolean isSingleLineMode,
      Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, SimpleEntry simpleEntry, Element sideChoices,
      Element codeDivInteriorForScrollPadding, Element scrollingDivForDoNotCover, 
      double leftPadding, double rightPadding, double topPadding, double bottomPadding,
      Element divForWindowWidth,
      SvgCodeRenderer.SvgTextWidthCalculator widthCalculator)
  {
    SubCodeArea codeArea = new SubCodeArea(mainDiv, choicesDiv,
        cursorOverlay, simpleEntry, sideChoices,
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
        codeArea.hideChoicesDiv();
        // Careful here. On losing focus, the simple entry will close
        // but won't fire an event saying that text entry has completed
        codeArea.simpleEntry.setVisible(false);
        codeArea.hasFocus = false;
        codeArea.updateCursorVisibilityIfFocused();
      }
    }, true);
    
    return codeArea;
  }
  
  private SubCodeArea(Element mainDiv, DivElement choicesDiv,
      Element cursorOverlay, SimpleEntry simpleEntry, Element sideChoices,
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
    
    this.choicesDiv = choicesDiv;
    this.cursorOverlay = new CodeWidgetCursorOverlay(this, cursorOverlay);
    this.simpleEntry = simpleEntry;
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
    docBlurListener.remove();
    super.close();
  }
}
