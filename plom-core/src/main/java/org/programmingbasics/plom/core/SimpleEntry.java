package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.Token;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.events.Event;
import elemental.events.KeyboardEvent;
import elemental.html.DivElement;
import elemental.html.FormElement;
import elemental.html.InputElement;

/**
 * Holds the logic and variables for the input box where programmers
 * type variable names and constants
 */
public class SimpleEntry
{
  DivElement container;
  Token simpleEntryToken;  // token being edited by simple entry
  InputCallback<Token> callback;
  String tokenPrefix = "";
  
  SimpleEntry(DivElement el)
  {
    container = el;
    hookSimpleEntry(el);
  }
  
  void setVisible(boolean isVisible)
  {
    if (isVisible)
      container.getStyle().setDisplay(Display.BLOCK);
    else
      container.getStyle().setDisplay(Display.NONE);
  }
  
  void simpleEntryInput(String val, boolean isFinal)
  {
    if (callback != null)
    {
      callback.input(tokenPrefix + val, isFinal, simpleEntryToken);
    }
  }

  private void hookSimpleEntry(DivElement simpleEntryDiv)
  {
    InputElement inputEl = (InputElement)simpleEntryDiv.querySelector("input");
    FormElement formEl = (FormElement)simpleEntryDiv.querySelector("form");
    formEl.addEventListener(Event.SUBMIT, (e) -> {
      e.preventDefault();
      simpleEntryInput(inputEl.getValue(), true);
    }, false);
    inputEl.addEventListener(Event.INPUT, (e) -> {
      simpleEntryInput(inputEl.getValue(), false);
    }, false);
    inputEl.addEventListener(Event.KEYDOWN, (e) -> {
      KeyboardEvent keyEvt = (KeyboardEvent)e;
      if (keyEvt.getWhich() == 9)  // Capture tab key presses
      {
        simpleEntryInput(inputEl.getValue(), true);
        e.preventDefault();
      }
    }, false);
  }
  
  <U extends Token> void showFor(String prefix, String postfix, String tokenValuePrefix, U token, InputCallback<U> callback)
  {
    container.querySelector("span.prefix").setTextContent(prefix);
    setVisible(true);
    InputElement inputEl = (InputElement)container.querySelector("input");
    inputEl.focus();
    inputEl.setValue("");
    simpleEntryToken = token;
    this.tokenPrefix = tokenValuePrefix;
    this.callback = (InputCallback<Token>)callback;
  }

  @FunctionalInterface static interface InputCallback<T extends Token>
  {
    void input(String val, boolean isFinal, T token);
  }
}
