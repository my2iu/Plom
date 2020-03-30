package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.programmingbasics.plom.core.ast.LL1Parser;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
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
      
      renderTokens(codeDiv, codeList, cursorPos, null, null);
      showPredictedTokenInput(choicesDiv);
      hookCodeClick(codeDiv);
   }
   
   StatementContainer codeList = new StatementContainer();
   {
      codeList.statements.addAll(Arrays.asList(
            new TokenContainer(Arrays.asList(
                  new Token.SimpleToken("1", Symbol.Number),
                  new Token.SimpleToken("+", Symbol.Number),
                  new Token.SimpleToken("1", Symbol.Number)
                  )),
            new TokenContainer(Arrays.asList()),
            new TokenContainer(Arrays.asList(
                  new Token.SimpleToken("a", Symbol.Number),
                  new Token.SimpleToken("=", Symbol.Number),
                  new Token.SimpleToken("4", Symbol.Number)
                  ))
            ));
   }
   DivElement codeDiv;
   DivElement choicesDiv;
   CodePosition cursorPos = new CodePosition();
   
   /**
    * Returns a mapping of divs for each line and their line numbers
    */
   static void renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, Map<Element, Integer> lineDivs, RenderedHitBox renderedHitBoxes)
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
      int lineno = 0;
      for (TokenContainer line: codeList.statements)
      {
         DivElement div = doc.createDivElement();
         RenderedHitBox lineHitBox = null;
         if (renderedHitBoxes != null)
         {
            lineHitBox = new RenderedHitBox(div);
            lineHitBox.children = new ArrayList<>();
            renderedHitBoxes.children.add(lineHitBox);
         }
         int tokenno = 0;
         for (Token tok: line.tokens)
         {
            if (lineno == pos.line && tokenno == pos.token)
            {
               DivElement toInsert = doc.createDivElement();
               toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
               div.appendChild(toInsert.querySelector("div"));
            }
            Element el = tok.visit(renderer);
            div.appendChild(el);
            if (lineHitBox != null)
            {
               lineHitBox.children.add(new RenderedHitBox(el));
            }
            tokenno++;
         }
         if (lineno == pos.line && pos.token == line.tokens.size()) 
         {
            DivElement toInsert = doc.createDivElement();
            toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
            div.appendChild(toInsert.querySelector("div"));
         }
         else if (line.tokens.isEmpty())
            div.setTextContent("\u00A0");
         codeDiv.appendChild(div);
         if (lineDivs != null)
            lineDivs.put(div, lineno);
         lineno++;
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
   
   void insertToken(CodePosition pos, String tokenText, Symbol tokenType)
   {
      TokenContainer line = codeList.statements.get(pos.line);
      line.tokens.add(pos.token, new SimpleToken(tokenText, tokenType));
      pos.token++;
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null, null);
      showPredictedTokenInput(choicesDiv);
   }
   
   void showPredictedTokenInput(DivElement choicesDiv)
   {
      choicesDiv.setInnerHTML("");
      
      // Parse the current statement up to the cursor position
      TokenContainer curLine = codeList.statements.get(cursorPos.line);
      LL1Parser stmtParser = new LL1Parser();
      stmtParser.stack.add(Symbol.Statement);
      for (int n = 0; n < cursorPos.token; n++)
      {
         Token tok = curLine.tokens.get(n);
         tok.visit(stmtParser);
      }
      Set<Symbol> allowedSymbols = stmtParser.allowedNextSymbols();
      
      // Buttons for next and enter
      choicesDiv.appendChild(makeButton("\u27a0", () -> {}));
      if (allowedSymbols.contains(Symbol.EndStatement))
      {
         choicesDiv.appendChild(makeButton("\u21b5", () -> {
            TokenContainer line = codeList.statements.get(cursorPos.line);
            TokenContainer newline = new TokenContainer(line.tokens.subList(cursorPos.token, line.tokens.size()));
            for (int n = line.tokens.size() - 1; n >= cursorPos.token; n--)
               line.tokens.remove(n);
            cursorPos.line++;
            codeList.statements.add(cursorPos.line, newline);
            cursorPos.token = 0;
            codeDiv.setInnerHTML("");
            renderTokens(codeDiv, codeList, cursorPos, null, null);
            showPredictedTokenInput(choicesDiv);
         }));
      }
      
      // Just some random tokens for initial prototyping
      for (Symbol sym: allowedSymbols)
      {
         if (sym == Symbol.EndStatement) continue;
         String text = "Unknown";
         switch(sym)
         {
         case Plus: text = "+"; break;
         case Minus: text = "-"; break;
         case Multiply: text = "*"; break;
         case Divide: text = "/"; break;
         case OpenParenthesis: text = "("; break;
         case ClosedParenthesis: text = ")"; break;
         case DUMMY_COMMENT: text = "//"; break;
         case Number: text = "123"; break;
         case String: text = "\"...\""; break;
         default:
         }
         String tokenText = text;
         choicesDiv.appendChild(makeButton(tokenText, () -> { insertToken(cursorPos, tokenText, sym); }));
      }
   }
   
   void hookCodeClick(DivElement div)
   {
      div.addEventListener(Event.CLICK, (evt)-> {
         MouseEvent mevt = (MouseEvent)evt;
         int x = mevt.getClientX() + div.getScrollLeft();
         int y = mevt.getClientY() + div.getScrollTop();
//         Map<Element, Integer> lineDivs = new HashMap<>();
         RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
         renderedHitBoxes.children = new ArrayList<>();
         codeDiv.setInnerHTML("");
         renderTokens(codeDiv, codeList, cursorPos, null, renderedHitBoxes);
         // Find which line matches the mouse position
         int bestMatchY = -1;
         int lineno = -1;
         for (int n = 0; n < renderedHitBoxes.children.size(); n++)
         {
            Element el = renderedHitBoxes.children.get(n).el;
            if (el.getOffsetTop() < y && el.getOffsetTop() > bestMatchY)
            {
               bestMatchY = el.getOffsetTop();
               lineno = n;
            }
         }
         if (lineno >= 0)
         {
            // Find which token that mouse position is over
            int bestMatchX = -1;
            int tokenno = 0;
            for (int n = 0; n < renderedHitBoxes.children.get(lineno).children.size(); n++)
            {
               Element el = renderedHitBoxes.children.get(lineno).children.get(n).el;
               if (el.getOffsetLeft() < x && el.getOffsetLeft() > bestMatchX)
               {
                  bestMatchX = el.getOffsetLeft();
                  tokenno = n;
               }
            }
            // Check if mouse is past the end of the last token
            if (tokenno == renderedHitBoxes.children.get(lineno).children.size() - 1)
            {
               Element el = renderedHitBoxes.children.get(lineno).children.get(tokenno).el;
               if (el.getOffsetLeft() + el.getOffsetWidth() < x)
               {
                  tokenno++;
               }
            }
            // Update the cursor position
            cursorPos = new CodePosition();
            cursorPos.line = lineno;
            cursorPos.token = tokenno;
            codeDiv.setInnerHTML("");
            renderTokens(codeDiv, codeList, cursorPos, null, null);
            showPredictedTokenInput(choicesDiv);
        }
      }, false);
   }
   
}