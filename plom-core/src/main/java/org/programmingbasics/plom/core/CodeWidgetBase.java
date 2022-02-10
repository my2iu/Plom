package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.StaticMemberSuggester;
import org.programmingbasics.plom.core.suggestions.Suggester;
import org.programmingbasics.plom.core.suggestions.TypeSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;
import org.programmingbasics.plom.core.view.CodeFragmentExtractor;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.EraseLeft;
import org.programmingbasics.plom.core.view.EraseSelection;
import org.programmingbasics.plom.core.view.GatherCodeCompletionInfo;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.InsertNewLine;
import org.programmingbasics.plom.core.view.InsertToken;
import org.programmingbasics.plom.core.view.NextPosition;
import org.programmingbasics.plom.core.view.ParseContext;
import org.programmingbasics.plom.core.view.RenderedCursorPosition;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;
import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.RenderedTokenHitBox;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.html.ClientRect;
import elemental.html.DivElement;
import elemental.svg.SVGSVGElement;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

public abstract class CodeWidgetBase implements CodeWidgetCursorOverlay.CursorMovingCallback 
{
  // References to other parts of the UI (possibly outside the coding area)
  SimpleEntry simpleEntry;
  DivElement choicesDiv;
  CodeWidgetCursorOverlay cursorOverlay;
  
  StatementContainer codeList = new StatementContainer();
  CodePosition cursorPos = new CodePosition();
  CodePosition selectionCursorPos = null;

  Symbol defaultParseContext = Symbol.FullStatement;
  
  boolean hasFocus = true;
  
  /** Does the codeDiv area used for getting x,y positions*/
  boolean codeAreaScrolls = true;
  
  /** 
   * Allows for the configuration of what global variables/types there are
   * for type checking.
   * */
  ConfigureGlobalScope globalConfigurator; 

  /** To configure object variables and function arguments that are accessible for code completion */
  VariableContextConfigurator variableContextConfigurator; 
  
  /** Errors to show in the code listing (error tokens will be underlined) */
  ErrorList codeErrors = new ErrorList();

  /** In tutorials, cut&paste support may be hidden to reduce clutter */
  boolean hasCutAndPaste = true;
  public void setHasCutAndPaste(boolean cutAndPaste) { hasCutAndPaste = cutAndPaste; }

  /** Some tokens may be filtered out for tutorials or if they aren't relevant for a certain context */
  Set<Symbol> filterExcludeTokens = new HashSet<>(Arrays.asList(Symbol.PrimitivePassthrough));
  public void setExcludeTokens(Collection<Symbol> tokens)
  {
    filterExcludeTokens = new HashSet<>(tokens);
  }

  /** If we want to show suggestions for small code snippets as a button.
   * Currently, only simple parameter tokens like @object or .from:to: are
   * handled. */
  @JsType
  public static class QuickSuggestion {
    public QuickSuggestion(String display, String code) { this.display = display; this.code = code; }
    public String display;
    public String code;
  }
  List<QuickSuggestion> suggestions = new ArrayList<>();
  public void setQuickSuggestions(Collection<QuickSuggestion> newSuggestions) { suggestions = new ArrayList<>(newSuggestions); }

  
  // Callback from the cursor overlay when the user has stopped dragging a cursor
  @Override public void cursorHandleEndMove()
  {
    updateCodeView(false);
    showPredictedTokenInput();
  }

  // Callback from the cursor overlay when the user is dragging a cursor by its handle
  @Override public void cursorHandleMoving(double x, double y, int pointerDownHandle, double cursorHandleXOffset, double cursorHandleYOffset)
  {
    CodePosition newPos = hitDetectPointer(x, y, cursorHandleXOffset, cursorHandleYOffset);
    if (newPos == null)
      newPos = new CodePosition();
    if (pointerDownHandle == 1)
      cursorPos = newPos;
    else if (pointerDownHandle == 2)
      selectionCursorPos = newPos;
    if (Objects.equals(selectionCursorPos, cursorPos))
      selectionCursorPos = null;
    updateCodeView(false);
//        showPredictedTokenInput(choicesDiv);
  }

  
  public void setCode(StatementContainer code)
  {
    this.codeList = code;
    updateCodeView(true);
    if (hasFocus)
      showPredictedTokenInput();
//    hookCodeScroller(codeDiv);
//    hookCodeClick(codeDiv);
  }

  @JsFunction
  public static interface VariableContextConfigurator {
    public void accept(CodeCompletionContext context);
  }
  public void setVariableContextConfigurator(ConfigureGlobalScope globalConfigurator, VariableContextConfigurator configurator)
  {
    this.globalConfigurator = globalConfigurator;
    variableContextConfigurator = configurator;
  }
  
  public void updateAfterResize()
  {
    updateCodeView(false);
  }

