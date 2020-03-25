package org.programmingbasics.plom.core;

import java.util.Arrays;

import org.programmingbasics.plom.core.Token.SimpleToken;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;

public class Entry implements EntryPoint
{
   @Override
   public void onModuleLoad()
   {
      DivElement mainDiv = (DivElement)Browser.getDocument().querySelector("div.main");
      mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());
      
      DivElement codeDiv = (DivElement)mainDiv.querySelector("div.code");
      DivElement choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
      
      TokenContainer codeList = new TokenContainer();
      codeList.tokens.addAll(Arrays.asList(
            new Token.SimpleToken("1"),
            new Token.SimpleToken("+"),
            new Token.SimpleToken("1")));
      renderTokens(codeDiv, codeList);
   }
   
   void renderTokens(DivElement codeDiv, TokenContainer codeList)
   {
      Document doc = Browser.getDocument();
      class TokenRenderer implements Token.TokenVisitor<Element>
      {
         @Override
         public Element visitSimpleToken(SimpleToken token)
         {
            DivElement div = doc.createDivElement();
            div.setClassName("token");
            div.setTextContent(token.contents);
            return div;
         }
      }
      
      TokenRenderer renderer = new TokenRenderer();
      for (Token tok: codeList.tokens)
      {
         Element el = tok.visit(renderer);
         codeDiv.appendChild(el);
      }
      
   }
   
}