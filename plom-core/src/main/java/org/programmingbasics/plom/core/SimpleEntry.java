package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.Token;

import com.google.gwt.regexp.shared.RegExp;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.css.CSSStyleDeclaration.Unit;
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
  DivElement suggestionsContainer;  // Outer container that scrolls
  DivElement suggestionsContentContainer;  // Actual suggestions content inside the scrolling container
  Token simpleEntryToken;  // token being edited by simple entry
  InputCallback<Token> callback;
  BackspaceAllCallback backspaceCallback;
  Runnable onKeyboardSwitchClick;
  String tokenPrefix = "";
  String tokenDisplayPrefix = "";  // Prefix that is shown in the UI (may be different than the prefix that is prepended to the token)
  String tokenPostfix = "";
  SuggesterClient suggester;
  boolean isEdit;
  Element doNotCoverEl;
  Element doNotCoverPaddingEl;
  String doNotCoverElOldRightPadding;
  double doNotCoverElOldScrollLeft;
  String currentText = "";
  
  SimpleEntry(DivElement el, DivElement suggestionsEl, DivElement suggestionsContentEl)
  {
    el.setInnerHTML(UIResources.INSTANCE.getSimpleEntryContentsHtml().getText());
    container = el;
    suggestionsContainer = suggestionsEl;
    suggestionsContentContainer = suggestionsContentEl;
    hookSimpleEntry(el);
  }
  
  // The side panel might show on the either the left or right side
  // depending 
  void setPositionRelativeTo(int x)
  {
    
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
      // If we scrolled the view so that it wouldn't be covered up by suggestions,
      // then we should scroll it back
      if (doNotCoverEl != null)
      {
        if (doNotCoverElOldRightPadding == null)
          doNotCoverPaddingEl.getStyle().clearPaddingRight();
        else
          doNotCoverPaddingEl.getStyle().setProperty("padding-right", doNotCoverElOldRightPadding);
        doNotCoverEl.setScrollLeft((int)doNotCoverElOldScrollLeft);
        doNotCoverEl = null;
      }
    }
  }

  void setEraseButtonVisible(boolean isVisible)
  {
    if (isVisible)
      container.querySelector(".simpleentry_erase").getStyle().setDisplay(Display.BLOCK);
    else
      container.querySelector(".simpleentry_erase").getStyle().setDisplay(Display.NONE);
  }

  void enableKeyboardSwitchButton(Runnable onClick)
  {
    container.querySelector(".simpleentry_kb").getStyle().setDisplay(Display.BLOCK);
    onKeyboardSwitchClick = onClick;
  }

  void hideKeyboardSwitchButton()
  {
    container.querySelector(".simpleentry_kb").getStyle().clearDisplay();
  }

  public void forceSafariBlur()
  {
    // Safari seems like it sometimes doesn't close the soft keyboard if the
    // user hits "return"/"enter" on the soft keyboard, so we'll force a blur to close it 
    container.querySelector("textarea").blur();
    container.querySelector("input").blur();
  }
  
  void simpleEntryInput(String val, boolean isFinal)
  {
    currentText = val;
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
  
  void refillSuggestions(String search)
  {
    if (suggester == null) 
    {
      suggestionsContainer.getStyle().setDisplay(Display.NONE);
      return;
    }
    else
    {
      suggestionsContainer.getStyle().clearDisplay();
    }
    suggester.gatherSuggestions(search, (suggestions) -> {
      // Check if callback came back too late, and the suggestions panel
      // isn't even on the screen any more
      if (!Browser.getDocument().contains(suggestionsContentContainer))
        return;
      suggestionsContentContainer.setInnerHTML("");
      Document doc = suggestionsContentContainer.getOwnerDocument();
      int maxSuggestions = 20;
      if (suggester.shouldShowAllSuggestions())
        maxSuggestions = 2000;
      for (int n = Math.min(maxSuggestions, suggestions.size() - 1); n >= 0; n--)
      {
        final String suggestionText = suggestions.get(n); 
        AnchorElement el = (AnchorElement)doc.createElement("a");
        el.setHref("#");
        DivElement div = doc.createDivElement();
        el.appendChild(div);
        div.setTextContent(suggestionText);
        suggestionsContentContainer.appendChild(el);
        el.addEventListener(Event.CLICK, (e) -> {
          e.preventDefault();
          simpleEntryInput(suggestionText, true);
        }, false);
      }
      // Scroll to bottom of the list
      suggestionsContainer.setScrollTop(suggestionsContainer.getScrollHeight());
      
    });
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
    // handle the erase button being pressed
    formEl.querySelector(".simpleentry_erase").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      if (inputEl.getStyle().getDisplay() != Display.NONE)
      {
        inputEl.setValue("");
        simpleEntryInput(inputEl.getValue(), false);
        refillSuggestions(inputEl.getValue());
        Main.forceFocusAndShowKeyboard(inputEl, false);
      }
      else
      {
        textAreaEl.setValue("");
        simpleEntryInput(textAreaEl.getValue(), false);
        Main.forceFocusAndShowKeyboard(textAreaEl, false);
      }
    }, false);
    // Keyboard switch button pressed
    formEl.querySelector(".simpleentry_kb").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      if (onKeyboardSwitchClick != null)
        onKeyboardSwitchClick.run();
    }, false);

    // handle text being typed into the input
    inputEl.addEventListener(Event.INPUT, (e) -> {
      simpleEntryInput(inputEl.getValue(), false);
      refillSuggestions(inputEl.getValue());
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
    // Bypass the default keyboard animation on Android and show/hide the keyboard faster
    Main.hookFastAndroidKeyboard(inputEl);
    Main.hookFastAndroidKeyboard(textAreaEl);
  }
  
  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    showFor(prefix, prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback,
        (InputElement)container.querySelector("input"),
        (TextAreaElement)container.querySelector("textarea"));
  }

  <U extends Token> void showFor(String displayPrefix, String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    showFor(displayPrefix, prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback,
        (InputElement)container.querySelector("input"),
        (TextAreaElement)container.querySelector("textarea"));
  }

  <U extends Token> void showMultilineFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    showFor(prefix, prefix, postfix, prompt, initialValue, token, isEdit, null, callback, bkspCallback,
        (TextAreaElement)container.querySelector("textarea"),
        (InputElement)container.querySelector("input"));
  }

  <U extends Token> void showFor(String displayPrefix, String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback, Element forInput, Element toHide)
  {
    if (prompt == null || prompt.isEmpty())
    {
      container.querySelector("span.prefix").setTextContent(displayPrefix);
      container.querySelector("span.postfix").setTextContent(postfix);
    }
    else
    {
      container.querySelector("span.prefix").setTextContent(prompt);
      container.querySelector("span.postfix").setTextContent("");
    }
    setEraseButtonVisible(false);
    setVisible(true);
    hideKeyboardSwitchButton();
    onKeyboardSwitchClick = null;
    toHide.getStyle().setDisplay(Display.NONE);
    forInput.getStyle().setDisplay(Display.INLINE);
    if (initialValue != null)
      ((InputElement)forInput).setValue(initialValue);
    else
      ((InputElement)forInput).setValue("");
    // In Safari, just setting focus won't cause the soft keyboard to trigger. I think a cursor also has to be placed to trigger the keyboard reliably.
    ((InputElement)forInput).setSelectionRange(((InputElement)forInput).getValue().length(), ((InputElement)forInput).getValue().length());
    Main.forceFocusAndShowKeyboard(forInput, false);
    simpleEntryToken = token;
    this.tokenPrefix = prefix;
    this.tokenDisplayPrefix = displayPrefix;
    this.tokenPostfix = postfix;
    this.isEdit = isEdit;
    this.suggester = suggester;
    this.callback = (InputCallback<Token>)callback;
    this.backspaceCallback = bkspCallback;
    suggestionsContentContainer.setInnerHTML("");
    refillSuggestions(initialValue);
    simpleEntryInput(initialValue, false);
  }

  public void scrollForDoNotCover(Element scrollableEl, Element extraPaddingEl, int doNotCoverLeftX, int doNotCoverRightX)
  {
    this.doNotCoverEl = scrollableEl;
    this.doNotCoverPaddingEl = extraPaddingEl;
    if (scrollableEl != null && suggester != null)
    {
      // If there is an element that we should ensure that a certain
      // part isn't covered up (because that's the part that the user is editing
      // and it might be confusing if it's covered up), then we will
      // try to scroll it so that it isn't covered up by any suggestions
      double sidePanelLeft = suggestionsContainer.getBoundingClientRect().getLeft();
      doNotCoverElOldRightPadding = scrollableEl.getStyle().getPropertyValue("padding-right");
      doNotCoverElOldScrollLeft = scrollableEl.getScrollLeft();
      double sidePanelWidth = suggestionsContainer.getBoundingClientRect().getWidth();
      int oldRightPadding = 0;
      String oldRightPaddingString = Browser.getWindow().getComputedStyle(extraPaddingEl, null).getPaddingRight();
      oldRightPadding = Integer.parseInt(RegExp.compile("[0-9]*").exec(oldRightPaddingString).getGroup(0));
      extraPaddingEl.getStyle().setPaddingRight(sidePanelWidth + oldRightPadding, Unit.PX);
      if (doNotCoverRightX - doNotCoverElOldScrollLeft > sidePanelLeft)
      {
        double newScrollLeft = doNotCoverRightX - sidePanelLeft;
        if (newScrollLeft > doNotCoverLeftX)
          newScrollLeft = doNotCoverLeftX;
        scrollableEl.setScrollLeft((int)newScrollLeft);
      }
    }
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
