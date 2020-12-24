package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class SvgCodeRendererTest extends TestCase
{
  @Test
  public void testSimpleToken()
  {
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SvgCodeRenderer.TextWidthCalculator() {
      @Override public double calculateWidth(String text)
      {
        return text.length() * 10;
      }
    };
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, 10, widthCalculator);
    Token tok = new Token.SimpleToken("22", Symbol.Number);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    tok.visit(tokenRenderer, returned, positioning, 0, null, hitBox);
    Assert.assertEquals("", returned.svgString);
  }
}
