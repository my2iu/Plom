package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.suggestions.Suggester;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.KeyboardEvent;
import elemental.html.AnchorElement;
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
  DivElement suggestionsContainer;
  Token simpleEntryToken;  // token being edited by simple entry
  InputCallback<Token> callback;
  String tokenPrefix = "";
  String tokenPostfix = "";
  Suggester suggester;
  boolean isEdit;
  
  SimpleEntry(DivElement el, DivElement suggestionsEl)
  {
    container = el;
    suggestionsContainer = suggestionsEl;
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
      callback.input(tokenPrefix + val + tokenPostfix, isFinal, simpleEntryToken, isEdit);
    }
  }

  void refillSuggestions()
  {
    suggestionsContainer.setInnerHTML("");
    if (suggester == null) return;
    List<String> suggestions = suggester.gatherSuggestions("");
    Document doc = suggestionsContainer.getOwnerDocument();
    for (int n = 0; n < Math.min(20, suggestions.size()); n++)
    {
      final String suggestionText = suggestions.get(n); 
      AnchorElement el = (AnchorElement)doc.createElement("a");
      el.setHref("#");
      el.setTextContent(suggestionText);
      suggestionsContainer.appendChild(el);
      el.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        simpleEntryInput(suggestionText, true);
      }, false);
      suggestionsContainer.appendChild(doc.createBRElement());
    }
  }
  
  private void hookSimpleEntry(DivElement simpleEntryDiv)
  {
    InputElement inputEl = (InputElement)simpleEntryDiv.querySelector("input");
    TextAreaElement textAreaEl = (TextAreaElement)simpleEntryDiv.querySelector("textarea");
    FormElement formEl = (FormElement)simpleEntryDiv.querySelector("form");
    // Catch the enter begin pressed
    formEl.addEventListener(Event.SUBMIT, (e) -> {
      e.preventDefault();
      simpleEntryInput(inputEl.getValue(), true);
    }, false);
    // handle the done button being pressed
    formEl.querySelector(".simpleentry_done").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      if (inputEl.getStyle().getDisplay() != Display.NONE)
        simpleEntryInput(inputEl.getValue(), true);
      else
        simpleEntryInput(textAreaEl.getValue(), true);
    }, false);
    // handle text being typed into the input
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
  
  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, Suggester suggester, InputCallback<U> callback)
  {
    showFor(prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback,
        (InputElement)container.querySelector("input"),
        (TextAreaElement)container.querySelector("textarea"));
  }

  <U extends Token> void showMultilineFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, InputCallback<U> callback)
  {
    showFor(prefix, postfix, prompt, initialValue, token, isEdit, null, callback,
        (TextAreaElement)container.querySelector("textarea"),
        (InputElement)container.querySelector("input"));
  }

  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, Suggester suggester, InputCallback<U> callback, Element forInput, Element toHide)
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
    if (initialValue != null)
      ((InputElement)forInput).setValue(initialValue);
    else
      ((InputElement)forInput).setValue("");
    simpleEntryToken = token;
    this.tokenPrefix = prefix;
    this.tokenPostfix = postfix;
    this.isEdit = isEdit;
    this.suggester = suggester;
    this.callback = (InputCallback<Token>)callback;
    refillSuggestions();
    simpleEntryInput(initialValue, false);
  }

  @FunctionalInterface static interface InputCallback<T extends Token>
  {
    void input(String val, boolean isFinal, T token, boolean isEdit);
  }
  
}