  public void close()
  {
    
  }

  
  // To ensure that predicted buttons end up in a consistent order and
  // with the most important ones showing first, we have a map with priorities
  // for each button
  static Map<Symbol, Integer> buttonOrderPriority = new HashMap<>();
  static {
    Symbol[] symbolOrder = new Symbol[] {
        Symbol.DotVariable,
        Symbol.AtType,
        Symbol.Number,
        Symbol.String,
        Symbol.TrueLiteral,
        Symbol.FalseLiteral,
        Symbol.NullLiteral,
        Symbol.This,
        Symbol.Super,
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
        Symbol.And,
        Symbol.Or,
        Symbol.OpenParenthesis,
        Symbol.ClosedParenthesis,
        Symbol.DUMMY_COMMENT,
        Symbol.Var,
        Symbol.COMPOUND_WHILE,
        Symbol.COMPOUND_FOR,
        Symbol.COMPOUND_IF,
        Symbol.COMPOUND_ELSEIF,
        Symbol.COMPOUND_ELSE,
        Symbol.Return,
        Symbol.Retype,
        Symbol.Is,
        Symbol.As,
//        Symbol.Colon,
        Symbol.In,
    };
    for (int n = 0; n < symbolOrder.length; n++)
      buttonOrderPriority.put(symbolOrder[n], n);
  }

  void showChoicesDiv()
  {
    choicesDiv.getStyle().setDisplay(Display.BLOCK);
  }

  void hideChoicesDiv()
  {
    choicesDiv.getStyle().setDisplay(Display.NONE);
  }

