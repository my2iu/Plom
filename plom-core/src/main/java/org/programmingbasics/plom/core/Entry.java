package org.programmingbasics.plom.core;

import java.util.Arrays;

import org.programmingbasics.plom.core.Token.SimpleToken;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;
import elemental.html.DivElement;

public class Entry implements EntryPoint
{
   @Override
   public void onModuleLoad()
   {
      DivElement mainDiv = (DivElement)Browser.getDocument().querySelector("div.main");
      mainDiv.setInnerHTML(UIResources.INSTANCE.getCodePanelHtml().getText());
      
      codeDiv = (DivElement)mainDiv.querySelector("div.code");
      choicesDiv = (DivElement)mainDiv.querySelector("div.choices");
      
      renderTokens(codeDiv, codeList);
      showTokenInput(choicesDiv);
   }
   
   StatementContainer codeList = new StatementContainer();
   {
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
   }
   DivElement codeDiv;
   DivElement choicesDiv;
   CodePosition cursorPos = new CodePosition();
   
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
   
   Element makeButton(String text, Runnable onclick)
   {
      Document doc = Browser.getDocument();
      DivElement div = doc.createDivElement();
      div.setClassName("tokenchoice");
      div.setTextContent(text);
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.appendChild(div);
      a.addEventListener(Event.CLICK, (evt)-> {
         evt.preventDefault();
         onclick.run();
      }, false);
      return a;
   }
   
   void insertToken(CodePosition pos, String tokenText)
   {
      TokenContainer line = codeList.statements.get(pos.line);
      line.tokens.add(pos.token, new SimpleToken(tokenText));
      pos.token++;
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList);
   }
   
   void showTokenInput(DivElement choicesDiv)
   {
      // Buttons for next and enter
      choicesDiv.appendChild(makeButton("\u27a0", () -> {}));
      choicesDiv.appendChild(makeButton("\u21b5", () -> {
         TokenContainer line = codeList.statements.get(cursorPos.line);
         TokenContainer newline = new TokenContainer(line.tokens.subList(cursorPos.token, line.tokens.size()));
         for (int n = line.tokens.size() - 1; n >= cursorPos.token; n--)
            line.tokens.remove(n);
         cursorPos.line++;
         codeList.statements.add(cursorPos.line, newline);
         cursorPos.token = 0;
         codeDiv.setInnerHTML("");
         renderTokens(codeDiv, codeList);
      }));
      
      // Just some random tokens for initial prototyping
      for (String tokenText: new String[] {"a", "+", "-", "5", "3"})
      {
         choicesDiv.appendChild(makeButton(tokenText, () -> { insertToken(cursorPos, tokenText); }));
      }
   }
   
}