package org.programmingbasics.plom.core;

import java.util.HashMap;
import java.util.Map;

import org.programmingbasics.plom.core.SimpleEntry.BackspaceAllCallback;
import org.programmingbasics.plom.core.SimpleEntry.InputCallback;
import org.programmingbasics.plom.core.ast.Token;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;

/**
 * The default number entry UI on mobile isn't great for code, so
 * this is a software virtual number pad specifically for entering
 * numbers in code
 */
public class NumberEntry
{
  DivElement container;
  Element numberOutputEl;
  String enteredText = "";
//  DivElement suggestionsContainer;  // Outer container that scrolls
//  DivElement suggestionsContentContainer;  // Actual suggestions content inside the scrolling container
  Token simpleEntryToken;  // token being edited by simple entry
  InputCallback<Token> callback;
  BackspaceAllCallback backspaceCallback;
//  String tokenPrefix = "";
//  String tokenDisplayPrefix = "";  // Prefix that is shown in the UI (may be different than the prefix that is prepended to the token)
//  String tokenPostfix = "";
//  SuggesterClient suggester;
  boolean isEdit;
//  Element doNotCoverEl;
//  Element doNotCoverPaddingEl;
//  String doNotCoverElOldRightPadding;
//  double doNotCoverElOldScrollLeft;
  
  NumberEntry(DivElement el)
  {
    el.setInnerHTML(UIResources.INSTANCE.getNumberEntryContentsHtml().getText());
    container = el;
    numberOutputEl = el.querySelector(".numberpadgrid .numpadgrid_output");
    hookNumberEntry(el);
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
      container.getStyle().setDisplay(Display.NONE);
//      // If we scrolled the view so that it wouldn't be covered up by suggestions,
//      // then we should scroll it back
//      if (doNotCoverEl != null)
//      {
//        if (doNotCoverElOldRightPadding == null)
//          doNotCoverPaddingEl.getStyle().clearPaddingRight();
//        else
//          doNotCoverPaddingEl.getStyle().setProperty("padding-right", doNotCoverElOldRightPadding);
//        doNotCoverEl.setScrollLeft((int)doNotCoverElOldScrollLeft);
//        doNotCoverEl = null;
//      }
    }
  }

//  void setEraseButtonVisible(boolean isVisible)
//  {
//    if (isVisible)
//      container.querySelector(".simpleentry_erase").getStyle().setDisplay(Display.BLOCK);
//    else
//      container.querySelector(".simpleentry_erase").getStyle().setDisplay(Display.NONE);
//  }
  
