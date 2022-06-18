package org.programmingbasics.plom.core;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Node;
import elemental.html.DivElement;

/**
 * When working with a CodeWidget, there are associated input panels
 * that may appear on the screen too. If you have multiple coding areas,
 * they will share access to these coding panels, so we need to have
 * a single close to manage focus and other issues
 */
public class CodeWidgetInputPanels
{
  SimpleEntry simpleEntry;
  DivElement choicesDiv;
  CodeWidgetCursorOverlay cursorOverlay;
  
  /** When showing the choices div, force it to gain focus. Needed with SubCodingArea 
   * so that when user switches between the actual SubCodingArea and the choices div
   * by touching on the choices div, "focus" isn't lost */
  boolean forceChoicesDivFocus;

  CodeWidgetInputPanels(DivElement choicesDiv, SimpleEntry simpleEntry, CodeWidgetCursorOverlay cursorOverlay, boolean forceChoicesDivFocus)
  {
    this.choicesDiv = choicesDiv;
    this.cursorOverlay = cursorOverlay;
    this.simpleEntry = simpleEntry;
    this.forceChoicesDivFocus = forceChoicesDivFocus;
  }
  
  void hideChoicesDiv()
  {
    choicesDiv.getStyle().setDisplay(Display.NONE);
  }
  
  void showChoicesDiv()
  {
    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    // Also assign focus to the coding area so that focus isn't lost
    if (forceChoicesDivFocus)
      choicesDiv.focus();
  }

  boolean isFocusInInputPanels(Node target)
  {
    while (target != null)
    {
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

  void hideSimpleEntry()
  {
    simpleEntry.setVisible(false);
  }
  
  void updateCursorVisibilityIfFocused(boolean hasFocus)
  {
    cursorOverlay.updateCursorVisibilityIfFocused(hasFocus);
  }
}
