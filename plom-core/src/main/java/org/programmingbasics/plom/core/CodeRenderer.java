package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Map;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Unit;
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

  static class TokenRenderer implements Token.TokenVisitor3<Element, CodePosition, Integer, RenderedHitBox>
  {
    Document doc;
    TokenRenderer(Document doc)
    {
      this.doc = doc;
    }
    @Override
    public Element visitSimpleToken(SimpleToken token, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      DivElement div = doc.createDivElement();
      div.setClassName("token");
      div.setTextContent(token.contents);
      if (hitBox != null)
        hitBox.el = div;
      return div;
    }
    @Override
    public Element visitOneExpressionOneBlockToken(
        OneExpressionOneBlockToken token, CodePosition pos, Integer level, RenderedHitBox hitBox)
    {
      DivElement div = doc.createDivElement();
      div.setClassName("blocktoken");

      RenderedHitBox exprHitBox = null;
      RenderedHitBox blockHitBox = null;
      if (hitBox != null)
      {
        exprHitBox = new RenderedHitBox();
        exprHitBox.children = new ArrayList<>();
        blockHitBox = new RenderedHitBox();
        blockHitBox.children = new ArrayList<>();
        hitBox.children = new ArrayList<>();
        hitBox.children.add(exprHitBox);
        hitBox.children.add(blockHitBox);
      }

      DivElement startLine = doc.createDivElement();
      SpanElement start = doc.createSpanElement();
      start.setTextContent(token.contents + " (");
      SpanElement expression = doc.createSpanElement();
      renderLine(token.expression, pos != null && pos.getOffset(level) == 0 ? pos : null, level + 1, expression, this, exprHitBox);
      if (token.expression.tokens.isEmpty())
        expression.setTextContent("\u00A0");
      SpanElement middle = doc.createSpanElement();
      middle.setTextContent(") {");
      startLine.appendChild(start);
      startLine.appendChild(expression);
      startLine.appendChild(middle);

      DivElement block = doc.createDivElement();
      block.getStyle().setPaddingLeft(1, Unit.EM);
      renderStatementContainer(block, token.block, pos != null && pos.getOffset(level) == 1 ? pos : null, level + 1, blockHitBox);

      DivElement endLine = doc.createDivElement();
      endLine.setTextContent("}");

      div.appendChild(startLine);
      div.appendChild(block);
      div.appendChild(endLine);
      if (hitBox != null)
        hitBox.el = div;
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
      renderLine(line, pos != null && lineno == pos.getOffset(level) ? pos : null, level + 1, div, renderer, lineHitBox);

      codeDiv.appendChild(div);
      lineno++;
    }
  }

  static void renderLine(TokenContainer line, CodePosition pos, int level, Element div, TokenRenderer renderer, RenderedHitBox lineHitBox)
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
      RenderedHitBox hitBox = null;
      if (lineHitBox != null)
        hitBox = new RenderedHitBox();
      Element el = tok.visit(renderer, pos != null && pos.hasOffset(level + 1) ? pos : null, level + 1, hitBox);
      div.appendChild(el);
      if (lineHitBox != null)
        lineHitBox.children.add(hitBox);
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

  CodePosition renderAndHitDetect(int x, int y, DivElement codeDiv, StatementContainer codeList, CodePosition oldPos)
  {
    RenderedHitBox renderedHitBoxes = new RenderedHitBox(null);
    renderedHitBoxes.children = new ArrayList<>();
    codeDiv.setInnerHTML("");
    render(codeDiv, codeList, oldPos, renderedHitBoxes);
    CodePosition pos = new CodePosition();
    return hitDetectStatementContainer(x, y, codeList, renderedHitBoxes, pos, 0);
  }

  private CodePosition hitDetectStatementContainer(int x, int y,
      StatementContainer statements,
      RenderedHitBox renderedHitBoxes,
      CodePosition newPos, 
      int level)
  {
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
      newPos.setOffset(level, lineno);
      return hitDetectTokens(x, y, statements.statements.get(lineno), renderedHitBoxes.children.get(lineno), newPos, level + 1);
    }
    return null;
  }

  private CodePosition hitDetectTokens(int x, int y, 
      TokenContainer tokens, RenderedHitBox renderedLineHitBoxes,
      CodePosition newPos, int level)
  {
    // Find which token that mouse position is over
    int bestMatchX = -1;
    int tokenno = 0;
    for (int n = 0; n < renderedLineHitBoxes.children.size(); n++)
    {
      Element el = renderedLineHitBoxes.children.get(n).el;
      if (el.getOffsetLeft() < x && el.getOffsetLeft() > bestMatchX)
      {
        bestMatchX = el.getOffsetLeft();
        tokenno = n;
      }
    }
    // Check if mouse is past the end of the last token
    if (tokenno == renderedLineHitBoxes.children.size() - 1)
    {
      Element el = renderedLineHitBoxes.children.get(tokenno).el;
      if (el.getOffsetLeft() + el.getOffsetWidth() < x)
      {
        tokenno++;
      }
    }
    // Update the cursor position
    newPos.setOffset(level, tokenno);
    return newPos;
  }
  
}
