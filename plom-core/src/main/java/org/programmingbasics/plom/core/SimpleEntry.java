package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.suggestions.Suggester;

import elemental.client.Browser;
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
  BackspaceAllCallback backspaceCallback;
  String tokenPrefix = "";
  String tokenPostfix = "";
  Suggester suggester;
  boolean isEdit;
  
  SimpleEntry(DivElement el, DivElement suggestionsEl)
  {
    el.setInnerHTML(UIResources.INSTANCE.getSimpleEntryContentsHtml().getText());
    container = el;
    suggestionsContainer = suggestionsEl;
    hookSimpleEntry(el);
  }
  
  void setVisible(boolean isVisible)
  {
    if (isVisible)
    {
      container.getStyle().setDisplay(Display.BLOCK);
    }
    else
    {
      suggestionsContainer.getStyle().setDisplay(Display.NONE);
      container.getStyle().setDisplay(Display.NONE);
    }
  }

  private void forceSafariBlur()
  {
    // Safari seems like it sometimes doesn't close the soft keyboard if the
    // user hits "return"/"enter" on the soft keyboard, so we'll force a blur to close it 
    container.querySelector("textarea").blur();
    container.querySelector("input").blur();
  }
  
  void simpleEntryInput(String val, boolean isFinal)
  {
    if (isFinal)
      forceSafariBlur();
      
    if (callback != null)
    {
      callback.input(tokenPrefix + val + tokenPostfix, isFinal, simpleEntryToken, isEdit);
    }
  }

  void deleteEntryInput()
  {
    if (backspaceCallback != null)
    {
      if (!backspaceCallback.bksp(isEdit))
      {
        forceSafariBlur();
      }
    }
  }
  
  void refillSuggestions()
  {
    suggestionsContainer.setInnerHTML("");
    if (suggester == null) 
    {
      suggestionsContainer.getStyle().setDisplay(Display.NONE);
      return;
    }
    else
    {
      suggestionsContainer.getStyle().setDisplay(Display.BLOCK);
    }
    List<String> suggestions = suggester.gatherSuggestions("");
    Document doc = suggestionsContainer.getOwnerDocument();
    for (int n = 0; n < Math.min(20, suggestions.size()); n++)
    {
      final String suggestionText = suggestions.get(n); 
      AnchorElement el = (AnchorElement)doc.createElement("a");
      el.setHref("#");
      DivElement div = doc.createDivElement();
      el.appendChild(div);
      div.setTextContent(suggestionText);
      suggestionsContainer.appendChild(el);
      el.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        simpleEntryInput(suggestionText, true);
      }, false);
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
      else if (keyEvt.getWhich() == 8)
      {
        // Intercept the backspace key being pressed so we can handle backspace being pressed on an empty token
        if (inputEl.getValue().isEmpty())
          deleteEntryInput();
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
      else if (keyEvt.getWhich() == 8)
      {
        // Intercept the backspace key being pressed so we can handle backspace being pressed on an empty token
        if (textAreaEl.getValue().isEmpty())
          deleteEntryInput();
      }
    }, false);
  }
  
  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, Suggester suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    showFor(prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback,
        (InputElement)container.querySelector("input"),
        (TextAreaElement)container.querySelector("textarea"));
  }

  <U extends Token> void showMultilineFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    showFor(prefix, postfix, prompt, initialValue, token, isEdit, null, callback, bkspCallback,
        (TextAreaElement)container.querySelector("textarea"),
        (InputElement)container.querySelector("input"));
  }

  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, Suggester suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback, Element forInput, Element toHide)
  {
    if (prompt == null || prompt.isEmpty())
    {
      container.querySelector("span.prefix").setTextContent(prefix);
      container.querySelector("span.postfix").setTextContent(postfix);
    }
    else
    {
      container.querySelector("span.prefix").setTextContent(prompt);
      container.querySelector("span.postfix").setTextContent("");
    }
    setVisible(true);
    toHide.getStyle().setDisplay(Display.NONE);
    forInput.getStyle().setDisplay(Display.INLINE);
    if (initialValue != null)
      ((InputElement)forInput).setValue(initialValue);
    else
      ((InputElement)forInput).setValue("");
    // In Safari, just setting focus won't cause the soft keyboard to trigger. I think a cursor also has to be placed to trigger the keyboard reliably.
    ((InputElement)forInput).setSelectionRange(((InputElement)forInput).getValue().length(), ((InputElement)forInput).getValue().length());
    forInput.focus();
    simpleEntryToken = token;
    this.tokenPrefix = prefix;
    this.tokenPostfix = postfix;
    this.isEdit = isEdit;
    this.suggester = suggester;
    this.callback = (InputCallback<Token>)callback;
    this.backspaceCallback = bkspCallback;
    refillSuggestions();
    simpleEntryInput(initialValue, false);
  }

  @FunctionalInterface static interface InputCallback<T extends Token>
  {
    void input(String val, boolean isFinal, T token, boolean isEdit);
  }
  @FunctionalInterface static interface BackspaceAllCallback
  {
    // Return false to signal the simple entry that it should close 
    boolean bksp(boolean isEdit);
  }
  
}
