package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.SimpleEntry.BackspaceAllCallback;
import org.programmingbasics.plom.core.SimpleEntry.InputCallback;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.suggestions.Suggester;

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
  
  // Current code widget that has focus (or null if nothing has focus)
  private CodeWidgetBase current;
  
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
  
  void showChoicesDivAndTakeFocus(CodeWidgetBase currentFocus)
  {
    setCurrent(currentFocus);
    hideSimpleEntry();
    updateCursorForScroll();
    updateCursorVisibilityIfFocused();
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

  /** 
   * Changes the focus away from the old coding area, hiding input panels, if  
   * the old coding area currently has focus. If the old coding area doesn't 
   * have focus (e.g. the user switched focus to another coding area), then it 
   * doesn't do anything. 
   */
  void loseFocusFrom(CodeWidgetBase old)
  {
    if (getCurrent() == old)
    {
      hideChoicesDiv();
      // Careful here. On losing focus, the simple entry will close
      // but won't fire an event saying that text entry has completed
      hideSimpleEntry();
      setCurrent(null);
      updateCursorVisibilityIfFocused();
    }
  }
  
  <U extends Token> void showSimpleEntryFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    // A simple entry is triggered from the choices div, and the choices div
    // automatically shows the cursor overlay, so the only visibility that
    // needs to change when starting up the simpleEntry is to hide the
    // choices div
    hideChoicesDiv();

    simpleEntry.showFor(prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback);
  }

  <U extends Token> void showSimpleEntryFor(String displayPrefix, String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    // A simple entry is triggered from the choices div, and the choices div
    // automatically shows the cursor overlay, so the only visibility that
    // needs to change when starting up the simpleEntry is to hide the
    // choices div
    hideChoicesDiv();

    simpleEntry.showFor(displayPrefix, prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback);
  }

  <U extends Token> void showSimpleEntryMultilineFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    // A simple entry is triggered from the choices div, and the choices div
    // automatically shows the cursor overlay, so the only visibility that
    // needs to change when starting up the simpleEntry is to hide the
    // choices div
    hideChoicesDiv();

    simpleEntry.showMultilineFor(prefix, postfix, prompt, initialValue, token, isEdit, callback, bkspCallback);
  }

  void hideSimpleEntry()
  {
    simpleEntry.setVisible(false);
  }
  
  void updateCursorVisibilityIfFocused()
  {
    cursorOverlay.updateCursorVisibilityIfFocused(getCurrent() != null);
  }

  void updateCursorForScroll()
  {
    if (getCurrent() != null)
      current.updateForScroll(this.cursorOverlay);
  }
  
  CodeWidgetBase getCurrent()
  {
    return current;
  }

  void setCurrent(CodeWidgetBase current)
  {
    this.current = current;
    cursorOverlay.codeWidget = current;
  }
}
