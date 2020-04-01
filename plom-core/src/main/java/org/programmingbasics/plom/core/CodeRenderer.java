package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Map;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;

public class CodeRenderer
{
   void render(DivElement codeDiv, StatementContainer codeList, CodePosition pos, RenderedHitBox renderedHitBoxes)
   {
      renderStatementContainer(codeDiv, codeList, pos, 0, renderedHitBoxes);
   }
   
   static class TokenRenderer implements Token.TokenVisitor<Element>
   {
      Document doc;
      TokenRenderer(Document doc)
      {
         this.doc = doc;
      }
      @Override
      public Element visitSimpleToken(SimpleToken token)
      {
         DivElement div = doc.createDivElement();
         div.setClassName("token");
         div.setTextContent(token.contents);
         return div;
      }
      @Override
      public Element visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token)
      {
         DivElement div = doc.createDivElement();
         div.setClassName("token");
         
         DivElement startLine = doc.createDivElement();
         SpanElement start = doc.createSpanElement();
         start.setTextContent(token.contents + " (");
         SpanElement expression = doc.createSpanElement();
         SpanElement middle = doc.createSpanElement();
         middle.setTextContent(") {");
         startLine.appendChild(start);
         startLine.appendChild(expression);
         startLine.appendChild(middle);
         
         DivElement endLine = doc.createDivElement();
         endLine.setTextContent("}");
         
         div.appendChild(startLine);
         div.appendChild(endLine);
         return div;
      }
   }

   static void renderStatementContainer(DivElement codeDiv, StatementContainer codeList, CodePosition pos, int level, RenderedHitBox renderedHitBoxes)
   {
      Document doc = codeDiv.getOwnerDocument();
      
      TokenRenderer renderer = new TokenRenderer(doc);
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
         renderStatement(line, lineno == pos.getOffset(level) ? pos : null, level + 1, div, renderer, lineHitBox);
         
         codeDiv.appendChild(div);
         lineno++;
      }
   }
   
   static void renderStatement(TokenContainer line, CodePosition pos, int level, DivElement div, TokenRenderer renderer, RenderedHitBox lineHitBox)
   {
      Document doc = div.getOwnerDocument();
      int tokenno = 0;
      for (Token tok: line.tokens)
      {
         if (pos != null && tokenno == pos.getOffset(level))
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
      if (pos != null && pos.getOffset(level) == line.tokens.size()) 
      {
         DivElement toInsert = doc.createDivElement();
         toInsert.setInnerHTML(UIResources.INSTANCE.getCursorHtml().getText());
         div.appendChild(toInsert.querySelector("div"));
      }
      else if (line.tokens.isEmpty())
         div.setTextContent("\u00A0");
   }

}