  void showPredictedTokenInput()
  {
    hasFocus = true;
    updateCursorVisibilityIfFocused();
    choicesDiv.setInnerHTML("");
    showChoicesDiv();
    simpleEntry.setVisible(false);

    // We have some buttons that float to the right, but on wide displays, those
    // buttons are too far to the right and get lost, so we put everything into
    // a separate inline-block div whose width is just the width of the content,
    // so then a div that floats right inside of it will just appear to the right
    // of the buttons instead of the right of the screen.
    DivElement contentDiv = Browser.getDocument().createDivElement();
    contentDiv.getStyle().setDisplay(Display.INLINE_BLOCK);
    contentDiv.getStyle().setProperty("white-space", "normal");
    choicesDiv.appendChild(contentDiv);
    // Actually, I don't like how the enter and backspace buttons keep moving
    // around now. I'll just clamp the width of the keyboard area.
    contentDiv.getStyle().setDisplay(Display.BLOCK);
    contentDiv.getStyle().setProperty("max-width", "35em");
    contentDiv.getStyle().setMarginLeft("auto");
    contentDiv.getStyle().setMarginRight("auto");

    // If the user has selected some code, we'll show options
    // specific to working with selections 
    if (selectionCursorPos != null) 
    {
      contentDiv.appendChild(makeButton("Copy", true, choicesDiv, () -> {
        // There's some weird bug in ios14 that I can't figure out
        // where after the pointer events for dragging some handles to
        // make a selection, if I then create a new anchor element
        // (i.e. a Copy button), then the first touch afterwards will
        // not result in any click events. This can be confusing because
        // the "Copy" button will not do anything the first time you
        // press it (only the second time). With all other buttons, that's
        // obvious, since they give feedback, but the copy button doesn't
        // so I need to show a message as feedback that a copy did happen.
        showToast("Copied.");
        CodePosition start;
        CodePosition end;
        if (cursorPos.isBefore(selectionCursorPos))
        {
          start = cursorPos;
          end = selectionCursorPos;
        }
        else
        {
          start = selectionCursorPos;
          end = cursorPos;
        }
        String fragment = CodeFragmentExtractor.extractFromStatements(codeList, start, end);
        Clipboard.instance.putCodeFragment(fragment);
      }));
      contentDiv.appendChild(makeButton("Erase", true, choicesDiv, () -> {
        CodePosition start;
        CodePosition end;
        if (cursorPos.isBefore(selectionCursorPos))
        {
          start = cursorPos;
          end = selectionCursorPos;
        }
        else
        {
          start = selectionCursorPos;
          end = cursorPos;
        }
        EraseSelection.fromStatements(codeList, start, end);
        cursorPos = start;
        selectionCursorPos = null;
        showPredictedTokenInput();
        updateCodeView(true);
      }));
      return;
    }
    
    // Parse the current statement up to the cursor position
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(defaultParseContext, codeList, cursorPos, 0);
    LL1Parser stmtParser = new LL1Parser();
    stmtParser.addToParse(parseContext.baseContext);
    for (Token tok: parseContext.tokens)
    {
      tok.visit(stmtParser);
    }
    Set<Symbol> allowedSymbols = stmtParser.allowedNextSymbols();

    // Butons for backspace and enter should be on the right
    DivElement floatDiv = Browser.getDocument().createDivElement();
    floatDiv.getStyle().setProperty("float", "right");
    floatDiv.getStyle().setProperty("text-align", "right");
    contentDiv.appendChild(floatDiv);
    // Backspace button
    DivElement floatDivLine = Browser.getDocument().createDivElement();
    floatDiv.appendChild(floatDivLine);
    floatDivLine.appendChild(makeButton("\u232B", true, choicesDiv, () -> {
      EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
      updateCodeView(true);
      showPredictedTokenInput();
    })); 
    // newline button
    floatDivLine = Browser.getDocument().createDivElement();
    floatDiv.appendChild(floatDivLine);
    if (parseContext.baseContext != Symbol.ExpressionOnly && parseContext.baseContext != Symbol.ForExpressionOnly)
    {
      floatDivLine.appendChild(makeButton("\u21b5", true, choicesDiv, () -> {
        InsertNewLine.insertNewlineIntoStatementContainer(codeList, cursorPos, 0);

        updateCodeView(true);
        showPredictedTokenInput();
      }));
    }
    else
      floatDivLine.appendChild(makeButton("\u21b5", false, choicesDiv, () -> {  }));
    
    // Buttons for next and edit buttons
    // Next button
    contentDiv.appendChild(makeButton("\u27a0", true, choicesDiv, () -> {
      NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateCodeView(false);
      showPredictedTokenInput();
    }));
    // Edit button for certain tokens
    Token currentToken = GetToken.inStatements(codeList, cursorPos, 0);
    boolean showEditButton = isTokenEditable(currentToken);
    if (showEditButton)
    {
      contentDiv.appendChild(makeButton("\u270e", true, choicesDiv, () -> {
        showSimpleEntryForToken(currentToken, true, null, cursorPos);
      }));
    }
    else
      contentDiv.appendChild(makeButton("\u270e", false, choicesDiv, () -> {  }));

    
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
      case Retype: text = "retype"; break;
      case Is: text = "is"; break;
      case As: text = "as"; break;
      case Gt: text = ">"; break;
      case Ge: text = ">="; break;
      case Lt: text = "<"; break;
      case Le: text = "<="; break;
      case Eq: text = "="; break;
      case Ne: text = "!="; break;
      case Or: text = "or"; break;
      case And: text = "and"; break;
      case Plus: text = "+"; break;
      case Minus: text = "-"; break;
      case Multiply: text = "*"; break;
      case Divide: text = "/"; break;
      case OpenParenthesis: text = "("; break;
      case ClosedParenthesis: text = ")"; break;
      case DUMMY_COMMENT: text = "//"; break;
      case DotVariable: text = "."; break;
      case AtType: text = "@"; break;
      case This: text = "this"; break;
      case Super: text = "super"; break;
      case TrueLiteral: text = "true"; break;
      case FalseLiteral: text = "false"; break;
      case NullLiteral: text = "null"; break;
      case Number: text = "123..."; break;
      case String: text = "\"...\""; break;
      case Var: text = "var"; break;
//      case Colon: text = ":"; break;
      case In: text = "in"; break;
      case COMPOUND_IF: text = "if"; break;
      case COMPOUND_ELSE: text = "else"; break;
      case COMPOUND_ELSEIF: text = "elseif"; break;
      case COMPOUND_WHILE: text = "while"; break;
      case COMPOUND_FOR: text = "for"; break;
      case Return: text = "return"; break;
      case PrimitivePassthrough: text = "primitive"; break;
      default:
      }
      String tokenText = text;
      if (filterExcludeTokens.contains(sym)) continue;
      // Here we distinguish between allowed symbols and valid symbols because we
      // don't want the UI to keep changing all the time with the changes in context.
      // Instead we show all normally allowed symbols, but disable the ones that
      // aren't valid for this particular context
      if (isValidSymbol)
        contentDiv.appendChild(makeButton(tokenText, true, choicesDiv, () -> { insertToken(cursorPos, tokenText, sym); }));
      else
        contentDiv.appendChild(makeButton(tokenText, false, choicesDiv, () -> {  }));
    }
    
    // Show quick suggestions
    if (!suggestions.isEmpty())
    {
      CodeCompletionContext suggestionContext = calculateSuggestionContext(codeList, cursorPos, globalConfigurator, variableContextConfigurator);
      Suggester dotSuggester = null;
      if (allowedSymbols.contains(Symbol.DotVariable))
        dotSuggester = getDotSuggester(suggestionContext, stmtParser.peekExpandedSymbols(Symbol.DotVariable)); 

      for (QuickSuggestion suggestion: suggestions)
      {
        // If the suggestion is for a variable or member
        if (suggestion.code.startsWith(".") && allowedSymbols.contains(Symbol.DotVariable))
        {
          // Check if variable would have been suggested here 
          if (dotSuggester != null && dotSuggester.gatherSuggestions(suggestion.code.substring(1)).contains(suggestion.code.substring(1)))
            contentDiv.appendChild(makeButton(suggestion.display, true, choicesDiv, () -> {
              Token newToken = new Token.ParameterToken(
                  Token.ParameterToken.splitVarAtColons(suggestion.code), 
                  Token.ParameterToken.splitVarAtColonsForPostfix(suggestion.code), 
                  Symbol.DotVariable);
              InsertToken.insertTokenIntoStatementContainer(codeList, newToken, cursorPos, 0, false);
              NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
              showPredictedTokenInput();
              updateCodeView(true);
              }));
        }
        // If the suggestion is for a type
        else if (suggestion.code.startsWith("@") && allowedSymbols.contains(Symbol.AtType))
        {
          // Check if type would have been suggested here 
          if (new TypeSuggester(suggestionContext, false).gatherSuggestions(suggestion.code.substring(1)).contains(suggestion.code.substring(1)))
            contentDiv.appendChild(makeButton(suggestion.display, true, choicesDiv, () -> { 
              Token newToken = new Token.ParameterToken(
                  Token.ParameterToken.splitVarAtColons(suggestion.code), 
                  Token.ParameterToken.splitVarAtColonsForPostfix(suggestion.code), 
                  Symbol.AtType);
              InsertToken.insertTokenIntoStatementContainer(codeList, newToken, cursorPos, 0, false);
              NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
              showPredictedTokenInput();
              updateCodeView(true);
              }));
        }
      }
    }
    
    // Show a paste button
    if (hasCutAndPaste)
    {
      contentDiv.appendChild(makeButton("\uD83D\uDCCB Paste", true, choicesDiv, () -> {
        doPaste();
      }));
    }
  }

  void insertToken(CodePosition pos, String tokenText, Symbol tokenType)
  {
    // Figure out some context to see if it can be used to narrow possible
    // options to show some more
    
    // Parse the current statement up to the cursor position
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(defaultParseContext, codeList, cursorPos, 0);
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
    case COMPOUND_FOR:
      newToken = new Token.OneExpressionOneBlockToken(tokenText, tokenType);
      break;
    case COMPOUND_ELSE:
      newToken = new Token.OneBlockToken(tokenText, tokenType);
      break;
    case AtType:
      newToken = new Token.ParameterToken(
          Token.ParameterToken.splitVarAtColons(tokenText), 
          Token.ParameterToken.splitVarAtColonsForPostfix(tokenText), 
          tokenType);
      break;
    case DotVariable:
      newToken = new Token.ParameterToken(
          Token.ParameterToken.splitVarAtColons(tokenText), 
          Token.ParameterToken.splitVarAtColonsForPostfix(tokenText), 
          tokenType);
      break;
    default:
      if (tokenType.isWide())
        newToken = new Token.WideToken(tokenText, tokenType);
      else
        newToken = new Token.SimpleToken(tokenText, tokenType);
      break;
    }
    InsertToken.insertTokenIntoStatementContainer(codeList, newToken, pos, 0, false);
    switch (tokenType)
    {
    case AtType:
    {
      updateCodeView(true);
      CodeCompletionContext suggestionContext = calculateSuggestionContext(codeList, pos, globalConfigurator, variableContextConfigurator);
      showSimpleEntryForToken(newToken, false, new TypeSuggester(suggestionContext, false), pos);
      break;
    }

    case DotVariable:
    {
      updateCodeView(true);
      CodeCompletionContext suggestionContext = calculateSuggestionContext(codeList, pos, globalConfigurator, variableContextConfigurator);
      showSimpleEntryForToken(newToken, false, getDotSuggester(suggestionContext, parentSymbols), pos);
      break;
    }

    case Number:
    case String:
    case DUMMY_COMMENT:
      updateCodeView(true);
      showSimpleEntryForToken(newToken, false, null, pos);
      break;
    default:
      NextPosition.nextPositionOfStatements(codeList, pos, 0);
      showPredictedTokenInput();
      updateCodeView(true);
      break;
    }
  }

  protected static CodeCompletionContext calculateSuggestionContext(StatementContainer codeList, CodePosition pos,
      ConfigureGlobalScope globalConfigurator, VariableContextConfigurator variableContextConfigurator)
  {
    CodeCompletionContext suggestionContext = new CodeCompletionContext();
    if (globalConfigurator != null)
      globalConfigurator.configure(suggestionContext.currentScope(), suggestionContext.coreTypes());
    if (variableContextConfigurator != null)
      variableContextConfigurator.accept(suggestionContext);
    suggestionContext.pushNewScope();
    if (codeList != null && pos != null)
      GatherCodeCompletionInfo.fromStatements(codeList, suggestionContext, pos, 0);
    return suggestionContext;
  }

  protected static boolean isTokenEditable(Token currentToken)
  {
    if (currentToken != null)
    {
      Symbol tokenType = null;
      if (currentToken instanceof Token.TokenWithSymbol)
        tokenType = ((Token.TokenWithSymbol)currentToken).getType();
      
      if (tokenType == Symbol.String || tokenType == Symbol.DUMMY_COMMENT
          || tokenType == Symbol.AtType || tokenType == Symbol.DotVariable)
        return true;
    }
    return false;
  }

  private void doPaste()
  {
    String fragmentStr = Clipboard.instance.getCodeFragment();
    if (fragmentStr == null) return;
    
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(defaultParseContext, codeList, cursorPos, 0);
    boolean canAcceptNewlinesAndWideTokens = (parseContext.baseContext != Symbol.ExpressionOnly && parseContext.baseContext != Symbol.ForExpressionOnly); 
    try {
      PlomTextReader.StringTextReader strReader = new PlomTextReader.StringTextReader(fragmentStr);
      PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(strReader); 
      StatementContainer container = PlomTextReader.readStatementContainer(lexer);
      boolean isFirst = true;
      for (TokenContainer tokens: container.statements)
      {
        if (!isFirst)
        {
          if (!canAcceptNewlinesAndWideTokens)
            break;
          InsertNewLine.insertNewlineIntoStatementContainer(codeList, cursorPos, 0);
        }
        isFirst = false;
        for (Token tok: tokens.tokens)
        {
          if (tok.isWide() && !canAcceptNewlinesAndWideTokens)
            break;
          InsertToken.insertTokenIntoStatementContainer(codeList, tok, cursorPos, 0, true);
        }
      }
      showPredictedTokenInput();
      updateCodeView(true);
    }
    catch (PlomReadException e)
    {
      // Do nothing if parsing goes wrong
      Browser.getWindow().getConsole().log(e);
    }
  }
  
  void showSimpleEntryForToken(Token newToken, boolean isEdit, Suggester suggester, CodePosition pos)
  {
    if (newToken == null) return;
    Symbol tokenType = null;
    String initialValue = "";
    if (newToken instanceof Token.TokenWithSymbol)
      tokenType = ((Token.TokenWithSymbol)newToken).getType();
    if (newToken instanceof Token.TokenWithEditableTextContent)
      initialValue = ((Token.TokenWithEditableTextContent)newToken).getTextContent();
    
    AtomicInteger doNotCoverLeftRef = new AtomicInteger(0);
    AtomicInteger doNotCoverRightRef = new AtomicInteger(0);
    getExtentOfCurrentToken(pos, doNotCoverLeftRef, doNotCoverRightRef);
    int doNotCoverLeft = doNotCoverLeftRef.get(), doNotCoverRight = doNotCoverRightRef.get();
    
    switch (tokenType)
    {
    case DotVariable:
      hideChoicesDiv();
      initialValue = initialValue.substring(1);
      simpleEntry.showFor(".", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case AtType:
      hideChoicesDiv();
      initialValue = initialValue.substring(1);
      simpleEntry.showFor("@", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case Number:
      hideChoicesDiv();
      simpleEntry.showFor("", "", "number: ", "", newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case String:
      hideChoicesDiv();
      if (isEdit)
        initialValue = initialValue.substring(1, initialValue.length() - 1);
      else
        initialValue = "";
      simpleEntry.showFor("\"", "\"", "", initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case DUMMY_COMMENT:
      hideChoicesDiv();
      initialValue = initialValue.substring(3);
      simpleEntry.showMultilineFor("// ", "", "", initialValue, newToken, isEdit, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      break;
    default:
      return;
    }
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
      updateCodeView(true);
    }
    else if (token instanceof Token.WideToken && ((Token.WideToken)token).type == Symbol.DUMMY_COMMENT)
    {
      ((Token.WideToken)token).contents = val;
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateCodeView(true);
    }
    else if (token instanceof Token.ParameterToken && ((Token.ParameterToken)token).type == Symbol.DotVariable)
    {
      ((Token.ParameterToken)token).setContents(
          Token.ParameterToken.splitVarAtColons(val),
          Token.ParameterToken.splitVarAtColonsForPostfix(val));
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateCodeView(true);
    }
    else if (token instanceof Token.ParameterToken && ((Token.ParameterToken)token).type == Symbol.AtType)
    {
      ((Token.ParameterToken)token).setContents(
          Token.ParameterToken.splitVarAtColons(val),
          Token.ParameterToken.splitVarAtColonsForPostfix(val));
      if (advanceToNext && isFinal)
        NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateCodeView(true);
    }
    if (isFinal)
    {
      showChoicesDiv();
      simpleEntry.setVisible(false);
      showPredictedTokenInput();
      // Force safari to blur, but do it after we've given ourselves the chance to
      // move focus somewhere else first.
      simpleEntry.forceSafariBlur();
    }
  }

  boolean simpleEntryBackspaceAll(boolean isEdit)
  {
    if (isEdit) return true;

    // Erase the just created token (it should be to the right of the cursor position)
    NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
    EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
    updateCodeView(true);
    showChoicesDiv();
    simpleEntry.setVisible(false);
    showPredictedTokenInput();
    return false;
  }

  abstract void updateCodeView(boolean isCodeChanged);
  abstract void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight);
  abstract CodePosition hitDetectPointer(double x, double y, double cursorHandleXOffset, double cursorHandleYOffset);
  abstract void updateForScroll();
  abstract void getExtentOfCurrentToken(CodePosition pos, AtomicInteger doNotCoverLeftRef, AtomicInteger doNotCoverRightRef);
  
  @JsFunction
  public static interface CodeOrCursorListener
  {
    public void onUpdate(boolean isCodeChanged);
  }
  CodeOrCursorListener listener;
  public void setListener(CodeOrCursorListener listener)
  {
    this.listener = listener;
  }
  

  
  // Map single touch position to be relative to the code panel 
  static double pointerToRelativeX(MouseEvent evt, DivElement div)
  {
    ClientRect rect = div.getBoundingClientRect();
    return (evt.getClientX() - rect.getLeft()) + div.getScrollLeft();
  }

  static double pointerToRelativeY(MouseEvent evt, DivElement div)
  {
    ClientRect rect = div.getBoundingClientRect();   
    return (evt.getClientY() - rect.getTop()) + div.getScrollTop();
  }

  void hookScrollUpdateCursor(Element div)
  {
    div.addEventListener(Event.SCROLL, (evt) -> {
      updateForScroll();
    }, false);
  }
  
  void hookCodeClick(DivElement div)
  {
    cursorOverlay.hookCursorHandles(div);
    
    div.addEventListener(Event.CLICK, (evt) -> {
      MouseEvent pevt = (MouseEvent)evt;
      double x = pointerToRelativeX(pevt, div);
      double y = pointerToRelativeY(pevt, div);
      CodePosition newPos = hitDetectPointer(x, y, 0, 0);
      if (newPos == null)
        newPos = new CodePosition();
      cursorPos = newPos;
      selectionCursorPos = null;

      updateCodeView(false);
      showPredictedTokenInput();
    }, false);
  }

  void updateCursorVisibilityIfFocused()
  {
    cursorOverlay.updateCursorVisibilityIfFocused(hasFocus);
  }
  
  // I can't remember why I needed to provide my own implementation
  // of touch scrolling. It must be something to due with DOM rendering
  // or maybe ios or something
  protected static void provideCustomDivScrolling(DivElement div)
  {
      class PointerScrollInfo {
        boolean isPointerDown;
        double pointerStartId;
        double pointerStartX, pointerStartY;
        double lastPointerX, lastPointerY;
        boolean isScrolling = false;
        Element scrollingEl;
      }
      PointerScrollInfo pointer = new PointerScrollInfo();
      
      // Pointer events don't seem to let you use preventDefault()
      // to override clicks or to override scrolling (so once you
      // move the pointer more than a certain distance, it will
      // register as a scroll and you'll receive a pointercancel
      // and stop receiving events). And Safari doesn't handle
      // complicated usages of touch-action and pointer-events (where
      // the svg tag has pointer-events none, but its contents have
      // pointer-events auto), so I'm going to have to write a
      // custom implementation of drag scrolling (and I'll disallow
      // pinch-zoom entirely due to excessive complexity)
      div.addEventListener("pointerdown", (evt) -> {
        PointerEvent pevt = (PointerEvent)evt;
        
        if (pointer.isPointerDown)
        {
          // Cancel the previous touchstart if there is more
          // than one touch, or if it's just unexpected
          if (pevt.getPointerId() == pointer.pointerStartId)
            releasePointerCapture(div, pointer.pointerStartId);
  
          
        }
  
        double x = pevt.getClientX();
        double y = pevt.getClientY();
  
        // Start tracking the new pointer down
        pointer.isPointerDown = true;
        pointer.pointerStartX = x;
        pointer.pointerStartY = y;
        pointer.isScrolling = false;
        pointer.lastPointerX = x;
        pointer.lastPointerY = y;
        pointer.pointerStartId = pevt.getPointerId();
        setPointerCapture(div, pevt.getPointerId());
      }, false);
      final double POINTER_SCROLL_TRIGGER_DIST = 5;
      div.addEventListener("pointermove", (evt) -> {
        PointerEvent pevt = (PointerEvent)evt;
        double x = pevt.getClientX();
        double y = pevt.getClientY();
        if (!pointer.isPointerDown) return;
        if (pointer.pointerStartId != pevt.getPointerId()) return;
        if (pointer.isPointerDown && !pointer.isScrolling
            && (Math.abs(x - pointer.pointerStartX) > POINTER_SCROLL_TRIGGER_DIST
            || Math.abs(y - pointer.pointerStartY) > POINTER_SCROLL_TRIGGER_DIST))
        {
          pointer.isScrolling = true;
          pointer.scrollingEl = div;
          // TODO: Allow scrolling of parent elements if it has already scrolled 
          // to its full extent
          // TODO: inertial/momentum scrolling
        }
        if (pointer.isScrolling)
        {
          pointer.scrollingEl.setScrollTop(doubleAsInt(pointer.scrollingEl.getScrollTop() - (y - pointer.lastPointerY)));
          pointer.scrollingEl.setScrollLeft(doubleAsInt(pointer.scrollingEl.getScrollLeft() - (x - pointer.lastPointerX)));
        }
        pointer.lastPointerX = x;
        pointer.lastPointerY = y;
      }, false);
      div.addEventListener("pointerup", (evt) -> {
        PointerEvent pevt = (PointerEvent)evt;
        // Pointer events doesn't have a proper preventDefault() mechanism
        // for allowing us to selectively pass certain events down for
        // default processing, so we have to manually code up logic for
        // determining whether an event is a click or not
        if (!pointer.isPointerDown) return;
        if (pevt.getPointerId() != pointer.pointerStartId) return;
        pointer.isPointerDown = false;
        releasePointerCapture(div, pointer.pointerStartId);
      }, false);
      div.addEventListener("pointercancel", (evt) -> {
  //      PointerEvent pevt = (PointerEvent)evt;
        if (pointer.isPointerDown)
        {
          // Cancel the previous pointer down
          pointer.isPointerDown = false;
          releasePointerCapture(div, pointer.pointerStartId);
        }
      }, false);
  }

  Suggester getDotSuggester(CodeCompletionContext suggestionContext, List<Symbol> parentSymbols)
  {
    Suggester suggester;
    if (parentSymbols.contains(Symbol.DotDeclareIdentifier))
    {
      suggester = null;
    }
    else
    {
      if (parentSymbols.contains(Symbol.StaticMethodCallExpression))
      {
        suggester = new StaticMemberSuggester(suggestionContext, true, true);
      }
      else if (parentSymbols.contains(Symbol.SuperCallExpression))
      {
        if (suggestionContext.getIsConstructorMethod())
        {
          suggester = new StaticMemberSuggester(suggestionContext, false, true);
        }
        else
        {
          // We only support super being used for constructor chaining right now
          suggester = null;
        }
      }
      else if (parentSymbols.contains(Symbol.DotMember))
      {
        suggester = new MemberSuggester(suggestionContext);
      }
      else
      {
        suggester = new VariableSuggester(suggestionContext);
      }
    }
    return suggester;
  }

  
  // Some of the Elemental APIs use an int when they should use a
  // double, but we don't want to lose precision so we pass around
  // double as if they were ints sometimes
  static native int doubleAsInt(double i) /*-{
    return i;
  }-*/;

  
  static native boolean getDefaultPrevented(Event evt) /*-{
    return evt.defaultPrevented;
  }-*/;

  static native void addActiveEventListenerTo(EventTarget target, String type, EventListener listener, boolean useCapture) /*-{
    target.addEventListener(type, 
      function(evt) { listener.@elemental.events.EventListener::handleEvent(Lelemental/events/Event;)(evt); }, 
      {passive: false, capture: useCapture});
  }-*/;

  static native void setPointerCapture(EventTarget target, double pointerId) /*-{
    target.setPointerCapture(pointerId);
  }-*/;

  static native void releasePointerCapture(EventTarget target, double pointerId) /*-{
    target.releasePointerCapture(pointerId);
  }-*/;

  protected static void showToast(String message)
  {
    Document doc = Browser.getDocument();
    DivElement div = doc.createDivElement();
    div.setTextContent(message);
    div.setClassName("plomToast");
    doc.getBody().appendChild(div);
    Browser.getWindow().setTimeout(() -> {
      div.getParentElement().removeChild(div);
    }, 2000);
  }

  static Element makeButton(String text, boolean enabled, Element refocusElement, Runnable onclick)
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
        // After a click, we often remove all the buttons, which causes
        // focus to be lost, which we want to manage. Instead, we force 
        // focus onto a known element (usually the entire div that holds
        // all the choices). 
        refocusElement.focus();
        onclick.run();
      }, false);
      return a;
    }
    else
    {
      return div;
    }
  }

  // Version of the CodeWidgetBase that uses SVG rendering
  public abstract static class CodeWidgetBaseSvg extends CodeWidgetBase
  {
    SVGSVGElement codeSvg;
    SvgCodeRenderer.TextWidthCalculator widthCalculator;
    RenderedHitBox svgHitBoxes;

    DivElement codeDiv;
    DivElement divForDeterminingWindowWidth;
    Element codeDivInteriorForScrollPadding;
    Element scrollingDivForDoNotCover;

    static final double leftPadding = 10;
    static final double rightPadding = 10;
    static final double topPadding = 10;
    static final double bottomPadding = 10;
    
    @Override void getExtentOfCurrentToken(CodePosition pos, AtomicInteger doNotCoverLeftRef, AtomicInteger doNotCoverRightRef)
    {
      final int MIN_TOKEN_SIZE_FOR_DO_NOT_COVER = 50;
      if (svgHitBoxes != null)
      {
        RenderedHitBox hitBox = RenderedTokenHitBox.inStatements(codeList, pos, 0, svgHitBoxes);
        if (hitBox != null)
        {
          doNotCoverLeftRef.set((int)(hitBox.getOffsetLeft() + leftPadding));
          doNotCoverRightRef.set(doNotCoverLeftRef.get() + Math.max(hitBox.getOffsetWidth(), MIN_TOKEN_SIZE_FOR_DO_NOT_COVER));
        }
      }
    }
    
    /**
     * Returns a mapping of divs for each line and their line numbers
     */
    protected static RenderedHitBox renderTokensSvg(DivElement divForDeterminingCodeWidth, SVGSVGElement codeSvg, StatementContainer codeList,
        CodePosition pos, CodePosition selectionPos, ErrorList codeErrors, SvgCodeRenderer.TextWidthCalculator widthCalculator)
    {
      double clientWidth = divForDeterminingCodeWidth.getClientWidth();
      if (selectionPos != null)
        return SvgCodeRenderer.renderSvgWithHitBoxes(codeSvg, codeList, null, pos, selectionPos, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
      else
      {
        return SvgCodeRenderer.renderSvgWithHitBoxes(codeSvg, codeList, pos, null, null, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
      }
    }

    @Override CodePosition hitDetectPointer(double x, double y, double cursorHandleXOffset, double cursorHandleYOffset)
    {
      double xOffset = cursorHandleXOffset, yOffset = cursorHandleYOffset;
      return HitDetect.detectHitBoxes((int)(x + xOffset - leftPadding), (int)(y + yOffset - topPadding), codeList, svgHitBoxes);
    }

    @Override void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight)
    {
      simpleEntry.scrollForDoNotCover(scrollingDivForDoNotCover, codeDivInteriorForScrollPadding, doNotCoverLeft, doNotCoverRight);
    }

    @Override void updateCodeView(boolean isCodeChanged)
    {
      if (listener != null)
        listener.onUpdate(isCodeChanged);
      RenderedHitBox renderedHitBoxes = renderTokensSvg(divForDeterminingWindowWidth, codeSvg, codeList, cursorPos, selectionCursorPos, codeErrors, widthCalculator);
      svgHitBoxes = renderedHitBoxes;
      updateCursor(renderedHitBoxes);
    }

    @Override void updateForScroll()
    {
      if (codeAreaScrolls)
        cursorOverlay.adjustForCodeDivScrolling((- codeDiv.getScrollLeft()) + leftPadding, (- codeDiv.getScrollTop()) + topPadding);
      else
      {
        ClientRect codeAreaRect = codeDiv.getBoundingClientRect();
        ClientRect scrollingAreaRect = scrollingDivForDoNotCover.getBoundingClientRect();
//        return (evt.getClientY() - rect.getTop()) + div.getScrollTop();
        cursorOverlay.adjustForCodeDivScrolling(codeAreaRect.getLeft() - scrollingAreaRect.getLeft() + leftPadding, codeAreaRect.getTop() - scrollingAreaRect.getTop() + topPadding);
      }
    }
    
    // We need the renderedhitboxes of the code to figure out where
    // the cursor is
    void updateCursor(RenderedHitBox renderedHitBoxes)
    {
      CursorRect cursorRect = RenderedCursorPosition.inStatements(codeList, cursorPos, 0, renderedHitBoxes);
      cursorOverlay.updateSvgCaret(cursorRect);
      double x = cursorRect.left;
      double y = cursorRect.bottom;
      
      // Handle scrolling
      updateForScroll();
      
      // Draw cursors
      updateCursorVisibilityIfFocused();
      cursorOverlay.updatePrimaryCursor(x, y, 0, 0, 0);
      // Secondary cursor
      CursorRect selectionCursorRect = null;
      if (selectionCursorPos != null)
      {
        selectionCursorRect = RenderedCursorPosition.inStatements(codeList, selectionCursorPos, 0, renderedHitBoxes);
      }
      cursorOverlay.updateSecondaryCursor(selectionCursorRect, selectionCursorPos, x, y, 0);
    }

  }
}
