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
      
      StatementContainer codeList = new StatementContainer();
      codeList.statements.addAll(Arrays.asList(
            new TokenContainer(Arrays.asList(
                  new Token.SimpleToken("1"),
                  new Token.SimpleToken("+"),
                  new Token.SimpleToken("1")
                  )),
            new TokenContainer(Arrays.asList()),
            new TokenContainer(Arrays.asList(
                  new Token.SimpleToken("a"),
                  new Token.SimpleToken("="),
                  new Token.SimpleToken("4")
                  ))
            ));
      renderTokens(codeDiv, codeList);
   }
   
   void renderTokens(DivElement codeDiv, StatementContainer codeList)
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
      for (TokenContainer line: codeList.statements)
      {
         DivElement div = doc.createDivElement();
         for (Token tok: line.tokens)
         {
            Element el = tok.visit(renderer);
            div.appendChild(el);
         }
         if (line.tokens.isEmpty())
            div.setTextContent("\u00A0");
         codeDiv.appendChild(div);
         
      }
      
   }
   
}