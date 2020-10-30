package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.StaticMemberSuggester;
import org.programmingbasics.plom.core.suggestions.Suggester;
import org.programmingbasics.plom.core.suggestions.TypeSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.EraseLeft;
import org.programmingbasics.plom.core.view.GatherCodeCompletionInfo;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.InsertNewLine;
import org.programmingbasics.plom.core.view.InsertToken;
import org.programmingbasics.plom.core.view.NextPosition;
import org.programmingbasics.plom.core.view.ParseContext;
import org.programmingbasics.plom.core.view.RenderedHitBox;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.html.ClientRect;
import elemental.html.DivElement;

public class CodePanel
{
  public CodePanel(DivElement mainDiv)
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());

    codeDiv = (DivElement)mainDiv.querySelector("div.code");
    choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));

    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    
    updateCodeView(true);
    showPredictedTokenInput(choicesDiv);
    hookCodeClick(codeDiv);
  }

  public void setCode(StatementContainer code)
  {
    this.codeList = code;
    updateCodeView(true);
    showPredictedTokenInput(choicesDiv);
    hookCodeClick(codeDiv);
  }

  public void setVariableContextConfigurator(ConfigureGlobalScope globalConfigurator, Consumer<CodeCompletionContext> configurator)
  {
    this.globalConfigurator = globalConfigurator;
    variableContextConfigurator = configurator;
  }
  
  public void close()
  {
    
  }
  
  StatementContainer codeList = new StatementContainer();
  DivElement codeDiv;
  DivElement choicesDiv;
  SimpleEntry simpleEntry;
  CodePosition cursorPos = new CodePosition();
  
  /** 
   * Allows for the configuration of what global variables/types there are
   * for type checking.
   * */
  ConfigureGlobalScope globalConfigurator; 

  /** To configure object variables and function arguments that are accessible for code completion */
  Consumer<CodeCompletionContext> variableContextConfigurator; 
  
  /** Errors to show in the code listing (error tokens will be underlined) */
  ErrorList codeErrors = new ErrorList();

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
        Symbol.This,
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
        Symbol.COMPOUND_IF,
        Symbol.COMPOUND_ELSEIF,
        Symbol.COMPOUND_ELSE,
        Symbol.Return,
        Symbol.Retype,
    };
    for (int n = 0; n < symbolOrder.length; n++)
      buttonOrderPriority.put(symbolOrder[n], n);
  }
  
  /**
   * Returns a mapping of divs for each line and their line numbers
   */
  static void renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes, ErrorList codeErrors)
  {
    CodeRenderer.render(codeDiv, codeList, pos, renderedHitBoxes, codeErrors);
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
    InsertToken.insertTokenIntoStatementContainer(codeList, newToken, pos, 0);
    switch (tokenType)
    {
    case AtType:
    {
      CodeCompletionContext suggestionContext = calculateSuggestionContext(codeList, pos, globalConfigurator, variableContextConfigurator);
      showSimpleEntryForToken(newToken, false, new TypeSuggester(suggestionContext, false));
      break;
    }

    case DotVariable:
      if (parentSymbols.contains(Symbol.DotDeclareIdentifier))
      {
        showSimpleEntryForToken(newToken, false, null);
      }
      else
      {
        CodeCompletionContext suggestionContext = calculateSuggestionContext(codeList, pos, globalConfigurator, variableContextConfigurator);
        Suggester suggester;
        if (parentSymbols.contains(Symbol.StaticMethodCallExpression))
        {
          suggester = new StaticMemberSuggester(suggestionContext);
        }
        else if (parentSymbols.contains(Symbol.DotMember))
        {
          suggester = new MemberSuggester(suggestionContext);
        }
        else
        {
          suggester = new VariableSuggester(suggestionContext);
        }
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
    updateCodeView(true);
  }

  static CodeCompletionContext calculateSuggestionContext(StatementContainer codeList, CodePosition pos, ConfigureGlobalScope globalConfigurator, Consumer<CodeCompletionContext> variableContextConfigurator)
  {
    CodeCompletionContext suggestionContext = new CodeCompletionContext();
    globalConfigurator.configure(suggestionContext.currentScope(), suggestionContext.coreTypes());
    variableContextConfigurator.accept(suggestionContext);
    suggestionContext.pushNewScope();
    if (codeList != null && pos != null)
      GatherCodeCompletionInfo.fromStatements(codeList, suggestionContext, pos, 0);
    return suggestionContext;
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
    case AtType:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(1);
      simpleEntry.showFor("@", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput);
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
    
    // Parse the current statement up to the cursor position
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(codeList, cursorPos, 0);
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
    floatDivLine.appendChild(makeButton("\u232B", true, () -> {
      EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
      updateCodeView(true);
      showPredictedTokenInput(choicesDiv);
    })); 
    // newline button
    floatDivLine = Browser.getDocument().createDivElement();
    floatDiv.appendChild(floatDivLine);
    if (parseContext.baseContext != Symbol.ExpressionOnly)
    {
      floatDivLine.appendChild(makeButton("\u21b5", true, () -> {
        InsertNewLine.insertNewlineIntoStatementContainer(codeList, cursorPos, 0);

        updateCodeView(true);
        showPredictedTokenInput(choicesDiv);
      }));
    }
    else
      floatDivLine.appendChild(makeButton("\u21b5", false, () -> {  }));
    
    // Buttons for next and edit buttons
    // Next button
    contentDiv.appendChild(makeButton("\u27a0", true, () -> {
      NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
      updateCodeView(false);
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
      contentDiv.appendChild(makeButton("\u270e", true, () -> {
        showSimpleEntryForToken(currentToken, true, null);
      }));
    }
    else
      contentDiv.appendChild(makeButton("\u270e", false, () -> {  }));

    
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
      case Return: text = "return"; break;
      case PrimitivePassthrough: text = "primitive"; break;
      default:
      }
      String tokenText = text;
      if (sym == Symbol.PrimitivePassthrough) continue;
      // Here we distinguish between allowed symbols and valid symbols because we
      // don't want the UI to keep changing all the time with the changes in context.
      // Instead we show all normally allowed symbols, but disable the ones that
      // aren't valid for this particular context
      if (isValidSymbol)
        contentDiv.appendChild(makeButton(tokenText, true, () -> { insertToken(cursorPos, tokenText, sym); }));
      else
        contentDiv.appendChild(makeButton(tokenText, false, () -> {  }));
    }
  }

  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      MouseEvent mevt = (MouseEvent)evt;
      ClientRect rect = div.getBoundingClientRect();
      int x = (int)(mevt.getClientX() - rect.getLeft()) + div.getScrollLeft();
      int y = (int)(mevt.getClientY() - rect.getTop()) + div.getScrollTop();

      CodePosition newPos = HitDetect.renderAndHitDetect(x, y, codeDiv, codeList, cursorPos, codeErrors);
      if (newPos == null)
        newPos = new CodePosition();
      cursorPos = newPos;

      updateCodeView(false);
      showPredictedTokenInput(choicesDiv);
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
      choicesDiv.getStyle().setDisplay(Display.BLOCK);
      simpleEntry.setVisible(false);
      showPredictedTokenInput(choicesDiv);
    }
  }
  
  public static interface CodeOrCursorListener
  {
    public void onUpdate(boolean isCodeChanged);
  }
  
  CodeOrCursorListener listener;
  public void setListener(CodeOrCursorListener listener)
  {
    this.listener = listener;
  }
  
  void updateCodeView(boolean isCodeChanged)
  {
    if (listener != null)
      listener.onUpdate(isCodeChanged);
    codeDiv.setInnerHTML("");
    renderTokens(codeDiv, codeList, cursorPos, null, codeErrors);
  }
}
