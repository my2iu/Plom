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
import org.programmingbasics.plom.core.view.RenderedCursorPosition;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;
import org.programmingbasics.plom.core.view.RenderedHitBox;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.css.CSSStyleDeclaration.Position;
import elemental.css.CSSStyleDeclaration.Unit;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.html.AnchorElement;
import elemental.html.ClientRect;
import elemental.html.DivElement;
import elemental.svg.SVGCircleElement;
import elemental.svg.SVGElement;
import elemental.svg.SVGSVGElement;

public class CodePanel
{
  public CodePanel(DivElement mainDiv)
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());

    codeDiv = (DivElement)mainDiv.querySelector("div.code");
    choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
    cursorOverlayEl = (Element)mainDiv.querySelector("svg.cursoroverlay");
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));

    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    
    updateCodeView(true);
    showPredictedTokenInput(choicesDiv);
    hookCodeScroller(codeDiv);
    hookCodeClick((DivElement)mainDiv.querySelector("div.code"));
  }

  public void setCode(StatementContainer code)
  {
    this.codeList = code;
    updateCodeView(true);
    showPredictedTokenInput(choicesDiv);
//    hookCodeScroller(codeDiv);
//    hookCodeClick(codeDiv);
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
  Element cursorOverlayEl;
  SimpleEntry simpleEntry;
  CodePosition cursorPos = new CodePosition();
  CodePosition selectionCursorPos = null;

  /**
   * For on-screen cursor handle that user can drag to move the cursor
   */
  static class CursorHandle
  {
    double x, y;
    double xOffset, yOffset;
  }
  CursorHandle cursorHandle1 = new CursorHandle();
  CursorHandle cursorHandle2 = new CursorHandle();
  final static int HANDLE_SIZE = 15;
  
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
        Symbol.NullLiteral,
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
  static RenderedHitBox renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, CodePosition selectionPos, ErrorList codeErrors)
  {
    if (selectionPos != null)
      return CodeRenderer.renderWithHitBoxes(codeDiv, codeList, pos, pos, selectionPos, codeErrors);
    else
      return CodeRenderer.renderWithHitBoxes(codeDiv, codeList, pos, null, null, codeErrors);
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
      simpleEntry.showFor(".", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      break;
    case AtType:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(1);
      simpleEntry.showFor("@", "", null, initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      break;
    case Number:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      simpleEntry.showFor("", "", "number: ", "", newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      break;
    case String:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      if (isEdit)
        initialValue = initialValue.substring(1, initialValue.length() - 1);
      else
        initialValue = "";
      simpleEntry.showFor("\"", "\"", "", initialValue, newToken, isEdit, suggester, this::simpleEntryInput, this::simpleEntryBackspaceAll);
      break;
    case DUMMY_COMMENT:
      choicesDiv.getStyle().setDisplay(Display.NONE);
      initialValue = initialValue.substring(3);
      simpleEntry.showMultilineFor("// ", "", "", initialValue, newToken, isEdit, this::simpleEntryInput, this::simpleEntryBackspaceAll);
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

    if (selectionCursorPos != null) 
    {
      contentDiv.appendChild(makeButton("Copy", true, () -> {
        
      }));
      return;
    }
    
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
      case NullLiteral: text = "null"; break;
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

  int pointerDownHandle = 0;  // 0 - pointer was not pressed on a handle
  double pointerDownX = 0;
  double pointerDownY = 0;
  
  // Map single touch position to be relative to the code panel 
  private double pointerToRelativeX(PointerEvent evt, DivElement div)
  {
    ClientRect rect = div.getBoundingClientRect();
    return (evt.getClientX() - rect.getLeft()) + div.getScrollLeft();
  }

  private double pointerToRelativeY(PointerEvent evt, DivElement div)
  {
    ClientRect rect = div.getBoundingClientRect();   
    return (evt.getClientY() - rect.getTop()) + div.getScrollTop();
  }

  static void hookCodeScroller(DivElement div)
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
      PointerEvent pevt = (PointerEvent)evt;
      if (pointer.isPointerDown)
      {
        // Cancel the previous pointer down
        pointer.isPointerDown = false;
        releasePointerCapture(div, pointer.pointerStartId);
      }
    }, false);
    
  }
  
  void hookCursorHandle(DivElement div, Element cursorHandleEl, int cursorId)
  {
    addActiveEventListenerTo(cursorHandleEl, "pointerdown", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      setPointerCapture(cursorHandleEl, pevt.getPointerId());
//      setPointerCapture(Browser.getDocument().querySelector(".asd"),
//          pevt.getPointerId());
      evt.preventDefault();
      evt.stopPropagation();
      pointerDownHandle = cursorId;
      double x = pointerToRelativeX(pevt, div);
      double y = pointerToRelativeY(pevt, div);
      pointerDownX = x;
      pointerDownY = y;
    }, false);
    addActiveEventListenerTo(cursorHandleEl, "pointermove", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      if (pointerDownHandle != 0)
      {
        double x = pointerToRelativeX(pevt, div);
        double y = pointerToRelativeY(pevt, div);
        pointerDownX = x;
        pointerDownY = y;
        if (pointerDownHandle == 1)
        {
          CodePosition newPos = HitDetect.renderAndHitDetect((int)(x + cursorHandle1.xOffset), (int)(y + cursorHandle1.yOffset), codeDiv, codeList, cursorPos, codeErrors);
          if (newPos == null)
            newPos = new CodePosition();
          cursorPos = newPos;
          if (cursorPos.equals(selectionCursorPos))
            selectionCursorPos = null;
        }
        else if (pointerDownHandle == 2)
        {
          CodePosition newPos = HitDetect.renderAndHitDetect((int)(x + cursorHandle2.xOffset), (int)(y + cursorHandle2.yOffset), codeDiv, codeList, cursorPos, codeErrors);
          if (newPos == null)
            newPos = new CodePosition();
          if (newPos.equals(cursorPos))
            selectionCursorPos = null;
          else
            selectionCursorPos = newPos;
        }
        updateCodeView(false);
//        showPredictedTokenInput(choicesDiv);
      }
      evt.preventDefault();
      evt.stopPropagation();
    }, false);
    cursorHandleEl.addEventListener("pointerup", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      releasePointerCapture(cursorHandleEl, pevt.getPointerId());
      evt.preventDefault();
      evt.stopPropagation();
      pointerDownHandle = 0;
      updateCodeView(false);
      showPredictedTokenInput(choicesDiv);
    }, false);
    cursorHandleEl.addEventListener("pointercancel", (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      pointerDownHandle = 0;
      releasePointerCapture(cursorHandleEl, pevt.getPointerId());
      updateCodeView(false);
      showPredictedTokenInput(choicesDiv);
    }, false);
    
  }
  
  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.SCROLL, (evt) -> {
      cursorOverlayEl.querySelector("g.cursorscrolltransform").setAttribute("transform", "translate(" + (- codeDiv.getScrollLeft()) + " " + (- codeDiv.getScrollTop()) + ")");
    }, false);
    hookCursorHandle(div, (Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0), 1);
    hookCursorHandle(div, (Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1), 2);
    
    div.addEventListener(Event.CLICK, (evt) -> {
      PointerEvent pevt = (PointerEvent)evt;
      double x = pointerToRelativeX(pevt, div);
      double y = pointerToRelativeY(pevt, div);
      CodePosition newPos = HitDetect.renderAndHitDetect((int)x, (int)y, codeDiv, codeList, cursorPos, codeErrors);
      if (newPos == null)
        newPos = new CodePosition();
      cursorPos = newPos;
      selectionCursorPos = null;

      updateCodeView(false);
      showPredictedTokenInput(choicesDiv);
    }, false);
  }

  // Some of the Elemental APIs use an int when they should use a
  // double, but we don't want to lose precision so we pass around
  // double as if they were ints sometimes
  private static native int doubleAsInt(double i) /*-{
    return i;
  }-*/;

  
  private static native boolean getDefaultPrevented(Event evt) /*-{
    return evt.defaultPrevented;
  }-*/;

  private static native void addActiveEventListenerTo(EventTarget target, String type, EventListener listener, boolean useCapture) /*-{
    target.addEventListener(type, 
      function(evt) { listener.@elemental.events.EventListener::handleEvent(Lelemental/events/Event;)(evt); }, 
      {passive: false, capture: useCapture});
  }-*/;

  private static native void setPointerCapture(EventTarget target, double pointerId) /*-{
    target.setPointerCapture(pointerId);
  }-*/;

  private static native void releasePointerCapture(EventTarget target, double pointerId) /*-{
    target.releasePointerCapture(pointerId);
  }-*/;

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

  boolean simpleEntryBackspaceAll(boolean isEdit)
  {
    if (isEdit) return true;

    // Erase the just created token (it should be to the right of the cursor position)
    NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
    EraseLeft.eraseLeftFromStatementContainer(codeList, cursorPos, 0);
    updateCodeView(true);
    choicesDiv.getStyle().setDisplay(Display.BLOCK);
    simpleEntry.setVisible(false);
    showPredictedTokenInput(choicesDiv);
    return false;
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
    RenderedHitBox renderedHitBoxes = renderTokens(codeDiv, codeList, cursorPos, selectionCursorPos, codeErrors);
    updateCursor(renderedHitBoxes);
//    addCursorOverlay();
  }
  
