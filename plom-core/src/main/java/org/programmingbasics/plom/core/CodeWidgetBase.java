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

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.view.CodeFragmentExtractor;
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
import elemental.html.SpanElement;
import elemental.svg.SVGSVGElement;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public abstract class CodeWidgetBase implements CodeWidgetCursorOverlay.CursorMovingCallback 
{
  // References to other parts of the UI (possibly outside the coding area)
  CodeWidgetInputPanels focus;
  
  StatementContainer codeList = new StatementContainer();
  CodePosition cursorPos = new CodePosition();
  CodePosition selectionCursorPos = null;

  Symbol defaultParseContext = Symbol.FullStatement;
  
  boolean hasFocus() { return focus.getCurrent() == this; };
  
  /** Does the codeDiv area used for getting x,y positions*/
  boolean codeAreaScrolls = true;

  /**
   * Special mode for entering small code fields (e.g. expressions etc). 
   */
  boolean isSingleLineMode = false;
  
  /** External interface to a component that can handle code completion suggestions */
  CodeCompletionSuggester codeCompletionSuggester;
  
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
    if (hasFocus())
      showPredictedTokenInput();
//    hookCodeScroller(codeDiv);
//    hookCodeClick(codeDiv);
  }
  
  public void setSingleLineCode(TokenContainer code)
  {
    if (code == null) code = new TokenContainer();
    setCode(new StatementContainer(code));
  }

  public TokenContainer getSingleLineCode()
  {
    if (codeList == null) 
      return new TokenContainer();
    if (codeList.statements.isEmpty())
      return new TokenContainer();
    return codeList.statements.get(0);
  }
  
  public void setCursorPosition(CodePosition pos)
  {
    cursorPos = pos;
    selectionCursorPos = null;

    showPredictedTokenInput();
    updateCodeView(false);
  }
  
  private CodePosition getSelectionTopMostPos()
  {
    if (cursorPos.isBefore(selectionCursorPos))
      return cursorPos;
    else
      return selectionCursorPos;
  }

  private CodePosition getSelectionBottomMostPos()
  {
    if (cursorPos.isBefore(selectionCursorPos))
      return selectionCursorPos;
    else
      return cursorPos;
  }

  /**
   * Interface for passing in code completion handlers into the code editor
   */
  public static interface CodeCompletionSuggester 
  {
    public Promise<Void> setCodeCompletionContextFor(StatementContainer code, CodePosition pos);
    public Promise<Void> setCodeCompletionContextForTypes();
    public SuggesterClient makeTypeSuggester(boolean allowVoid);
    public SuggesterClient makeVariableSuggester();
    public SuggesterClient makeMemberSuggester();
    public SuggesterClient makeStaticMemberSuggester(boolean includeNonConstructors, boolean includeConstructors);
    
    /** Whether the current method being edited is a constructor, static, or normal
     * method. Used when gather suggestions for a super call e.g. super.?
     */
    public boolean isCurrentMethodConstructor();
    public boolean isCurrentMethodStatic();
    
    /** When making a function lambda, it's hard to remember all the
     * names and parameters, so this will do a code suggestions of what
     * tokens are needed to complete a function lambda
     */
    public Promise<List<Token>> gatherExpectedTypeTokens();
  }

  public static interface VariableContextConfigurator {
    public void accept(CodeCompletionContext.Builder contextBuilder);
  }
