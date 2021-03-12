package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.SvgCodeRenderer.SvgTextWidthCalculator;

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
    positioning.maxNestingForLine = 1;
    positioning.currentNestingInLine = 0;
    tok.visit(tokenRenderer, returned, positioning, 0, new CodePosition(), hitBox);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>22</text>", returned.svgString);
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
    supplement.nesting = new CodeNestingCounter();
    supplement.nesting.calculateNestingForLine(line);
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplement, 10, new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    positioning.fontSize = 10;
    supplement.nesting.calculateNestingForLine(line);
    positioning.maxNestingForLine = supplement.nesting.expressionNesting.get(line);
    positioning.currentNestingInLine = 0;
    SvgCodeRenderer.renderLine(line, returned, positioning, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplement);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>1</text>\n" + 
        "<rect x='20.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='25.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='40.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='45.0' y='13.0' class='codetoken'>2</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(18, returned.height, 0.001);
  }
  
  @Test
  public void testStatementContainerOfLinesOfSimpleTokens()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("22 adf df", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.SimpleToken("55", Symbol.Number)
            )
        );
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='105.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='0.0' width='130.0' height='18' class='codetoken'/><text x='125.0' y='13.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='18.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='31.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(36, positioning.lineTop, 0.001);
  }

  @Test
  public void testSimpleWideToken()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.WideToken("// Comment", Symbol.DUMMY_COMMENT),
            new Token.SimpleToken("22 adf df", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.SimpleToken("55", Symbol.Number)
            )
        );
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken tokencomment'>// Comment</text>\n" + 
        "<rect x='0.0' y='18.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='31.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='18.0' width='20.0' height='18' class='codetoken'/><text x='105.0' y='31.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='18.0' width='130.0' height='18' class='codetoken'/><text x='125.0' y='31.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='36.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='49.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(54, positioning.lineTop, 0.001);
  }
  
  @Test
  public void testParameterToken()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.WideToken("// Comment", Symbol.DUMMY_COMMENT),
            Token.ParameterToken.fromContents("@Type", Symbol.AtType),
            Token.ParameterToken.fromContents(".a:", Symbol.DotVariable,
                new TokenContainer()),
            Token.ParameterToken.fromContents(".a:b:c:", Symbol.DotVariable,
                new TokenContainer(
                    Token.ParameterToken.fromContents(".d:", Symbol.DotVariable, 
                        new TokenContainer(new Token.SimpleToken("12", Symbol.Number)))),
                new TokenContainer(),
                new TokenContainer(new Token.SimpleToken("32", Symbol.Number))
                ),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.SimpleToken("55", Symbol.Number)
            )
        );
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken tokencomment'>// Comment</text>\n" + 
        "<rect x='0.0' y='18.0' width='60.0' height='30' class='codetoken'/><text x='5.0' y='37.0' class='codetoken'>@Type</text>\n" + 
        "<rect x='60.0' y='18.0' width='45.0' height='30' class='codetoken'/><text x='65.0' y='37.0' class='codetoken'>.a:</text>\n" + 
        "<rect x='105.0' y='18.0' width='210.0' height='30' class='codetoken'/><text x='110.0' y='37.0' class='codetoken'>.a:</text><text x='225.0' y='37.0' class='codetoken'>b:</text><text x='255.0' y='37.0' class='codetoken'>c:</text>\n" + 
        "<rect x='145.0' y='21.0' width='75.0' height='24' class='codetoken'/><text x='150.0' y='37.0' class='codetoken'>.d:</text>\n" + 
        "<rect x='185.0' y='24.0' width='30.0' height='18' class='codetoken'/><text x='190.0' y='37.0' class='codetoken'>12</text>\n" + 
        "<rect x='280.0' y='21.0' width='30.0' height='24' class='codetoken'/><text x='285.0' y='37.0' class='codetoken'>32</text>\n" + 
        "<rect x='315.0' y='18.0' width='20.0' height='30' class='codetoken'/><text x='320.0' y='37.0' class='codetoken'>+</text>\n" + 
        "<rect x='335.0' y='18.0' width='130.0' height='30' class='codetoken'/><text x='340.0' y='37.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='48.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='61.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(54, positioning.lineTop, 0.001);
  }

  @Test
  public void testWideToken()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.WideToken("// Comment", Symbol.DUMMY_COMMENT),
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    new Token.SimpleToken("true", Symbol.TrueLiteral)),
                new StatementContainer(
                    new TokenContainer())),
            new Token.SimpleToken("22 adf df", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.SimpleToken("55", Symbol.Number)
            )
        );
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning();
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='105.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='0.0' width='130.0' height='18' class='codetoken'/><text x='125.0' y='13.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='18.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='31.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(36, positioning.lineTop, 0.001);
    
  }
}
