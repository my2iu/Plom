package org.programmingbasics.plom.core;

/*
TODO:
- interpreter
- functions
- for loop
- number constants
- string constants
- variable names
- valign to middle
- nesting of blocks for functions
- next button
- keyboard movement
- predict variables
- keyboard entry
- better sorting of predicted symbols
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.EraseLeft;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.InsertNewLine;
import org.programmingbasics.plom.core.view.InsertToken;
import org.programmingbasics.plom.core.view.ParseContext;
import org.programmingbasics.plom.core.view.RenderedHitBox;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.FormElement;
import elemental.html.InputElement;

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    DivElement mainDiv = (DivElement)Browser.getDocument().querySelector("div.main");
    mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());

    codeDiv = (DivElement)mainDiv.querySelector("div.code");
    choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"));

    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    
    codeDiv.setInnerHTML("");
    renderTokens(codeDiv, codeList, cursorPos, null);
    showPredictedTokenInput(choicesDiv);
    hookCodeClick(codeDiv);
  }

  StatementContainer codeList = new StatementContainer();
  {
    Token.OneExpressionOneBlockToken ifToken = new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF);
    ifToken.expression.tokens.addAll(Arrays.asList(
        new Token.SimpleToken("1", Symbol.Number)));
    ifToken.block.statements.add(
        new TokenContainer(Arrays.asList(
            new Token.SimpleToken("1", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Number),
            new Token.SimpleToken("1", Symbol.Number)
            ))
        );
    codeList.statements.addAll(Arrays.asList(
        new TokenContainer(Arrays.asList(
            ifToken)),
        new TokenContainer(Arrays.asList()),
        new TokenContainer(Arrays.asList(
            new Token.SimpleToken("a", Symbol.Number),
            new Token.SimpleToken("=", Symbol.Number),
            new Token.SimpleToken("4", Symbol.Number)
            ))
        ));
  }
  DivElement codeDiv;
  DivElement choicesDiv;
  SimpleEntry simpleEntry;
  CodePosition cursorPos = new CodePosition();


  /**
   * Returns a mapping of divs for each line and their line numbers
   */
  static void renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
  {
    new CodeRenderer().render(codeDiv, codeList, pos, renderedHitBoxes);
  }

  Element makeButton(String text, boolean enabled, Runnable onclick)
  {
    Document doc = Browser.getDocument();
    DivElement div = doc.createDivElement();
    if (enabled)
      div.setClassName("tokenchoice");
    else
      div.setClassName("tokenchoicedisabled");
    div.setTextContent(text);
    if (enabled)
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.appendChild(div);
      a.addEventListener(Event.CLICK, (evt)-> {
        evt.preventDefault();
        onclick.run();
      }, false);
      return a;
    }
    else
    {
      return div;
    }
  }

  void insertToken(CodePosition pos, String tokenText, Symbol tokenType)
  {
    Token newToken;
    switch(tokenType)
    {
    case COMPOUND_IF:
    case COMPOUND_ELSEIF:
      newToken = new Token.OneExpressionOneBlockToken(tokenText, tokenType);
      break;
    case COMPOUND_ELSE:
      newToken = new Token.OneBlockToken(tokenText, tokenType);
      break;
    default:
      if (tokenType.isWide())
        newToken = new WideToken(tokenText, tokenType);
      else
        newToken = new SimpleToken(tokenText, tokenType);
      break;
    }
    InsertToken.insertTokenIntoStatementContainer(codeList, newToken, pos, 0);
    codeDiv.setInnerHTML("");
    renderTokens(codeDiv, codeList, cursorPos, null);
    switch (tokenType)
    {
    case DotVariable:
    case Number:
    case String:
    case DUMMY_COMMENT:
      showSimpleEntryForToken(newToken, false);
      break;
    default:
      showPredictedTokenInput(choicesDiv);
    }
  }

  void showSimpleEntryForToken(Token newToken, boolean isEdit)
  {
    if (newToken == null) return;
    Symbol tokenType = null;
    String initialValue = "";
    if (newToken instanceof Token.SimpleToken)
    {
      tokenType = ((Token.SimpleToken)newToken).type;
      initialValue = ((Token.SimpleToken)newToken).contents;
    }
    else if (newToken instanceof Token.WideToken)
    {
      tokenType = ((Token.WideToken)newToken).type;
      initialValue = ((Token.WideToken)newToken).contents;
    }
    
    switch (tokenType)
    {
    case DotVariable:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(1);
      simpleEntry.showFor(".", "", null, initialValue, newToken, this::simpleEntryInput);
      break;
    case Number:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      simpleEntry.showFor("", "", "number: ", "", newToken, this::simpleEntryInput);
      break;
    case String:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      if (isEdit)
        initialValue = initialValue.substring(1, initialValue.length() - 1);
      else
        initialValue = "";
      simpleEntry.showFor("\"", "\"", "", initialValue, newToken, this::simpleEntryInput);
      break;
    case DUMMY_COMMENT:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(3);
      simpleEntry.showMultilineFor("// ", "", "", initialValue, newToken, this::simpleEntryInput);
      break;
    default:
      return;
    }
  }
  
  void showPredictedTokenInput(DivElement choicesDiv)
  {
    choicesDiv.setInnerHTML("");
    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);

    // Parse the current statement up to the cursor position
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(codeList, cursorPos, 0);
    LL1Parser stmtParser = new LL1Parser();
    stmtParser.stack.add(parseContext.baseContext);
    for (Token tok: parseContext.tokens)
    {
      tok.visit(stmtParser);
    }
    Set<Symbol> allowedSymbols = stmtParser.allowedNextSymbols();

    // Buttons for next and enter
    choicesDiv.appendChild(makeButton("\u27a0", true, () -> {}));
    choicesDiv.appendChild(makeButton("\u232B", true, () -> {
      // Backspace
      EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null);
      showPredictedTokenInput(choicesDiv);
    })); 
    if (parseContext.baseContext != Symbol.ExpressionOnly)
    {
      choicesDiv.appendChild(makeButton("\u21b5", true, () -> {
        InsertNewLine.insertNewlineIntoStatementContainer(codeList, cursorPos, 0);

        codeDiv.setInnerHTML("");
        renderTokens(codeDiv, codeList, cursorPos, null);
        showPredictedTokenInput(choicesDiv);
      }));
    }
    
    // Just some random tokens for initial prototyping
    for (Symbol sym: allowedSymbols)
    {
      if (sym == Symbol.EndStatement) continue;
      boolean isValidSymbol = stmtParser.peekParseSymbol(sym);
      String text = "Unknown";
      switch(sym)
      {
      case Assignment: text = ":="; break;
      case Plus: text = "+"; break;
      case Minus: text = "-"; break;
      case Multiply: text = "*"; break;
      case Divide: text = "/"; break;
      case OpenParenthesis: text = "("; break;
      case ClosedParenthesis: text = ")"; break;
      case DUMMY_COMMENT: text = "//"; break;
      case DotVariable: text = "."; break;
      case Number: text = "123"; break;
      case String: text = "\"...\""; break;
      case COMPOUND_IF: text = "if"; break;
      case COMPOUND_ELSE: text = "else"; break;
      case COMPOUND_ELSEIF: text = "elseif"; break;
      default:
      }
      String tokenText = text;
      // Here we distinguish between allowed symbols and valid symbols because we
      // don't want the UI to keep changing all the time with the changes in context.
      // Instead we show all normally allowed symbols, but disable the ones that
      // aren't valid for this particular context
      if (isValidSymbol)
        choicesDiv.appendChild(makeButton(tokenText, true, () -> { insertToken(cursorPos, tokenText, sym); }));
      else
        choicesDiv.appendChild(makeButton(tokenText, false, () -> {  }));
    }
    
    // Edit button for certain tokens
    Token currentToken = GetToken.inStatements(codeList, cursorPos, 0);
    if (currentToken != null)
    {
      Symbol tokenType = null;
      if (currentToken instanceof Token.SimpleToken)
        tokenType = ((Token.SimpleToken)currentToken).type;
      else if (currentToken instanceof Token.WideToken)
        tokenType = ((Token.WideToken)currentToken).type;
      
      if (tokenType == Symbol.String || tokenType == Symbol.DUMMY_COMMENT)
      {
        choicesDiv.appendChild(makeButton("\u270e", true, () -> {
          showSimpleEntryForToken(currentToken, true);
        }));
      }
    }
  }

  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.CLICK, (evt)-> {
      MouseEvent mevt = (MouseEvent)evt;
      int x = mevt.getClientX() + div.getScrollLeft();
      int y = mevt.getClientY() + div.getScrollTop();

      CodePosition newPos = HitDetect.renderAndHitDetect(x, y, codeDiv, codeList, cursorPos);

      if (cursorPos != null)
      {
        cursorPos = newPos;
        codeDiv.setInnerHTML("");
        renderTokens(codeDiv, codeList, cursorPos, null);
        showPredictedTokenInput(choicesDiv);
      }
    }, false);
  }

  <U extends Token> void simpleEntryInput(String val, boolean isFinal, U token)
  {
    if (token == null) return;
    if (token instanceof Token.SimpleToken)
    {
      if (((Token.SimpleToken)token).type == Symbol.Number && val.isEmpty())
      {
        val = "0";
      }
      ((Token.SimpleToken)token).contents = val;
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null);
      
      if (isFinal)
      {
        choicesDiv.getStyle().setDisplay(Display.BLOCK);
        simpleEntry.setVisible(false);
        showPredictedTokenInput(choicesDiv);
      }
    }
    else if (token instanceof Token.WideToken && ((Token.WideToken)token).type == Symbol.DUMMY_COMMENT)
    {
      ((Token.WideToken)token).contents = val;
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null);
      
      if (isFinal)
      {
        choicesDiv.getStyle().setDisplay(Display.BLOCK);
        simpleEntry.setVisible(false);
        showPredictedTokenInput(choicesDiv);
      }
      
    }

  }
  
}