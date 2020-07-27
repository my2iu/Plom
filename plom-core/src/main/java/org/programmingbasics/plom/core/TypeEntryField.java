package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.CodePosition;
import org.programmingbasics.plom.core.view.CodeRenderer;
import org.programmingbasics.plom.core.view.HitDetect;
import org.programmingbasics.plom.core.view.NextPosition;
import org.programmingbasics.plom.core.view.RenderedHitBox;

import elemental.css.CSSStyleDeclaration.Display;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.ClientRect;
import elemental.html.DivElement;

/**
 * Handles UI logic for a field where a type can be entered
 */
public class TypeEntryField
{
  public TypeEntryField(Token.ParameterToken type, DivElement div, SimpleEntry simpleEntry)
  {
    this.type = type;
    this.simpleEntry = simpleEntry;
    fieldDiv = div;
    hookCodeClick(div);
  }
  DivElement fieldDiv;
  SimpleEntry simpleEntry;
  Token.ParameterToken type;
  CodePosition cursorPos;
  RenderedHitBox hitBox;
  
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
      if (type == null)
        type = new Token.ParameterToken(
                Token.ParameterToken.splitVarAtColons("@"), 
                Token.ParameterToken.splitVarAtColonsForPostfix("@"), 
                Symbol.AtType);
      Token hitToken = type;
      String initialValue = ((Token.ParameterToken)hitToken).getTextContent().substring(1);
      simpleEntry.showFor("@", "", null, initialValue, hitToken, true, (prefix) -> {
        List<String> toReturn = new ArrayList<>();
        toReturn.add("string");
        toReturn.add("number");
        toReturn.add("object");
        toReturn.add("boolean");
        return toReturn;
      }, this::simpleEntryInput);

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
    render();
    
    if (isFinal)
    {
      cursorPos = null;
      simpleEntry.setVisible(false);
    }
    
  }
  
  public void render()
  {
    hitBox = CodeRenderer.renderTypeToken(fieldDiv, type, cursorPos);
//    CodeRenderer.render(fieldDiv, 
//        type == null ? new StatementContainer(new TokenContainer()) : new StatementContainer(new TokenContainer(type)), 
//            cursorPos, null, new ErrorList());

  }
}