//  void addCursorOverlay()
//  {
//    // Add an svg overlay
//    SVGSVGElement svg = Browser.getDocument().createSVGElement();
//    svg.getStyle().setLeft("0");
//    svg.getStyle().setTop("0");
//    svg.getStyle().setWidth(codeDiv.getScrollWidth(), Unit.PX);
////    svg.getStyle().setRight("0");
////    svg.getStyle().setBottom("0");
//    svg.getStyle().setHeight(codeDiv.getScrollHeight(), Unit.PX);
//    svg.setAttribute("width", "100%");
//    svg.setAttribute("height", "100%");
//    svg.getStyle().setPosition(Position.ABSOLUTE);
//    svg.getStyle().setProperty("pointer-events", "none");
//    codeDiv.appendChild(svg);
//    
//    // Find where the cursor should be placed
//    DivElement cursorDiv = (DivElement)codeDiv.querySelector(".codecursor");
//    if (cursorDiv == null) return;
//    double x = 0, y = 0;
//    for (Element el = cursorDiv; el != codeDiv; el = el.getOffsetParent())
//    {
//      x += el.getOffsetLeft();
//      y += el.getOffsetTop();
//    }
//    
//    // Draw a handle under the cursor
////    updateCursor();
//
////    createCursorHandleSvg(svg, cursorHandle1);
//  }

  
  // We need the renderedhitboxes of the code to figure out where
  // the cursor is
  void updateCursor(RenderedHitBox renderedHitBoxes)
  {
    DivElement cursorDiv = (DivElement)codeDiv.querySelector(".codecursor");
    if (cursorDiv == null) return;
    double x = 0, y = 0;
    for (Element el = cursorDiv; el != codeDiv; el = el.getOffsetParent())
    {
      x += el.getOffsetLeft();
      y += el.getOffsetTop();
    }
    // Handle scrolling
    cursorOverlayEl.querySelector("g.cursorscrolltransform").setAttribute("transform", "translate(" + (- codeDiv.getScrollLeft()) + " " + (- codeDiv.getScrollTop()) + ")");

    // Main cursor
    if (pointerDownHandle == 1)
    {
      ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
    }
    else
    {
      ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(0)).setAttribute("transform", "translate(" + (x) + " " + (y + cursorDiv.getOffsetHeight() + HANDLE_SIZE) + ")");
    }
    cursorHandle1.x = x;
    cursorHandle1.y = y + cursorDiv.getOffsetHeight() + HANDLE_SIZE + 2;
    cursorHandle1.xOffset = (x + (double)cursorDiv.getOffsetWidth() / 2) - cursorHandle1.x; 
    cursorHandle1.yOffset = (y + (double)cursorDiv.getOffsetHeight() * 0.8) - cursorHandle1.y;
    // Secondary cursor
    CursorRect selectionCursorRect = null;
    if (selectionCursorPos != null)
    {
      selectionCursorRect = RenderedCursorPosition.inStatements(codeList, selectionCursorPos, 0, renderedHitBoxes);
    }
    // Draw caret for the secondary cursor
    Element caretCursor = cursorOverlayEl.querySelector(".cursorcaret"); 
    if (selectionCursorRect != null)
    {
      caretCursor.getStyle().clearDisplay();
      caretCursor.setAttribute("x1", "" + selectionCursorRect.left);
      caretCursor.setAttribute("x2", "" + selectionCursorRect.left);
      caretCursor.setAttribute("y1", "" + selectionCursorRect.top);
      caretCursor.setAttribute("y2", "" + selectionCursorRect.bottom);
    }
    else
      caretCursor.getStyle().setDisplay(Display.NONE);
    // Secondary cursor handle
    if (selectionCursorRect == null)
    {
      // If there is no secondary cursor to draw a handle around, draw
      // the handle relative to the main cursor instead  
      selectionCursorRect = new CursorRect(x, y, y + cursorDiv.getOffsetHeight());
    }
    x = selectionCursorRect.left;
    y = selectionCursorRect.top;
    if (pointerDownHandle == 2)
    {
      ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (pointerDownX) + " " + (pointerDownY) + ")");