//  @JsMethod
  public void setVariableContextConfigurator(CodeCompletionSuggester codeCompletionSuggester)
  {
    this.codeCompletionSuggester = codeCompletionSuggester;
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
        Symbol.FunctionTypeName,
        Symbol.Returns,
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
        Symbol.COMPOUND_COMMENT,
        Symbol.Var,
        Symbol.COMPOUND_WHILE,
        Symbol.COMPOUND_FOR,
        Symbol.COMPOUND_IF,
        Symbol.COMPOUND_ELSEIF,
        Symbol.COMPOUND_ELSE,
        Symbol.Return,
        Symbol.Break,
        Symbol.Continue,
        Symbol.Retype,
        Symbol.Is,
        Symbol.As,
//        Symbol.Colon,
        Symbol.In,
        Symbol.FunctionLiteral,
    };
    for (int n = 0; n < symbolOrder.length; n++)
      buttonOrderPriority.put(symbolOrder[n], n);
  }
  
  /** When we show predicted tokens, some of the predictions may be
   * done in a worker thread, so the results/predictions may come
   * back so late that the user has already typed some other tokens
   * and the predictions are no longer relevant. The context is 
   * different, and requests for predictions for this other context 
   * are likely already queued up for processing in the worker thread.
   * So this object is used to keep track of what the current
   * context is. Every time the user types something, a new prediction
   * context is generated, and we can check incoming predictions against
   * this context to see if they came from the same context
   */
  Object currentPredictedTokenUiContext = new Object();

  void showPredictedTokenInput()
  {
    focus.showChoicesDivAndTakeFocus(this);
    DivElement choicesDiv = focus.choicesDiv;
    choicesDiv.setInnerHTML("");

    // Start a new context, invalidating all old, pending predictions
    // that are being calculated in the worker thread
    currentPredictedTokenUiContext = new Object();
    
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
      contentDiv.appendChild(makeButton("Cut", true, choicesDiv, () -> {
        CodePosition start = getSelectionTopMostPos();
        CodePosition end = getSelectionBottomMostPos();
        String fragment = CodeFragmentExtractor.extractFromStatements(codeList, start, end);
        Clipboard.instance.putCodeFragment(fragment);
        EraseSelection.fromStatements(codeList, start, end);
        cursorPos = start;
        selectionCursorPos = null;
        showPredictedTokenInput();
        updateCodeView(true);
      }));
      contentDiv.appendChild(makeButton("Copy", true, choicesDiv, () -> {
        // We need to show some feedback that a "Copy" has occurred because
        // this is the only button that doesn't change the code when it is
        // pressed.
        showToast("Copied.");
        CodePosition start = getSelectionTopMostPos();
        CodePosition end = getSelectionBottomMostPos();
        String fragment = CodeFragmentExtractor.extractFromStatements(codeList, start, end);
        Clipboard.instance.putCodeFragment(fragment);
      }));
      contentDiv.appendChild(makeButton("Erase", true, choicesDiv, () -> {
        CodePosition start = getSelectionTopMostPos();
        CodePosition end = getSelectionBottomMostPos();
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
    if (!parseContext.baseContext.isRejectNewlines())
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
        SuggesterClient suggester = null;
        if (currentToken instanceof Token.TokenWithSymbol)
        {
          switch (((Token.TokenWithSymbol)currentToken).getType())
          {
            case AtType:
            {
              if (codeCompletionSuggester != null)
                codeCompletionSuggester.setCodeCompletionContextFor(codeList, cursorPos);
              List<Symbol> parentSymbols = stmtParser.peekExpandedSymbols(Symbol.AtType);
              suggester = codeCompletionSuggester.makeTypeSuggester(parentSymbols.contains(Symbol.ReturnTypeField));
              break;
            }

            case DotVariable:
            {
              if (codeCompletionSuggester != null)
                codeCompletionSuggester.setCodeCompletionContextFor(codeList, cursorPos);
              List<Symbol> parentSymbols = stmtParser.peekExpandedSymbols(Symbol.DotVariable);
              suggester = getDotSuggester(parentSymbols);
              break;
            }
          }
        }
        showSimpleEntryForToken(currentToken, true, suggester, cursorPos);
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
      String buttonText = null;
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
      case COMPOUND_COMMENT: text = "/*"; break;
      case DotVariable: text = "."; break;
      case AtType: text = "@"; break;
      case FunctionTypeName: text = "f@"; buttonText = "\u0192@"; break;
      case FunctionLiteral: text = "lambda"; buttonText = "\u03bb"; break;
      case Returns: text = "returns"; buttonText = "\u2192"; break;
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
      case Break: text = "break"; break;
      case Continue: text = "continue"; break;
      case PrimitivePassthrough: text = "primitive"; break;
      default:
      }
      if (buttonText == null) 
        buttonText = text;
      String tokenText = text;
      if (filterExcludeTokens.contains(sym)) continue;
      // Here we distinguish between allowed symbols and valid symbols because we
      // don't want the UI to keep changing all the time with the changes in context.
      // Instead we show all normally allowed symbols, but disable the ones that
      // aren't valid for this particular context
      if (isValidSymbol)
        contentDiv.appendChild(makeButton(buttonText, true, choicesDiv, () -> { insertToken(cursorPos, tokenText, sym); }));
      else
        contentDiv.appendChild(makeButton(buttonText, false, choicesDiv, () -> {  }));
    }

    // Calculation of suggestions is done asynchronously, so we save an insertion point
    // in the document for where we would want suggestions to go, then after we calculate
    // the suggestions, we know where to put them.
    SpanElement suggestionInsertionPoint = Browser.getDocument().createSpanElement();
    contentDiv.appendChild(suggestionInsertionPoint);
    
    // We might need to grab additional context for suggestions, but we don't want to
    // calculate these suggestions more than once, so just do it lazily as needed
//    CodeCompletionContext suggestionContext = null;
    boolean suggestionContextSent = false;
    
    // Show a suggestion for filling in function literals
    if (parseContext.baseContext == Symbol.FunctionLiteralExpressionOnly
        && parseContext.tokens.isEmpty())
    {
      if (!suggestionContextSent)
      {
        suggestionContextSent = true;
        if (codeCompletionSuggester != null)
          codeCompletionSuggester.setCodeCompletionContextFor(codeList, cursorPos);
      }
      codeCompletionSuggester.gatherExpectedTypeTokens().thenNow((tokens) -> {
//        Type expectedType = suggestionContext.getExpectedExpressionType();
//        if (expectedType != null && expectedType instanceof Type.LambdaFunctionType)
//        {
//          Type.LambdaFunctionType lambdaType = (Type.LambdaFunctionType)expectedType;
        if (tokens == null) return null;
        if (tokens.size() < 1) return null;
        if (!(tokens.get(0) instanceof Token.ParameterToken)) return null;
//          contentDiv.insertBefore(makeButton("\u0192@" + lambdaType.name, true, choicesDiv, () -> {
            contentDiv.insertBefore(makeButton("\u0192@" + ((Token.ParameterToken)tokens.get(0)).getLookupName(), true, choicesDiv, () -> {
            for (Token newToken: tokens)
            {
              InsertToken.insertTokenIntoStatementContainer(codeList, newToken, cursorPos, 0, false);
              // Manually advance to end of inserted token
              for (int n = 0;; n++)
              {
                if (!cursorPos.hasOffset(n + 1))
                {
                  cursorPos.setOffset(n, cursorPos.getOffset(n) + 1);
                  break;
                }
              }
            }
            showPredictedTokenInput();
            updateCodeView(true);
          }),
          suggestionInsertionPoint);
//        }
        return null;
      });
    }

    // Show quick suggestions
    if (!suggestions.isEmpty())
    {
      if (!suggestionContextSent)
      {
        suggestionContextSent = true;
        if (codeCompletionSuggester != null)
          codeCompletionSuggester.setCodeCompletionContextFor(codeList, cursorPos);
      }
      SuggesterClient dotSuggester = null;
      if (allowedSymbols.contains(Symbol.DotVariable))
        dotSuggester = getDotSuggester(stmtParser.peekExpandedSymbols(Symbol.DotVariable));
      Object requestPredictedTokenUiContext = currentPredictedTokenUiContext;

      for (QuickSuggestion suggestion: suggestions)
      {
        // If the suggestion is for a variable or member
        if (suggestion.code.startsWith(".") && allowedSymbols.contains(Symbol.DotVariable))
        {
          // Check if variable would have been suggested here 
          if (dotSuggester != null)
          {
            dotSuggester.gatherSuggestions(suggestion.code.substring(1), (suggestions) -> {
              if (requestPredictedTokenUiContext != currentPredictedTokenUiContext)
                return;
              if (!Browser.getDocument().contains(contentDiv))
                return;
              if (!suggestions.contains(suggestion.code.substring(1)))
                return;
              contentDiv.insertBefore(makeButton(suggestion.display, true, choicesDiv, () -> {
                Token newToken = new Token.ParameterToken(
                    Token.ParameterToken.splitVarAtColons(suggestion.code), 
                    Token.ParameterToken.splitVarAtColonsForPostfix(suggestion.code), 
                    Symbol.DotVariable);
                InsertToken.insertTokenIntoStatementContainer(codeList, newToken, cursorPos, 0, false);
                NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
                showPredictedTokenInput();
                updateCodeView(true);
                }),
                suggestionInsertionPoint);
            });
          }
        }
        // If the suggestion is for a type
        else if (suggestion.code.startsWith("@") && allowedSymbols.contains(Symbol.AtType))
        {
          // Check if type would have been suggested here 
          codeCompletionSuggester.makeTypeSuggester(false).gatherSuggestions(suggestion.code.substring(1), (suggestions) -> {
            if (requestPredictedTokenUiContext != currentPredictedTokenUiContext)
              return;
            if (!Browser.getDocument().contains(contentDiv))
              return;
            if (!suggestions.contains(suggestion.code.substring(1)))
              return;
            contentDiv.insertBefore(makeButton(suggestion.display, true, choicesDiv, () -> { 
              Token newToken = new Token.ParameterToken(
                  Token.ParameterToken.splitVarAtColons(suggestion.code), 
                  Token.ParameterToken.splitVarAtColonsForPostfix(suggestion.code), 
                  Symbol.AtType);
              InsertToken.insertTokenIntoStatementContainer(codeList, newToken, cursorPos, 0, false);
              NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
              showPredictedTokenInput();
              updateCodeView(true);
              }),
              suggestionInsertionPoint);
          });
        }
        // If the suggestion is for a function type
        else if (suggestion.code.startsWith("f@") && allowedSymbols.contains(Symbol.FunctionTypeName))
        {
          // TODO: quick suggestions for function types
          
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

  /** Given a type, returns a list of tokens that can be used to create that type in some code */
  private static List<Token> makeTokensForType(Type type)
  {
    List<Token> toReturn = new ArrayList<>();
    if (type instanceof Type.LambdaFunctionType)
    {
      Type.LambdaFunctionType lambdaType = (Type.LambdaFunctionType)type;
      Token.ParameterToken newToken = new Token.ParameterToken(
          Token.ParameterToken.splitVarAtColons("f@" + lambdaType.name), 
          Token.ParameterToken.splitVarAtColonsForPostfix("f@" + lambdaType.name), 
          Symbol.FunctionTypeName);
      for (int n = 0; n < lambdaType.args.size(); n++)
      {
        if (lambdaType.optionalArgNames.get(n) != null && !lambdaType.optionalArgNames.get(n).isEmpty())
        {
          newToken.parameters.get(n).tokens.add(Token.ParameterToken.fromContents("." + lambdaType.optionalArgNames.get(n), Symbol.DotVariable));
        }
        newToken.parameters.get(n).tokens.addAll(makeTokensForType(lambdaType.args.get(n)));
      }
      toReturn.add(newToken);
      
      if (lambdaType.returnType != null)
      {
        toReturn.add(new Token.SimpleToken("returns", Symbol.Returns));
        toReturn.addAll(makeTokensForType(lambdaType.returnType));
      }
    }
    else if (type instanceof Type)
    {
      toReturn.add(Token.ParameterToken.fromContents("@" + type.name, Symbol.AtType));
    }
    return toReturn;
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
    case FunctionLiteral:
      newToken = new Token.OneExpressionOneBlockToken(tokenText, tokenType);
      break;
    case COMPOUND_ELSE:
    case COMPOUND_COMMENT:
      newToken = new Token.OneBlockToken(tokenText, tokenType);
      break;
    case AtType:
    case FunctionTypeName:
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
      codeCompletionSuggester.setCodeCompletionContextForTypes();
      showSimpleEntryForToken(newToken, false, 
          codeCompletionSuggester.makeTypeSuggester(parentSymbols.contains(Symbol.ReturnTypeField)), 
          pos);
      break;
    }

    case FunctionTypeName:
    {
      updateCodeView(true);
      codeCompletionSuggester.setCodeCompletionContextForTypes();
      showSimpleEntryForToken(newToken, false, 
          codeCompletionSuggester.makeTypeSuggester(parentSymbols.contains(Symbol.ReturnTypeField)), 
          pos);
      break;
    }

    case DotVariable:
    {
      updateCodeView(true);
      if (codeCompletionSuggester != null)
        codeCompletionSuggester.setCodeCompletionContextFor(codeList, pos);
      showSimpleEntryForToken(newToken, false, getDotSuggester(parentSymbols), pos);
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

  // TODO: Delete this and related fields
  @Deprecated private static CodeCompletionContext calculateSuggestionContext(StatementContainer codeList, CodePosition pos,
      CodeCompletionSuggester codeCompletionSuggester, ConfigureGlobalScope globalConfigurator, VariableContextConfigurator variableContextConfigurator)
  {
    if (codeCompletionSuggester != null)
      codeCompletionSuggester.setCodeCompletionContextFor(codeList, pos);
    CodeCompletionContext.Builder suggestionContextBuilder = CodeCompletionContext.builder();
    if (globalConfigurator != null)
      globalConfigurator.configure(suggestionContextBuilder.currentScope(), suggestionContextBuilder.coreTypes());
    if (variableContextConfigurator != null)
      variableContextConfigurator.accept(suggestionContextBuilder);
    suggestionContextBuilder.pushNewScope();
    CodeCompletionContext suggestionContext = suggestionContextBuilder.build();
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
          || tokenType == Symbol.AtType || tokenType == Symbol.FunctionTypeName
          || tokenType == Symbol.DotVariable)
        return true;
    }
    return false;
  }

  private void doPaste()
  {
    String fragmentStr = Clipboard.instance.getCodeFragment();
    if (fragmentStr == null) return;
    
    ParseContext.ParseContextForCursor parseContext = ParseContext.findPredictiveParseContextForStatements(defaultParseContext, codeList, cursorPos, 0);
    boolean canAcceptNewlinesAndWideTokens = !parseContext.baseContext.isRejectNewlines(); 
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
          if (!tok.isInline() && !canAcceptNewlinesAndWideTokens)
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
  
  void showSimpleEntryForToken(Token newToken, boolean isEdit, SuggesterClient suggester, CodePosition pos)
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
      initialValue = initialValue.substring(1);
      focus.showSimpleEntryFor(".", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case AtType:
      initialValue = initialValue.substring(1);
      focus.showSimpleEntryFor("@", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case FunctionTypeName:
      initialValue = initialValue.substring(2);
      focus.showSimpleEntryFor("\u0192@", "f@", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case Number:
      focus.showNumberEntryFor("", newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case String:
      if (isEdit)
        initialValue = initialValue.substring(1, initialValue.length() - 1);
      else
        initialValue = "";
      focus.showSimpleEntryFor("\"", "\"", "", initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      scrollSimpleEntryToNotCover(doNotCoverLeft, doNotCoverRight);
      break;
    case DUMMY_COMMENT:
      initialValue = initialValue.substring(3);
      focus.showSimpleEntryMultilineFor("// ", "", "", initialValue, newToken, isEdit, this::simpleEntryInput, this::simpleEntryBackspaceAll);
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
      if (((Token.SimpleToken)token).type == Symbol.Number)
      {
        val = PlomTextReader.sanitizeForNumberMatch(val);
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
      val = PlomTextReader.sanitizeDotVariable(val);
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
    else if (token instanceof Token.ParameterToken && ((Token.ParameterToken)token).type == Symbol.FunctionTypeName)
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
//      focus.showChoicesDiv();
//      focus.hideSimpleEntry();
      showPredictedTokenInput();
      // Force safari to blur, but do it after we've given ourselves the chance to
      // move focus somewhere else first.
      focus.simpleEntry.forceSafariBlur();
    }
  }

  boolean simpleEntryBackspaceAll(boolean isEdit)
  {
    if (isEdit) return true;

    // Erase the just created token (it should be to the right of the cursor position)
    NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
    EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
    updateCodeView(true);
//    focus.showChoicesDiv();
//    focus.hideSimpleEntry();
    showPredictedTokenInput();
    return false;
  }

  abstract void updateCodeView(boolean isCodeChanged);
  abstract void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight);
  abstract CodePosition hitDetectPointer(double x, double y, double cursorHandleXOffset, double cursorHandleYOffset);
  abstract void updateForScroll(CodeWidgetCursorOverlay cursorOverlay);
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
      focus.updateCursorForScroll();
    }, false);
  }
  
  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.CLICK, (evt) -> {
      MouseEvent pevt = (MouseEvent)evt;
      double x = pointerToRelativeX(pevt, div);
      double y = pointerToRelativeY(pevt, div);
      CodePosition newPos = hitDetectPointer(x, y, 0, 0);
      if (newPos == null)
        newPos = new CodePosition();
      cursorPos = newPos;
      selectionCursorPos = null;

      showPredictedTokenInput();
      updateCodeView(false);
    }, false);
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

  SuggesterClient getDotSuggester(List<Symbol> parentSymbols)
  {
    SuggesterClient suggester;
    if (parentSymbols.contains(Symbol.DotDeclareIdentifier))
    {
      suggester = null;
    }
    else
    {
      if (parentSymbols.contains(Symbol.StaticMethodCallExpression))
      {
        suggester = codeCompletionSuggester.makeStaticMemberSuggester(true, true);
      }
      else if (parentSymbols.contains(Symbol.SuperCallExpression))
      {
        if (codeCompletionSuggester.isCurrentMethodConstructor())
        {
          // Constructor chaining
          suggester = codeCompletionSuggester.makeStaticMemberSuggester(false, true);
        }
        else if (!codeCompletionSuggester.isCurrentMethodConstructor() && !codeCompletionSuggester.isCurrentMethodStatic())
        {
          // Instance method can call instance methods of the parent
          suggester = codeCompletionSuggester.makeMemberSuggester();
        }
        else
        {
          // Static methods aren't handled
          suggester = null;
        }
      }
      else if (parentSymbols.contains(Symbol.DotMember))
      {
        suggester = codeCompletionSuggester.makeMemberSuggester();
      }
      else
      {
        suggester = codeCompletionSuggester.makeVariableSuggester();
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

    double leftPadding = 10;
    double rightPadding = 10;
    double topPadding = 10;
    double bottomPadding = 10;
    
    @Override public DivElement getBaseCodeDiv()
    {
      return codeDiv;
    }

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
        CodePosition pos, CodePosition selectionPos, ErrorList codeErrors, SvgCodeRenderer.TextWidthCalculator widthCalculator,
        double leftPadding, double rightPadding, double topPadding, double bottomPadding,
        boolean isSingleLineMode)
    {
      double clientWidth = divForDeterminingCodeWidth.getClientWidth();
      if (!isSingleLineMode)
      {
        if (selectionPos != null)
          return SvgCodeRenderer.renderSvgWithHitBoxes(codeSvg, codeList, null, pos, selectionPos, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
        else
          return SvgCodeRenderer.renderSvgWithHitBoxes(codeSvg, codeList, pos, null, null, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
      }
      else
      {
        if (selectionPos != null)
          return SvgCodeRenderer.renderSvgSingleLineWithHitBoxes(codeSvg, codeList, null, pos, selectionPos, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
        else
          return SvgCodeRenderer.renderSvgSingleLineWithHitBoxes(codeSvg, codeList, pos, null, null, codeErrors, widthCalculator, clientWidth, leftPadding, topPadding, rightPadding, bottomPadding);
      }
    }

    @Override CodePosition hitDetectPointer(double x, double y, double cursorHandleXOffset, double cursorHandleYOffset)
    {
      double xOffset = cursorHandleXOffset, yOffset = cursorHandleYOffset;
      return HitDetect.detectHitBoxes((int)(x + xOffset - leftPadding), (int)(y + yOffset - topPadding), codeList, svgHitBoxes);
    }

    @Override void scrollSimpleEntryToNotCover(int doNotCoverLeft, int doNotCoverRight)
    {
      focus.simpleEntry.scrollForDoNotCover(scrollingDivForDoNotCover, codeDivInteriorForScrollPadding, doNotCoverLeft, doNotCoverRight);
    }

    @Override void updateCodeView(boolean isCodeChanged)
    {
      if (listener != null)
        listener.onUpdate(isCodeChanged);
      RenderedHitBox renderedHitBoxes = renderTokensSvg(divForDeterminingWindowWidth, codeSvg, codeList, cursorPos, selectionCursorPos, codeErrors, widthCalculator,
          leftPadding, rightPadding, topPadding, bottomPadding,
          isSingleLineMode);
      svgHitBoxes = renderedHitBoxes;
      updateCursor(renderedHitBoxes);
    }

    @Override void updateForScroll(CodeWidgetCursorOverlay cursorOverlay)
    {
      if (codeAreaScrolls)
        cursorOverlay.adjustForCodeDivScrolling((- codeDiv.getScrollLeft()) + leftPadding, (- codeDiv.getScrollTop()) + topPadding);
      else
      {
        if (focus.getCurrent() == this)
        {
          ClientRect codeAreaRect = codeDiv.getBoundingClientRect();
          ClientRect scrollingAreaRect = scrollingDivForDoNotCover.getBoundingClientRect();
//          return (evt.getClientY() - rect.getTop()) + div.getScrollTop();
          cursorOverlay.adjustForCodeDivScrolling(codeAreaRect.getLeft() - scrollingAreaRect.getLeft() + leftPadding, codeAreaRect.getTop() - scrollingAreaRect.getTop() + topPadding);
        }
      }
    }
    
    // We need the renderedhitboxes of the code to figure out where
    // the cursor is
    void updateCursor(RenderedHitBox renderedHitBoxes)
    {
      CursorRect cursorRect = RenderedCursorPosition.inStatements(codeList, cursorPos, 0, renderedHitBoxes);
      focus.cursorOverlay.updateSvgCaret(cursorRect);
      double x = cursorRect.left;
      double y = cursorRect.bottom;
      
      // Handle scrolling
      focus.updateCursorForScroll();
      
      // Draw cursors
      focus.updateCursorVisibilityIfFocused();
      focus.cursorOverlay.updatePrimaryCursor(x, y, 0, 0, 0);
      // Secondary cursor
      CursorRect selectionCursorRect = null;
      if (selectionCursorPos != null)
      {
        selectionCursorRect = RenderedCursorPosition.inStatements(codeList, selectionCursorPos, 0, renderedHitBoxes);
      }
      focus.cursorOverlay.updateSecondaryCursor(selectionCursorRect, selectionCursorPos, x, y, 0);
    }

  }
}
