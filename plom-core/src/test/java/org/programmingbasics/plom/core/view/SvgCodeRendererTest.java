package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class SvgCodeRendererTest extends TestCase
{
  static class SimpleWidthCalculator implements SvgCodeRenderer.TextWidthCalculator
  {
    @Override public double calculateWidth(String text)
    {
      return text.length() * 10;
    }
  }
  
  @Test
  public void testSimpleToken()
  {
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, 10, new SimpleWidthCalculator());
    Token tok = new Token.SimpleToken("22", Symbol.Number);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    tok.visit(tokenRenderer, returned, positioning, 0, new CodePosition(), hitBox);
    Assert.assertEquals("<rect width='30.0' height='18' class='codetoken'/><text x='5' y='13' class='codetoken'>22</text>", returned.svgString);
    Assert.assertEquals(30, returned.width, 0.001);
    Assert.assertEquals(18, returned.height, 0.002);
  }
  
  @Test
  public void testLineOfSimpleToken()
  {
    TokenContainer line = new TokenContainer(Arrays.asList(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number)));
    SvgCodeRenderer.RenderSupplementalInfo supplement = new SvgCodeRenderer.RenderSupplementalInfo();
    supplement.codeErrors = new ErrorList();
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplement, 10, new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    SvgCodeRenderer.renderLine(line, returned, new CodePosition(), 0, null, null, false, tokenRenderer, null, supplement);
    Assert.assertEquals("<rect width='30.0' height='18' class='codetoken'/><text x='5' y='13' class='codetoken'>22</text>", returned.svgString);
  }
}
