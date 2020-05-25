package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.Token;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.KeyboardEvent;
import elemental.html.DivElement;
import elemental.html.FormElement;
import elemental.html.InputElement;
import elemental.html.TextAreaElement;

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
  String tokenPostfix = "";
  
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
      callback.input(tokenPrefix + val + tokenPostfix, isFinal, simpleEntryToken);
    }
  }

  private void hookSimpleEntry(DivElement simpleEntryDiv)
  {
    InputElement inputEl = (InputElement)simpleEntryDiv.querySelector("input");
    TextAreaElement textAreaEl = (TextAreaElement)simpleEntryDiv.querySelector("textarea");
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
    // For mulit-line inputs, we use a text area, which we hook the same way
    // as the input element
    textAreaEl.addEventListener(Event.INPUT, (e) -> {
      simpleEntryInput(textAreaEl.getValue(), false);
    }, false);
    textAreaEl.addEventListener(Event.KEYDOWN, (e) -> {
      KeyboardEvent keyEvt = (KeyboardEvent)e;
      if (keyEvt.getWhich() == 9)  // Capture tab key presses
      {
        simpleEntryInput(textAreaEl.getValue(), true);
        e.preventDefault();
      }
    }, false);
  }
  
  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialDisplayValue, U token, InputCallback<U> callback)
  {
    showFor(prefix, postfix, prompt, initialDisplayValue, token, callback,
        (InputElement)container.querySelector("input"),
        (TextAreaElement)container.querySelector("textarea"));
  }

  <U extends Token> void showMultilineFor(String prefix, String postfix, String prompt, String initialDisplayValue, U token, InputCallback<U> callback)
  {
    showFor(prefix, postfix, prompt, initialDisplayValue, token, callback,
        (TextAreaElement)container.querySelector("textarea"),
        (InputElement)container.querySelector("input"));
  }

  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialDisplayValue, U token, InputCallback<U> callback, Element forInput, Element toHide)
  {
    if (prompt == null || prompt.isEmpty())
    {
      container.querySelector("span.prefix").setTextContent(prefix);
      container.querySelector("span.postfix").setTextContent(postfix);
    }
    else
      container.querySelector("span.prefix").setTextContent(prompt);
    setVisible(true);
    toHide.getStyle().setDisplay(Display.NONE);
    forInput.getStyle().setDisplay(Display.INLINE);
    forInput.focus();
    ((InputElement)forInput).setValue("");
    simpleEntryToken = token;
    this.tokenPrefix = prefix;
    this.tokenPostfix = postfix;
    this.callback = (InputCallback<Token>)callback;
    simpleEntryInput(initialDisplayValue, false);
  }

  @FunctionalInterface static interface InputCallback<T extends Token>
  {
    void input(String val, boolean isFinal, T token);
  }
}
