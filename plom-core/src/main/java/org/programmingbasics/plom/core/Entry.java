package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.Suggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.EraseLeft;
import org.programmingbasics.plom.core.view.ErrorList;
import org.programmingbasics.plom.core.view.GatherCodeCompletionInfo;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.InsertNewLine;
import org.programmingbasics.plom.core.view.InsertToken;
import org.programmingbasics.plom.core.view.NextPosition;
import org.programmingbasics.plom.core.view.ParseContext;
import org.programmingbasics.plom.core.view.RenderedHitBox;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.html.ClientRect;
import elemental.html.DivElement;

/*
TODO:
- functions
- number constants (change inputmode to numeric)
- string constants (allow multi-line strings?)
- valign to middle
- nesting of blocks for functions
- keyboard movement
- predict variables
- keyboard entry
- variable declaration with assignment 
- adding a newline in the middle of a function call
 */

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    DivElement mainDiv = (DivElement)Browser.getDocument().querySelector("div.main");
    mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());

    codeDiv = (DivElement)mainDiv.querySelector("div.code");
    choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));

    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    
    updateErrorList();
    codeDiv.setInnerHTML("");
    renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
    showPredictedTokenInput(choicesDiv);
    hookCodeClick(codeDiv);
    
    // Need to have a basic way to run code initially in order to get a better
    // feel for the design of the programming language
    hookRun();
  }

  StatementContainer codeList = new StatementContainer(
      new TokenContainer(
          new Token.SimpleToken("var", Symbol.Var),
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
          new Token.SimpleToken(":", Symbol.Colon),
          Token.ParameterToken.fromContents(".string", Symbol.DotVariable)
          ),
      new TokenContainer(
          Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
          new Token.SimpleToken(":=", Symbol.Assignment),
          Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
              new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
          ),
      new TokenContainer(
          new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
              new TokenContainer(
                  Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                  new Token.SimpleToken("==", Symbol.Eq),
                  new Token.SimpleToken("\"8\"", Symbol.String)
                  ),
              new StatementContainer(
                  new TokenContainer(
                      Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                          new TokenContainer(
                              new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
                              ))
                      ))
              ),
          new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
              new StatementContainer(
                  new TokenContainer(
                      Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                          new TokenContainer(
                              new Token.SimpleToken("\"Incorrect\"", Symbol.String)
                              ))
                      ))
              )
          )
      );
  DivElement codeDiv;
  DivElement choicesDiv;
  SimpleEntry simpleEntry;
  CodePosition cursorPos = new CodePosition();
  ErrorList codeErrors = new ErrorList();
  {
    codeErrors.add(ParseToAst.ParseException.forToken(codeList.statements.get(0).tokens.get(1)));
    codeErrors.add(ParseToAst.ParseException.forEnd(codeList.statements.get(1).tokens.get(2)));
  }

  // To ensure that predicted buttons end up in a consistent order and
  // with the most important ones showing first, we have a map with priorities
  // for each button
  static Map<Symbol, Integer> buttonOrderPriority = new HashMap<>();
  static {
    Symbol[] symbolOrder = new Symbol[] {
        Symbol.DotVariable,
        Symbol.Number,
        Symbol.String,
        Symbol.TrueLiteral,
        Symbol.FalseLiteral,
        Symbol.Assignment,
        Symbol.Plus,
        Symbol.Minus,
        Symbol.Multiply,
        Symbol.Divide,
        Symbol.Eq,
        Symbol.Ne,
        Symbol.Lt,
        Symbol.Le,
        Symbol.Ge,
        Symbol.Gt,
        Symbol.OpenParenthesis,
        Symbol.ClosedParenthesis,
        Symbol.DUMMY_COMMENT,
        Symbol.Var,
        Symbol.COMPOUND_WHILE,
        Symbol.COMPOUND_IF,
        Symbol.COMPOUND_ELSEIF,
        Symbol.COMPOUND_ELSE,
    };
    for (int n = 0; n < symbolOrder.length; n++)
      buttonOrderPriority.put(symbolOrder[n], n);
  }
  
  /**
   * Returns a mapping of divs for each line and their line numbers
   */
  static void renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes, ErrorList codeErrors)
  {
    new CodeRenderer().render(codeDiv, codeList, pos, renderedHitBoxes, codeErrors);
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
    // Figure out some context to see if it can be used to narrow possible
    // options to show some more
    
    // Parse the current statement up to the cursor position
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(codeList, cursorPos, 0);
    LL1Parser stmtParser = new LL1Parser();
    stmtParser.addToParse(parseContext.baseContext);
    for (Token tok: parseContext.tokens)
    {
      tok.visit(stmtParser);
    }
    List<Symbol> parentSymbols = stmtParser.peekExpandedSymbols(tokenType);
    
    // Create an appropriate token object
    Token newToken;
    switch(tokenType)
    {
    case COMPOUND_IF:
    case COMPOUND_ELSEIF:
    case COMPOUND_WHILE:
      newToken = new Token.OneExpressionOneBlockToken(tokenText, tokenType);
      break;
    case COMPOUND_ELSE:
      newToken = new Token.OneBlockToken(tokenText, tokenType);
      break;
    case DotVariable:
      newToken = new Token.ParameterToken(
          Token.ParameterToken.splitVarAtColons(tokenText), 
          Token.ParameterToken.splitVarAtColonsForPostfix(tokenText), 
          tokenType);
      break;
    default:
      if (tokenType.isWide())
        newToken = new WideToken(tokenText, tokenType);
      else
        newToken = new SimpleToken(tokenText, tokenType);
      break;
    }
    InsertToken.insertTokenIntoStatementContainer(codeList, newToken, pos, 0);
    switch (tokenType)
    {
    case DotVariable:
      if (parentSymbols.contains(Symbol.DotType))
      {
        Suggester suggester = (prefix) -> {
          List<String> toReturn = new ArrayList<>();
          toReturn.add("string");
          toReturn.add("number");
          toReturn.add("object");
          toReturn.add("boolean");
          return toReturn;
        };
        showSimpleEntryForToken(newToken, false, suggester);
      }
      else if (parentSymbols.contains(Symbol.DotDeclareIdentifier))
      {
        showSimpleEntryForToken(newToken, false, null);
      }
      else
      {
        CodeCompletionContext suggestionContext = new CodeCompletionContext();
        StandardLibrary.createGlobals(null, suggestionContext.currentScope());
        GatherCodeCompletionInfo.fromStatements(codeList, suggestionContext, pos, 0);
        VariableSuggester suggester = new VariableSuggester(suggestionContext);
        showSimpleEntryForToken(newToken, false, suggester);
      }
      break;

    case Number:
    case String:
    case DUMMY_COMMENT:
      showSimpleEntryForToken(newToken, false, null);
      break;
    default:
      NextPosition.nextPositionOfStatements(codeList, pos, 0);
      showPredictedTokenInput(choicesDiv);
      break;
    }
    updateErrorList();
    codeDiv.setInnerHTML("");
    renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
  }

  void showSimpleEntryForToken(Token newToken, boolean isEdit, Suggester suggester)
  {
    if (newToken == null) return;
    Symbol tokenType = null;
    String initialValue = "";
    if (newToken instanceof Token.TokenWithSymbol)
      tokenType = ((Token.TokenWithSymbol)newToken).getType();
    if (newToken instanceof Token.TokenWithEditableTextContent)
      initialValue = ((Token.TokenWithEditableTextContent)newToken).getTextContent();
    
    switch (tokenType)
    {
    case DotVariable:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(1);
      simpleEntry.showFor(".", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput);
      break;
    case Number:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      simpleEntry.showFor("", "", "number: ", "", newToken, isEdit, suggester, this::simpleEntryInput);
      break;
    case String:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      if (isEdit)
        initialValue = initialValue.substring(1, initialValue.length() - 1);
      else
        initialValue = "";
      simpleEntry.showFor("\"", "\"", "", initialValue, newToken, isEdit, suggester, this::simpleEntryInput);
      break;
    case DUMMY_COMMENT:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(3);
      simpleEntry.showMultilineFor("// ", "", "", initialValue, newToken, isEdit, this::simpleEntryInput);
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
    stmtParser.addToParse(parseContext.baseContext);
    for (Token tok: parseContext.tokens)
    {
      tok.visit(stmtParser);
    }
    Set<Symbol> allowedSymbols = stmtParser.allowedNextSymbols();

    // Buttons for next, backspace, and enter
    // Next button
    choicesDiv.appendChild(makeButton("\u27a0", true, () -> {
      NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
      showPredictedTokenInput(choicesDiv);
    }));
    // Backspace button
    choicesDiv.appendChild(makeButton("\u232B", true, () -> {
      EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
      updateErrorList();
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
      showPredictedTokenInput(choicesDiv);
    })); 
    // Edit button for certain tokens
    Token currentToken = GetToken.inStatements(codeList, cursorPos, 0);
    boolean showEditButton = false;
    if (currentToken != null)
    {
      Symbol tokenType = null;
      if (currentToken instanceof Token.TokenWithSymbol)
        tokenType = ((Token.TokenWithSymbol)currentToken).getType();
      
      if (tokenType == Symbol.String || tokenType == Symbol.DUMMY_COMMENT)
        showEditButton = true;
    }
    if (showEditButton)
    {
      choicesDiv.appendChild(makeButton("\u270e", true, () -> {
        showSimpleEntryForToken(currentToken, true, null);
      }));
    }
    else
      choicesDiv.appendChild(makeButton("\u270e", false, () -> {  }));
    // newline button
    if (parseContext.baseContext != Symbol.ExpressionOnly)
    {
      choicesDiv.appendChild(makeButton("\u21b5", true, () -> {
        InsertNewLine.insertNewlineIntoStatementContainer(codeList, cursorPos, 0);

        updateErrorList();
        codeDiv.setInnerHTML("");
        renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
        showPredictedTokenInput(choicesDiv);
      }));
    }

    
    // Just some random tokens for initial prototyping
    List<Symbol> sortedAllowedSymbols = new ArrayList<>(allowedSymbols);
    sortedAllowedSymbols.sort(Comparator.comparing(s -> buttonOrderPriority.containsKey(s) ? buttonOrderPriority.get(s) : buttonOrderPriority.size()));
    for (Symbol sym: sortedAllowedSymbols)
    {
      if (sym == Symbol.EndStatement) continue;
      boolean isValidSymbol = stmtParser.peekParseSymbol(sym);
      String text = "Unknown";
      switch(sym)
      {
      case Assignment: text = ":="; break;
      case Gt: text = ">"; break;
      case Ge: text = ">="; break;
      case Lt: text = "<"; break;
      case Le: text = "<="; break;
      case Eq: text = "="; break;
      case Ne: text = "!="; break;
      case Plus: text = "+"; break;
      case Minus: text = "-"; break;
      case Multiply: text = "*"; break;
      case Divide: text = "/"; break;
      case OpenParenthesis: text = "("; break;
      case ClosedParenthesis: text = ")"; break;
      case DUMMY_COMMENT: text = "//"; break;
      case DotVariable: text = "."; break;
      case TrueLiteral: text = "true"; break;
      case FalseLiteral: text = "false"; break;
      case Number: text = "123..."; break;
      case String: text = "\"...\""; break;
      case Var: text = "var"; break;
      case Colon: text = ":"; break;
      case COMPOUND_IF: text = "if"; break;
      case COMPOUND_ELSE: text = "else"; break;
      case COMPOUND_ELSEIF: text = "elseif"; break;
      case COMPOUND_WHILE: text = "while"; break;
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
  }

  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.CLICK, (evt)-> {
      MouseEvent mevt = (MouseEvent)evt;
      ClientRect rect = div.getBoundingClientRect();
      int x = (int)(mevt.getClientX() - rect.getLeft()) + div.getScrollLeft();
      int y = (int)(mevt.getClientY() - rect.getTop()) + div.getScrollTop();

      CodePosition newPos = HitDetect.renderAndHitDetect(x, y, codeDiv, codeList, cursorPos, codeErrors);

      if (cursorPos != null)
      {
        cursorPos = newPos;
        codeDiv.setInnerHTML("");
        renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
        showPredictedTokenInput(choicesDiv);
      }
    }, false);
  }

  <U extends Token> void simpleEntryInput(String val, boolean isFinal, U token, boolean isEdit)
  {
    boolean advanceToNext = !isEdit;
    if (token == null) return;
    if (token instanceof Token.SimpleToken)
    {
      if (((Token.SimpleToken)token).type == Symbol.Number && val.isEmpty())
      {
        val = "0";
      }
      ((Token.SimpleToken)token).contents = val;
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateErrorList();
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
    }
    else if (token instanceof Token.WideToken && ((Token.WideToken)token).type == Symbol.DUMMY_COMMENT)
    {
      ((Token.WideToken)token).contents = val;
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
    }
    else if (token instanceof Token.ParameterToken && ((Token.ParameterToken)token).type == Symbol.DotVariable)
    {
      ((Token.ParameterToken)token).setContents(
          Token.ParameterToken.splitVarAtColons(val),
          Token.ParameterToken.splitVarAtColonsForPostfix(val));
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateErrorList();
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
    }
    if (isFinal)
    {
      choicesDiv.getStyle().setDisplay(Display.BLOCK);
      simpleEntry.setVisible(false);
      showPredictedTokenInput(choicesDiv);
    }

  }
  
  void updateErrorList()
  {
    codeErrors.clear();
    try {
      ParseToAst.parseStatementContainer(codeList);
    } 
    catch (ParseToAst.ParseException e)
    {
      codeErrors.add(e);
    }

  }
 
  void hookRun()
  {
    Element runEl = Browser.getDocument().querySelector("a.runbutton");
    runEl.addEventListener(Event.CLICK, (evt) -> {
      SimpleInterpreter terp = new SimpleInterpreter(codeList);
      try {
        terp.runNoReturn();
      } 
      catch (Exception err)
      {
        Browser.getWindow().getConsole().log(err);
      }
      evt.preventDefault();
    }, false);
  }
}