//  public void forceSafariBlur()
//  {
//    // Safari seems like it sometimes doesn't close the soft keyboard if the
//    // user hits "return"/"enter" on the soft keyboard, so we'll force a blur to close it 
//    container.querySelector("textarea").blur();
//    container.querySelector("input").blur();
//  }
  
  void simpleEntryInput(String val, boolean isFinal)
  {
    if (callback != null)
    {
      callback.input(val, isFinal, simpleEntryToken, isEdit);
//      callback.input(tokenPrefix + val + tokenPostfix, isFinal, simpleEntryToken, isEdit);
    }
  }

  void deleteEntryInput()
  {
    if (backspaceCallback != null)
    {
      if (!backspaceCallback.bksp(isEdit))
      {
//        forceSafariBlur();
      }
    }
  }
  
  static private Map<String, String> cssGridMap = new HashMap<>();
  static {
    cssGridMap.put("numpadgrid_7", "7");
    cssGridMap.put("numpadgrid_8", "8");
    cssGridMap.put("numpadgrid_9", "9");
    cssGridMap.put("numpadgrid_4", "4");
    cssGridMap.put("numpadgrid_5", "5");
    cssGridMap.put("numpadgrid_6", "6");
    cssGridMap.put("numpadgrid_1", "1");
    cssGridMap.put("numpadgrid_2", "2");
    cssGridMap.put("numpadgrid_3", "3");
    cssGridMap.put("numpadgrid_0", "0");
    cssGridMap.put("numpadgrid_dot", ".");
    cssGridMap.put("numpadgrid_plusminus", "\u207a\u2215\u208b");
    cssGridMap.put("numpadgrid_kb", "\u2328");

    cssGridMap.put("numpadgrid_enter", "\u21b5");
    cssGridMap.put("numpadgrid_bksp", "\u232B");

  }
  
  void configureNumPadButton(AnchorElement a, Element refocusElement, Runnable onclick)
  {
    a.setHref("#");
    CodeWidgetBase.addActiveEventListenerTo(a, "pointerdown", (evt) -> {
      // There's some weird bug in ios14 that I can't figure out
      // where after the pointer events for dragging some handles to
      // make a selection, if I then create a new anchor element
      // (i.e. a Copy button), then the first touch afterwards will
      // not result in any click events. 
      //
      // Creating this empty pointer event listener on the button
      // does seem to convince ios14 to let the click event occur
      // on the first touch (instead of requiring two touches). I
      // don't understand why, but it seems to work.
    }, false);
    a.addEventListener(Event.CLICK, (evt)-> {
      evt.preventDefault();
      // After a click, we often remove all the buttons, which causes
      // focus to be lost, which we want to manage. Instead, we force 
      // focus onto a known element (usually the entire div that holds
      // all the choices). 
      container.focus();
      onclick.run();
    }, false);
  }
  
  private void hookNumberEntry(DivElement numberEntryDiv)
  {
    Element gridEl = numberEntryDiv.querySelector(".numberpadgrid");
    for (Map.Entry<String, String> classToText: cssGridMap.entrySet())
    {
      final String keyText = classToText.getValue();
      AnchorElement a = (AnchorElement)gridEl.querySelector("." + classToText.getKey());
      a.setTextContent(keyText);
      switch (classToText.getKey())
      {
      case "numpadgrid_plusminus":
        configureNumPadButton(a, container, () -> {
          if (!enteredText.isEmpty() && enteredText.charAt(0) == '-')
            enteredText = enteredText.substring(1);
          else
            enteredText = "-" + enteredText;
          numberOutputEl.setTextContent(enteredText);
          simpleEntryInput(enteredText, false);
        });
        break;
      case "numpadgrid_kb":
        configureNumPadButton(a, container, () -> {
          if (onKeyboardSwitchClick != null)
            onKeyboardSwitchClick.run();
        });
        break;
      case "numpadgrid_enter":
        configureNumPadButton(a, container, () -> {
          simpleEntryInput(enteredText, true);
        });
        break;
      case "numpadgrid_bksp":
        configureNumPadButton(a, container, () -> {
          if (!enteredText.isEmpty()) 
          {
            int lastIndex = enteredText.length() - 1;
            for (; lastIndex >= 0; lastIndex--)
            {
              if (Character.isHighSurrogate(enteredText.charAt(lastIndex)))
                break;
              if (!Character.isLowSurrogate(enteredText.charAt(lastIndex)))
                break;
            }
            enteredText = enteredText.substring(0, lastIndex);
            numberOutputEl.setTextContent(enteredText);
            simpleEntryInput(enteredText, false);
          }
          else
          {
            deleteEntryInput();
          }
          
        });
        break;
      default:
        configureNumPadButton(a, container, () -> {
          enteredText += keyText;
          numberOutputEl.setTextContent(enteredText);
          simpleEntryInput(enteredText, false);
        });
        break;
      }
    }
//    for (String buttonText: buttonGridText)
//    {
//      Element el = CodeWidgetBase.makeButton(buttonText, true, container, () -> {
//        
//      });
//      gridEl.appendChild(el);
//    }
//    InputElement inputEl = (InputElement)simpleEntryDiv.querySelector("input");
//    TextAreaElement textAreaEl = (TextAreaElement)simpleEntryDiv.querySelector("textarea");
//    FormElement formEl = (FormElement)simpleEntryDiv.querySelector("form");
//    // Catch the enter begin pressed
//    formEl.addEventListener(Event.SUBMIT, (e) -> {
//      e.preventDefault();
//      simpleEntryInput(inputEl.getValue(), true);
//    }, false);
//    // handle the done button being pressed
//    formEl.querySelector(".simpleentry_done").addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//      if (inputEl.getStyle().getDisplay() != Display.NONE)
//        simpleEntryInput(inputEl.getValue(), true);
//      else
//        simpleEntryInput(textAreaEl.getValue(), true);
//    }, false);
//    // handle the erase button being pressed
//    formEl.querySelector(".simpleentry_erase").addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//      if (inputEl.getStyle().getDisplay() != Display.NONE)
//      {
//        inputEl.setValue("");
//        simpleEntryInput(inputEl.getValue(), false);
//        Main.forceFocusAndShowKeyboard(inputEl, false);
//      }
//      else
//      {
//        textAreaEl.setValue("");
//        simpleEntryInput(textAreaEl.getValue(), false);
//        Main.forceFocusAndShowKeyboard(textAreaEl, false);
//      }
//    }, false);
//    // handle text being typed into the input
//    inputEl.addEventListener(Event.INPUT, (e) -> {
//      simpleEntryInput(inputEl.getValue(), false);
//    }, false);
//    inputEl.addEventListener(Event.KEYDOWN, (e) -> {
//      KeyboardEvent keyEvt = (KeyboardEvent)e;
//      if (keyEvt.getWhich() == 9)  // Capture tab key presses
//      {
//        simpleEntryInput(inputEl.getValue(), true);
//        e.preventDefault();
//      }
//      else if (keyEvt.getWhich() == 8)
//      {
//        // Intercept the backspace key being pressed so we can handle backspace being pressed on an empty token
//        if (inputEl.getValue().isEmpty())
//          deleteEntryInput();
//      }
//    }, false);
//    // For mulit-line inputs, we use a text area, which we hook the same way
//    // as the input element
//    textAreaEl.addEventListener(Event.INPUT, (e) -> {
//      simpleEntryInput(textAreaEl.getValue(), false);
//    }, false);
//    textAreaEl.addEventListener(Event.KEYDOWN, (e) -> {
//      KeyboardEvent keyEvt = (KeyboardEvent)e;
//      if (keyEvt.getWhich() == 9)  // Capture tab key presses
//      {
//        simpleEntryInput(textAreaEl.getValue(), true);
//        e.preventDefault();
//      }
//      else if (keyEvt.getWhich() == 8)
//      {
//        // Intercept the backspace key being pressed so we can handle backspace being pressed on an empty token
//        if (textAreaEl.getValue().isEmpty())
//          deleteEntryInput();
//      }
//    }, false);
//    // Bypass the default keyboard animation on Android and show/hide the keyboard faster
//    Main.hookFastAndroidKeyboard(inputEl);
//    Main.hookFastAndroidKeyboard(textAreaEl);
  }