//      cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
//      cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
    else
    {
      if (selectionCursorPos != null)
      {
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      else
      {
        ((Element)cursorOverlayEl.querySelectorAll(".cursorhandle").item(1)).setAttribute("transform", "translate(" + (x + 2.5 * HANDLE_SIZE) + " " + (selectionCursorRect.bottom + HANDLE_SIZE) + ")");
      }
      cursorHandle2.x = x;
      cursorHandle2.y = selectionCursorRect.bottom + HANDLE_SIZE + 2;
      cursorHandle2.xOffset = selectionCursorRect.left - cursorHandle2.x; 
      cursorHandle2.yOffset = (selectionCursorRect.top * 0.2 + selectionCursorRect.bottom * 0.8) - cursorHandle2.y;
    }
  }
  
//  void createCursorHandleSvg(SVGElement parent, CursorHandle handle)
//  {
//    SVGCircleElement handleSvg = Browser.getDocument().createSVGCircleElement();
//    parent.appendChild(handleSvg);
//    handleSvg.setAttribute("cx", "" + handle.x);
//    handleSvg.setAttribute("cy", "" + handle.y);
//    handleSvg.setAttribute("r", "" + HANDLE_SIZE);
//    handleSvg.getStyle().setProperty("fill", "none");
//    handleSvg.getStyle().setProperty("stroke", "#000");
//    handleSvg.getStyle().setProperty("stroke-width", "1px");
//    
//  }
}
