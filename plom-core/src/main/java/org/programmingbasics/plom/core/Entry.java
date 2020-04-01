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
      
      renderTokens(codeDiv, codeList, cursorPos, null);
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
   static void renderTokens(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
   {
      new CodeRenderer().render(codeDiv, codeList, pos, renderedHitBoxes);
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
      TokenContainer line = codeList.statements.get(pos.getOffset(0));
      line.tokens.add(pos.getOffset(1), new SimpleToken(tokenText, tokenType));
      pos.setOffset(1, pos.getOffset(1) + 1);
      codeDiv.setInnerHTML("");
      renderTokens(codeDiv, codeList, cursorPos, null);
      showPredictedTokenInput(choicesDiv);
   }
   
   void showPredictedTokenInput(DivElement choicesDiv)
   {
      choicesDiv.setInnerHTML("");
      
      // Parse the current statement up to the cursor position
      TokenContainer curLine = codeList.statements.get(cursorPos.getOffset(0));
      LL1Parser stmtParser = new LL1Parser();
      stmtParser.stack.add(Symbol.Statement);
      for (int n = 0; n < cursorPos.getOffset(1); n++)
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
            TokenContainer line = codeList.statements.get(cursorPos.getOffset(0));
            TokenContainer newline = new TokenContainer(line.tokens.subList(cursorPos.getOffset(1), line.tokens.size()));
            for (int n = line.tokens.size() - 1; n >= cursorPos.getOffset(1); n--)
               line.tokens.remove(n);
            cursorPos.setOffset(0, cursorPos.getOffset(0) + 1);
            codeList.statements.add(cursorPos.getOffset(0), newline);
            cursorPos.setOffset(1, 0);
            codeDiv.setInnerHTML("");
            renderTokens(codeDiv, codeList, cursorPos, null);
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
         case COMPOUND_IF: text = "if"; break;
         case COMPOUND_ELSE: text = "else"; break;
         case COMPOUND_ELSEIF: text = "elseif"; break;
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
         renderTokens(codeDiv, codeList, cursorPos, renderedHitBoxes);
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
            cursorPos.setOffset(0, lineno);
            cursorPos.setOffset(1, tokenno);
            codeDiv.setInnerHTML("");
            renderTokens(codeDiv, codeList, cursorPos, null);
            showPredictedTokenInput(choicesDiv);
        }
      }, false);
   }
   
}