//  <U extends Token> void showFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
//  {
//    showFor(prefix, prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback,
//        (InputElement)container.querySelector("input"),
//        (TextAreaElement)container.querySelector("textarea"));
//  }
//
//  <U extends Token> void showFor(String displayPrefix, String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
//  {
//    showFor(displayPrefix, prefix, postfix, prompt, initialValue, token, isEdit, suggester, callback, bkspCallback,
//        (InputElement)container.querySelector("input"),
//        (TextAreaElement)container.querySelector("textarea"));
//  }
//
//  <U extends Token> void showMultilineFor(String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
//  {
//    showFor(prefix, prefix, postfix, prompt, initialValue, token, isEdit, null, callback, bkspCallback,
//        (TextAreaElement)container.querySelector("textarea"),
//        (InputElement)container.querySelector("input"));
//  }
//
//  <U extends Token> void showFor(String displayPrefix, String prefix, String postfix, String prompt, String initialValue, U token, boolean isEdit, SuggesterClient suggester, InputCallback<U> callback, BackspaceAllCallback bkspCallback, Element forInput, Element toHide)
//  {
//    if (prompt == null || prompt.isEmpty())
//    {
//      container.querySelector("span.prefix").setTextContent(displayPrefix);
//      container.querySelector("span.postfix").setTextContent(postfix);
//    }
//    else
//    {
//      container.querySelector("span.prefix").setTextContent(prompt);
//      container.querySelector("span.postfix").setTextContent("");
//    }
////    setEraseButtonVisible(false);
//    setVisible(true);
//    toHide.getStyle().setDisplay(Display.NONE);
//    forInput.getStyle().setDisplay(Display.INLINE);
//    if (initialValue != null)
//      ((InputElement)forInput).setValue(initialValue);
//    else
//      ((InputElement)forInput).setValue("");
//    // In Safari, just setting focus won't cause the soft keyboard to trigger. I think a cursor also has to be placed to trigger the keyboard reliably.
//    ((InputElement)forInput).setSelectionRange(((InputElement)forInput).getValue().length(), ((InputElement)forInput).getValue().length());
//    Main.forceFocusAndShowKeyboard(forInput, false);
//    simpleEntryToken = token;
//    this.tokenPrefix = prefix;
//    this.tokenDisplayPrefix = displayPrefix;
//    this.tokenPostfix = postfix;
//    this.isEdit = isEdit;
//    this.suggester = suggester;
//    this.callback = (InputCallback<Token>)callback;
//    this.backspaceCallback = bkspCallback;
//    simpleEntryInput(initialValue, false);
//  }

  Runnable onKeyboardSwitchClick;

  void enableKeyboardSwitchButton(Runnable onClick)
  {
    container.querySelector(".numberpadgrid .numpadgrid_kb").getStyle().setDisplay(Display.BLOCK);
    onKeyboardSwitchClick = onClick;
  }

  void hideKeyboardSwitchButton()
  {
    container.querySelector(".numberpadgrid .numpadgrid_kb").getStyle().clearDisplay();
    onKeyboardSwitchClick = null;
  }

  
  <U extends Token> void showFor(String initialValue, U token, boolean isEdit, InputCallback<U> callback, BackspaceAllCallback bkspCallback)
  {
    setVisible(true);
    hideKeyboardSwitchButton();
    onKeyboardSwitchClick = null;
//    container.
//    forInput.getStyle().setDisplay(Display.INLINE);
    if (initialValue != null)
      enteredText = initialValue;
    else
      enteredText = "";
    numberOutputEl.setTextContent(enteredText);
//    // In Safari, just setting focus won't cause the soft keyboard to trigger. I think a cursor also has to be placed to trigger the keyboard reliably.
//    ((InputElement)forInput).setSelectionRange(((InputElement)forInput).getValue().length(), ((InputElement)forInput).getValue().length());
//    Main.forceFocusAndShowKeyboard(forInput, false);
    simpleEntryToken = token;
    this.isEdit = isEdit;
    this.callback = (InputCallback<Token>)callback;
    this.backspaceCallback = bkspCallback;
    simpleEntryInput(initialValue, false);
  }

  
