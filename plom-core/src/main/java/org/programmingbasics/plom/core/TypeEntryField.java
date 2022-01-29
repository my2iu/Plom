package org.programmingbasics.plom.core;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.CodeWidgetBase.VariableContextConfigurator;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.TypeSuggester;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.GetToken;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.RenderedHitBox;
import org.programmingbasics.plom.core.view.RenderedTokenHitBox;
import org.programmingbasics.plom.core.view.SetTypeToken;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.ClientRect;
import elemental.html.DivElement;
import elemental.svg.SVGDocument;

/**
 * Handles UI logic for a field where a type can be entered
 */
public class TypeEntryField
{
  public TypeEntryField(Token.ParameterToken type, DivElement div, SimpleEntry simpleEntry, boolean isReturnType, ConfigureGlobalScope globalConfigurator, VariableContextConfigurator variableContextConfigurator, SvgCodeRenderer.SvgTextWidthCalculator widthCalculator, int maxClientWidth, Element scrollableDiv, Element scrollableExtraPaddingDiv)
  {
    this.type = type;
    this.simpleEntry = simpleEntry;
    this.isReturnType = isReturnType;
    fieldDiv = div;
    this.globalConfigurator = globalConfigurator;
    this.variableContextConfigurator = variableContextConfigurator;
    this.widthCalculator = widthCalculator;
    this.maxClientWidth = maxClientWidth;
    this.scrollableDiv = scrollableDiv;
    this.scrollableExtraPaddingDiv = scrollableExtraPaddingDiv;
    hookCodeClick(div);
  }
  boolean useSvg = true;
  boolean isReturnType;  // Should void be allowed as a type
  DivElement fieldDiv;
  SimpleEntry simpleEntry;
  Token.ParameterToken type;
  CodePosition cursorPos;
  RenderedHitBox hitBox;
  BiConsumer<Token.ParameterToken, Boolean> listener;
  SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
  int maxClientWidth;
  // A div that can be scrolled to ensure that the type being edited is not overlapped by another element
  Element scrollableDiv;
  Element scrollableExtraPaddingDiv;
  
  /** 
   * Allows for the configuration of what global variables/types there are
   * for type checking.
   * */
  ConfigureGlobalScope globalConfigurator; 

  /** To configure object variables and function arguments that are accessible for code completion */
  VariableContextConfigurator variableContextConfigurator; 

  /** 
   * You don't actually need to listen for changes because if there is an
   * existing type, the contents of the type will actually be rewritten with
   * the new type, but it's safer to have one, and it's needed if you don't
   * have an existing type with the type field being blank at the beginning
   */
  public void setChangeListener(BiConsumer<Token.ParameterToken, Boolean> listener)
  {
    this.listener = listener;
  }
  
  void hookCodeClick(DivElement div)
  {
    div.addEventListener(Event.CLICK, (evt)-> {
      evt.preventDefault();
      MouseEvent mevt = (MouseEvent)evt;
      ClientRect rect = div.getBoundingClientRect();
      int x = (int)(mevt.getClientX() - rect.getLeft()) + div.getScrollLeft();
      int y = (int)(mevt.getClientY() - rect.getTop()) + div.getScrollTop();

      if (hitBox != null)
      {
        cursorPos = HitDetect.hitDetectTypeField(x, y, type, hitBox, new CodePosition(), 0);
      }
      else
      {
        cursorPos = new CodePosition();
      }
      Token hitToken = GetToken.inLine(new TokenContainer(Arrays.asList(type)), null, cursorPos, 0);
      if (hitToken == null)
      {
        hitToken = new Token.ParameterToken(
            Token.ParameterToken.splitVarAtColons("@"), 
            Token.ParameterToken.splitVarAtColonsForPostfix("@"), 
            Symbol.AtType);
        type = (ParameterToken) SetTypeToken.set(type, hitToken, cursorPos);
      }
      
      int doNotCoverLeft = 0, doNotCoverRight = 0;
      final int MIN_TOKEN_SIZE_FOR_DO_NOT_COVER = 50;
      RenderedHitBox typeHitBox = RenderedTokenHitBox.inTypeField(type, hitBox, cursorPos);
      if (typeHitBox != null)
      {
        doNotCoverLeft = (int)(typeHitBox.getOffsetLeft() + div.getOffsetLeft() + scrollableExtraPaddingDiv.getOffsetLeft());
        doNotCoverRight = doNotCoverLeft + Math.max(typeHitBox.getOffsetWidth(), MIN_TOKEN_SIZE_FOR_DO_NOT_COVER);
      }
      
      String initialValue = ((Token.ParameterToken)hitToken).getTextContent().substring(1);
      CodeCompletionContext suggestionContext = CodePanel.calculateSuggestionContext(null, null, globalConfigurator, variableContextConfigurator);
      simpleEntry.showFor("@", "", null, initialValue, hitToken, true, new TypeSuggester(suggestionContext, isReturnType && !cursorPos.hasOffset(1)), this::simpleEntryInput, this::simpleEntryBackspaceAll);
      simpleEntry.scrollForDoNotCover(scrollableDiv, scrollableExtraPaddingDiv, doNotCoverLeft, doNotCoverRight);
      simpleEntry.setEraseButtonVisible(true);

      render();
    }, false);
  }
  
  <U extends Token> void simpleEntryInput(String val, boolean isFinal, U token, boolean isEdit)
  {
    ((Token.ParameterToken)token).setContents(
        Token.ParameterToken.splitVarAtColons(val),
        Token.ParameterToken.splitVarAtColonsForPostfix(val));
//    if (advanceToNext && isFinal)
//      NextPosition.nextPositionOfStatements(codeList, cursorPos, 0);
    
    if (isFinal)
    {
      cursorPos = null;
      simpleEntry.setVisible(false);
      simpleEntry.forceSafariBlur();
    }
    render();
    if (listener != null)
      listener.accept(type, isFinal);
    
  }
  
  boolean simpleEntryBackspaceAll(boolean isEdit)
  {
    // Don't do anything special is you delete past the beginning of the input entry
    return true;
  }
  
  public void render()
  {
    if (useSvg)
      hitBox = SvgCodeRenderer.renderTypeToken(fieldDiv, type, cursorPos, widthCalculator, maxClientWidth);
    else
      hitBox = CodeRenderer.renderTypeToken(fieldDiv, type, cursorPos);
//    CodeRenderer.render(fieldDiv, 
//        type == null ? new StatementContainer(new TokenContainer()) : new StatementContainer(new TokenContainer(type)), 
//            cursorPos, null, new ErrorList());

  }
}