//  public void scrollForDoNotCover(Element scrollableEl, Element extraPaddingEl, int doNotCoverLeftX, int doNotCoverRightX)
//  {
//    this.doNotCoverEl = scrollableEl;
//    this.doNotCoverPaddingEl = extraPaddingEl;
//    if (scrollableEl != null && suggester != null)
//    {
//      // If there is an element that we should ensure that a certain
//      // part isn't covered up (because that's the part that the user is editing
//      // and it might be confusing if it's covered up), then we will
//      // try to scroll it so that it isn't covered up by any suggestions
//      doNotCoverElOldRightPadding = scrollableEl.getStyle().getPropertyValue("padding-right");
//      doNotCoverElOldScrollLeft = scrollableEl.getScrollLeft();
//      int oldRightPadding = 0;
//      String oldRightPaddingString = Browser.getWindow().getComputedStyle(extraPaddingEl, null).getPaddingRight();
//      oldRightPadding = Integer.parseInt(RegExp.compile("[0-9]*").exec(oldRightPaddingString).getGroup(0));
//      extraPaddingEl.getStyle().setPaddingRight(sidePanelWidth + oldRightPadding, Unit.PX);
//      if (doNotCoverRightX - doNotCoverElOldScrollLeft > sidePanelLeft)
//      {
//        double newScrollLeft = doNotCoverRightX - sidePanelLeft;
//        if (newScrollLeft > doNotCoverLeftX)
//          newScrollLeft = doNotCoverLeftX;
//        scrollableEl.setScrollLeft((int)newScrollLeft);
//      }
//    }
//  }
//  
}
