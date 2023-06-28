package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;

import junit.framework.TestCase;

public class SvgCodeRendererTest extends TestCase
{
  static final double DEFAULT_CANVAS_WIDTH = 1000;
  static final double THIN_CANVAS_WIDTH = 300;
  static class SimpleWidthCalculator implements SvgCodeRenderer.TextWidthCalculator
  {
    @Override public double calculateWidth(String text)
    {
      return text.length() * 10;
    }
    @Override public double getFontSize() { return 15; }
  }
  
  @Test
  public void testSimpleToken()
  {
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, 10, widthCalculator);
    Token tok = new Token.SimpleToken("22", Symbol.Number);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH, widthCalculator);
    positioning.maxNestingForLine = 1;
    positioning.currentNestingInLine = 0;
    tok.visit(tokenRenderer, returned, positioning, 0, new CodePosition(), hitBox);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>22</text>", returned.svgString);
    Assert.assertEquals(30, returned.width, 0.001);
    Assert.assertEquals(18, returned.height, 0.002);
  }

  @Test
  public void testSimpleTokenEscaping()
  {
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, 10, widthCalculator);
    Token tok = new Token.SimpleToken("\"This is a string with & and < and other symbols\"", Symbol.String);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH, widthCalculator);
    positioning.maxNestingForLine = 1;
    positioning.currentNestingInLine = 0;
    tok.visit(tokenRenderer, returned, positioning, 0, new CodePosition(), hitBox);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='570.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>\"This is a string with &amp; and &lt; and other symbols\"</text>", returned.svgString);
    Assert.assertEquals(570, returned.width, 0.001);
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
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplement, 10, widthCalculator);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH, widthCalculator);
    positioning.fontSize = 10;
    supplement.nesting.calculateNestingForLine(line);
    positioning.maxNestingForLine = supplement.nesting.expressionNesting.get(line);
    positioning.currentNestingInLine = 0;
    SvgCodeRenderer.renderLine(line, returned, positioning, 0, currentTokenPos, false, tokenRenderer, supplement, 0, false);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>1</text>\n" + 
        "<rect x='20.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='25.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='40.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='45.0' y='13.0' class='codetoken'>2</text>\n" + 
        "", returned.svgString);
    Assert.assertEquals(18, returned.height, 0.001);
  }

  @Test
  public void testLineOfSimpleTokenWithSelection()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("2", Symbol.Number)));
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    supplementalInfo.selectionStart = CodePosition.fromOffsets(0, 1);
    supplementalInfo.selectionEnd = CodePosition.fromOffsets(0, 2);
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH, widthCalculator);
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), widthCalculator);
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    RenderedHitBox hitBox = new RenderedHitBox();
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>1</text>\n" + 
        "<rect x='20.0' y='0.0' width='20.0' height='18' class='codetoken tokenselected'/><text x='25.0' y='13.0' class='codetoken tokenselected'>+</text>\n" + 
        "<rect x='40.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='45.0' y='13.0' class='codetoken'>2</text>\n" + 
        "", returned.svgString);
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
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='105.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='0.0' width='130.0' height='18' class='codetoken'/><text x='125.0' y='13.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='18.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='31.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    SvgCodeRenderer.TokenRendererPositioning positioning = renderPlainForPositioning(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals(36, positioning.lineTop, 0.001);
    Assert.assertEquals(36, returned.height, 0.001);
    Assert.assertEquals(250, returned.width, 0.001);
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
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='1000.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken tokencomment'>// Comment</text>\n" + 
        "<rect x='0.0' y='18.0' width='100.0' height='18' class='codetoken'/><text x='5.0' y='31.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='18.0' width='20.0' height='18' class='codetoken'/><text x='105.0' y='31.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='18.0' width='130.0' height='18' class='codetoken'/><text x='125.0' y='31.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='36.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='49.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    SvgCodeRenderer.TokenRendererPositioning positioning = renderPlainForPositioning(codeList, DEFAULT_CANVAS_WIDTH);
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
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='1000.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken tokencomment'>// Comment</text>\n" + 
        "<rect x='0.0' y='18.0' width='60.0' height='30.0' class='codetoken'/><text x='5.0' y='37.0' class='codetoken'>@Type</text>\n" + 
        "<rect x='60.0' y='18.0' width='75.0' height='30.0' class='codetoken'/>\n" + 
        "<text x='65.0' y='37.0' class='codetoken'>.a:</text>\n" + 
        "<rect x='100.0' y='21.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<path d=\"M100.0 45.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='135.0' y='18.0' width='264.0' height='30.0' class='codetoken'/>\n" + 
        "<text x='140.0' y='37.0' class='codetoken'>.a:</text>\n" + 
        "<rect x='175.0' y='21.0' width='87.0' height='24.0' class='codetoken'/>\n" + 
        "<path d=\"M175.0 45.0 l87.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='180.0' y='37.0' class='codetoken'>.d:</text>\n" + 
        "<rect x='215.0' y='24.0' width='30.0' height='18' class='codetoken'/><text x='220.0' y='37.0' class='codetoken'>12</text><path d=\"M215.0 42.0 l30.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='267.0' y='37.0' class='codetoken'>b:</text>\n" + 
        "<rect x='292.0' y='21.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<path d=\"M292.0 45.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<text x='327.0' y='37.0' class='codetoken'>c:</text>\n" + 
        "<rect x='352.0' y='21.0' width='30.0' height='24' class='codetoken'/><text x='357.0' y='37.0' class='codetoken'>32</text><path d=\"M352.0 45.0 l30.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='399.0' y='18.0' width='20.0' height='30' class='codetoken'/><text x='404.0' y='37.0' class='codetoken'>+</text>\n" + 
        "<rect x='419.0' y='18.0' width='130.0' height='30' class='codetoken'/><text x='424.0' y='37.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='48.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='61.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    SvgCodeRenderer.TokenRendererPositioning positioning = renderPlainForPositioning(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals(66, positioning.lineTop, 0.001);
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
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='1000.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken tokencomment'>// Comment</text>\n" + 
        "<path d='M0.0 18.0 l 100.0 0 l 0 24.0 l -80.0 0 L 20.0 78.0 L 0.0 78.0 z' class='codetoken'/><text x='5.0' y='34.0' class='codetoken'>if</text><text x='85.0' y='34.0' class='codetoken'>{</text><rect x='30.0' y='21.0' width='50.0' height='18' class='codetoken'/><text x='35.0' y='34.0' class='codetoken'>true</text><path d=\"M30.0 39.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<text x='5.0' y='73.0' class='codetoken'>}</text>\n" + 
        "<rect x='0.0' y='78.0' width='100.0' height='12' class='codetoken'/><text x='5.0' y='88.0' class='codetoken'>22 adf df</text>\n" + 
        "<rect x='100.0' y='78.0' width='20.0' height='12' class='codetoken'/><text x='105.0' y='88.0' class='codetoken'>+</text>\n" + 
        "<rect x='120.0' y='78.0' width='130.0' height='12' class='codetoken'/><text x='125.0' y='88.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='0.0' y='96.0' width='30.0' height='18' class='codetoken'/><text x='5.0' y='109.0' class='codetoken'>55</text>\n" + 
        "", returned.svgString);
    SvgCodeRenderer.TokenRendererPositioning positioning = renderPlainForPositioning(codeList, DEFAULT_CANVAS_WIDTH);
    Assert.assertEquals(114, positioning.lineTop, 0.001);
  }
  
  @Test
  public void testHitBoxRenderedCursorPosition()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            new Token.SimpleToken("32", Symbol.Number)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents(".b", Symbol.DotVariable),
            new Token.SimpleToken(":=", Symbol.Assignment),
            Token.ParameterToken.fromContents(".a:b:", Symbol.DotVariable,
                new TokenContainer(new Token.SimpleToken("3", Symbol.Number)),
                new TokenContainer())
            ),
        new TokenContainer(),
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
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    Token.ParameterToken.fromContents(".and:", Symbol.DotVariable, 
                        new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)))), 
                new StatementContainer(
                    new TokenContainer(new Token.SimpleToken("64", Symbol.Number)),
                    new TokenContainer(new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF)))),
            new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE,
                new StatementContainer(
                    new TokenContainer(
                        new Token.OneExpressionOneBlockToken("while", Symbol.COMPOUND_WHILE, 
                            new TokenContainer(new Token.SimpleToken("true", Symbol.TrueLiteral)), 
                            new StatementContainer(
                                new TokenContainer(
                                    new Token.SimpleToken("3", Symbol.Number)
                                    ))))))
            )
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, DEFAULT_CANVAS_WIDTH);
    RenderedHitBox hitBox = returned.hitBox;
    
    // Check if we can find cursor positions
    CursorRect cursorRect = null;
    
    // Start of a line of simple tokens
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(0, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 0, 18}, cursorRect.getTestDimensions(), 0.001);

    // Middle of a line of simple tokens
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(0, 2), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {60, 0, 18}, cursorRect.getTestDimensions(), 0.001);

    // End of a line
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(0, 3), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {90, 0, 18}, cursorRect.getTestDimensions(), 0.001);
    
    // Middle of 2nd line before a parameter token
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(1, 2), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {60, 18, 42}, cursorRect.getTestDimensions(), 0.001);
    
    // End of a 2nd line of non-wide tokens (after parameter token)
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(1, 3), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {185, 18, 42}, cursorRect.getTestDimensions(), 0.001);

    // Inside a parameter token
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(1, 2, SvgCodeRenderer.PARAMTOK_POS_EXPRS, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {100, 21, 39}, cursorRect.getTestDimensions(), 0.001);

    // Empty parameter token expression
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(1, 2, SvgCodeRenderer.PARAMTOK_POS_EXPRS, 1), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {150, 21, 39}, cursorRect.getTestDimensions(), 0.001);

    // Empty line
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(2, 0, SvgCodeRenderer.EXPRBLOCK_POS_EXPR), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 42, 60}, cursorRect.getTestDimensions(), 0.001);
    
    // Start of a line with a simple wide token
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(3, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 60, 78}, cursorRect.getTestDimensions(), 0.001);

    // After a simple wide token
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(3, 1), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 78, 108}, cursorRect.getTestDimensions(), 0.001);
  
    // Before an "if" block
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(4, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 108, 126}, cursorRect.getTestDimensions(), 0.001);

    // Inside the expression of an if block
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(4, 0, SvgCodeRenderer.EXPRBLOCK_POS_EXPR, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {30, 111, 135}, cursorRect.getTestDimensions(), 0.001);

    // Inside the block of a block nested inside another block
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(4, 1, SvgCodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 0, SvgCodeRenderer.EXPRBLOCK_POS_BLOCK, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {40, 288, 306}, cursorRect.getTestDimensions(), 0.001);

    // After a wide token with nothing there
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(4, 2), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {0, 360, 378}, cursorRect.getTestDimensions(), 0.001);
    
    // Inside an empty block
    cursorRect = RenderedCursorPosition.inStatements(codeList, 
        CodePosition.fromOffsets(4, 0, SvgCodeRenderer.EXPRBLOCK_POS_BLOCK, 1, 0, SvgCodeRenderer.EXPRBLOCK_POS_BLOCK, 0, 0), 
        0, hitBox);
    Assert.assertArrayEquals(new double[] {40, 174, 192}, cursorRect.getTestDimensions(), 0.001);
  }
  
  private SvgCodeRenderer.TokenRendererPositioning renderPlainForPositioning(StatementContainer codeList, double canvasWidth)
  {
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(canvasWidth, widthCalculator);
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    renderPlain(codeList, positioning, returned);
    return positioning;
  }
  
  private SvgCodeRenderer.TokenRendererReturn renderPlain(StatementContainer codeList, double canvasWidth)
  {
    SvgCodeRenderer.TextWidthCalculator widthCalculator = new SimpleWidthCalculator(); 
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(canvasWidth, widthCalculator);
    positioning.fontSize = 10;
    positioning.wrapLineStart = 25;
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    renderPlain(codeList, positioning, returned);
    return returned;
  }

  private void renderPlain(StatementContainer codeList, SvgCodeRenderer.TokenRendererPositioning positioning, SvgCodeRenderer.TokenRendererReturn returned)
  {
    SvgCodeRenderer.RenderSupplementalInfo supplementalInfo = new SvgCodeRenderer.RenderSupplementalInfo();
    supplementalInfo.codeErrors = new ErrorList();
    supplementalInfo.nesting = new CodeNestingCounter();
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
    CodePosition currentTokenPos = new CodePosition();
    SvgCodeRenderer.renderStatementContainer(codeList, returned, positioning, new CodePosition(), 0, currentTokenPos, tokenRenderer, null, supplementalInfo);
  }
  
  @Test
  public void testWrappingSimpleTokens()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String),
            new Token.SimpleToken("+", Symbol.Plus),
            new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
            ),
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                new TokenContainer(
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    new Token.SimpleToken("AND", Symbol.And),
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    new Token.SimpleToken("AND", Symbol.And),
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    new Token.SimpleToken("AND", Symbol.And),
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    new Token.SimpleToken("AND", Symbol.And),
                    new Token.SimpleToken("true", Symbol.TrueLiteral),
                    new Token.SimpleToken("AND", Symbol.And),
                    new Token.SimpleToken("true", Symbol.TrueLiteral)
                    ), 
                new StatementContainer(
                    new TokenContainer(
                        new Token.SimpleToken("1", Symbol.Number),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String),
                        new Token.SimpleToken("+", Symbol.Plus),
                        new Token.SimpleToken("\"sdfasdfasf\"", Symbol.String)
                        )
                    )))
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, THIN_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>1</text>\n" + 
        "<rect x='20.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='25.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='40.0' y='0.0' width='130.0' height='18' class='codetoken'/><text x='45.0' y='13.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='170.0' y='0.0' width='20.0' height='18' class='codetoken'/><text x='175.0' y='13.0' class='codetoken'>+</text>\n" + 
        "<rect x='25.0' y='18.0' width='130.0' height='18' class='codetoken'/><text x='30.0' y='31.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='155.0' y='18.0' width='20.0' height='18' class='codetoken'/><text x='160.0' y='31.0' class='codetoken'>+</text>\n" + 
        "<rect x='25.0' y='36.0' width='130.0' height='18' class='codetoken'/><text x='30.0' y='49.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<path d='M0.0 54.0 l 300.0 0 l 0 60.0 l -280.0 0 L 20.0 186.0 L 0.0 186.0 z' class='codetoken'/><text x='5.0' y='70.0' class='codetoken'>if</text><text x='25.0' y='106.0' class='codetoken'>{</text><rect x='30.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='35.0' y='70.0' class='codetoken'>true</text><path d=\"M30.0 75.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='80.0' y='57.0' width='40.0' height='18' class='codetoken'/><text x='85.0' y='70.0' class='codetoken'>AND</text><path d=\"M80.0 75.0 l40.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='120.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='125.0' y='70.0' class='codetoken'>true</text><path d=\"M120.0 75.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='170.0' y='57.0' width='40.0' height='18' class='codetoken'/><text x='175.0' y='70.0' class='codetoken'>AND</text><path d=\"M170.0 75.0 l40.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='210.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='215.0' y='70.0' class='codetoken'>true</text><path d=\"M210.0 75.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='25.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='30.0' y='88.0' class='codetoken'>AND</text><path d=\"M25.0 93.0 l40.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='65.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='70.0' y='88.0' class='codetoken'>true</text><path d=\"M65.0 93.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='115.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='120.0' y='88.0' class='codetoken'>AND</text><path d=\"M115.0 93.0 l40.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='155.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='160.0' y='88.0' class='codetoken'>true</text><path d=\"M155.0 93.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='205.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='210.0' y='88.0' class='codetoken'>AND</text><path d=\"M205.0 93.0 l40.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='245.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='250.0' y='88.0' class='codetoken'>true</text><path d=\"M245.0 93.0 l50.0 0\" class=\"wideexpressionslot\"/>\n" + 
        "<rect x='20.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='25.0' y='127.0' class='codetoken'>1</text>\n" + 
        "<rect x='40.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='45.0' y='127.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='114.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='127.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='190.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='195.0' y='127.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='132.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='145.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='190.0' y='132.0' width='20.0' height='18' class='codetoken'/><text x='195.0' y='145.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='150.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='163.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<text x='5.0' y='181.0' class='codetoken'>}</text>\n" + 
        "", returned.svgString);
  }
  
  @Test
  public void testSimpleWrappingParameterToken()
  {
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".a", Symbol.DotVariable)
            ),
        new TokenContainer(
            Token.ParameterToken.fromContents("a:b:c:d:e:", Symbol.DotVariable, 
                new TokenContainer(
                    new Token.SimpleToken("2", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("3", Symbol.Number)
                    ),
                new TokenContainer(
                    new Token.SimpleToken("5", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("6", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("\"longer string\"", Symbol.String)
                    ),
                new TokenContainer(),
                new TokenContainer(
                    new Token.SimpleToken("8", Symbol.Number)
                    ),
                new TokenContainer()
                )
            ),
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number)
            )
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, THIN_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='40.0' height='18' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>var</text>\n" + 
        "<rect x='40.0' y='0.0' width='30.0' height='18.0' class='codetoken'/><text x='45.0' y='13.0' class='codetoken'>.a</text>\n" + 
        "<rect x='0.0' y='18.0' width='300.0' height='66.0' class='codetoken'/>\n" + 
        "<rect x='0.0' y='18.0' width='2' height='66.0' class=\"plomMultilineAccent\"/>\n" + 
        "<text x='5.0' y='34.0' class='codetoken'>a:</text>\n" + 
        "<rect x='30.0' y='21.0' width='20.0' height='18' class='codetoken'/><text x='35.0' y='34.0' class='codetoken'>2</text><path d=\"M30.0 39.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='50.0' y='21.0' width='20.0' height='18' class='codetoken'/><text x='55.0' y='34.0' class='codetoken'>+</text><path d=\"M50.0 39.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='70.0' y='21.0' width='20.0' height='18' class='codetoken'/><text x='75.0' y='34.0' class='codetoken'>3</text><path d=\"M70.0 39.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='5.0' y='55.0' class='codetoken'>b:</text>\n" + 
        "<rect x='30.0' y='42.0' width='20.0' height='18' class='codetoken'/><text x='35.0' y='55.0' class='codetoken'>5</text><path d=\"M30.0 60.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='50.0' y='42.0' width='20.0' height='18' class='codetoken'/><text x='55.0' y='55.0' class='codetoken'>+</text><path d=\"M50.0 60.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='70.0' y='42.0' width='20.0' height='18' class='codetoken'/><text x='75.0' y='55.0' class='codetoken'>6</text><path d=\"M70.0 60.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='90.0' y='42.0' width='20.0' height='18' class='codetoken'/><text x='95.0' y='55.0' class='codetoken'>+</text><path d=\"M90.0 60.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='110.0' y='42.0' width='160.0' height='18' class='codetoken'/><text x='115.0' y='55.0' class='codetoken'>\"longer string\"</text><path d=\"M110.0 60.0 l160.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='5.0' y='76.0' class='codetoken'>c:</text>\n" + 
        "<rect x='30.0' y='63.0' width='30' height='18' class='fillinblank'/>\n" + 
        "<path d=\"M30.0 81.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<text x='65.0' y='76.0' class='codetoken'>d:</text>\n" + 
        "<rect x='90.0' y='63.0' width='20.0' height='18' class='codetoken'/><text x='95.0' y='76.0' class='codetoken'>8</text><path d=\"M90.0 81.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='115.0' y='76.0' class='codetoken'>e:</text>\n" + 
        "<rect x='140.0' y='63.0' width='30' height='18' class='fillinblank'/>\n" + 
        "<path d=\"M140.0 81.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='0.0' y='84.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='97.0' class='codetoken'>1</text>\n" + 
        "", returned.svgString);
  }

  @Test
  public void testWrappingParameterToken()
  {
    // multiple parameters on a line, a later one wraps (but name and expression fit on a line)
    // name and expression of a parameter don't fit on a single line
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents("a:b:nameExprOneLine:c:exprSeparateFromName:e:", Symbol.DotVariable, 
                new TokenContainer(
                    new Token.SimpleToken("3", Symbol.Number)
                    ),
                new TokenContainer(),
                new TokenContainer(
                    new Token.SimpleToken("8", Symbol.Number)
                    ),
                new TokenContainer(),
                new TokenContainer(
                    new Token.SimpleToken("\"longer string\"", Symbol.String),
                    new Token.SimpleToken("5", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("6", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("\"longer string\"", Symbol.String)
                    ),
                new TokenContainer()
                )
            ),
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number)
            )
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, THIN_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='300.0' height='123.0' class='codetoken'/>\n" + 
        "<rect x='0.0' y='0.0' width='2' height='123.0' class=\"plomMultilineAccent\"/>\n" + 
        "<text x='5.0' y='16.0' class='codetoken'>a:</text>\n" + 
        "<rect x='30.0' y='3.0' width='20.0' height='18' class='codetoken'/><text x='35.0' y='16.0' class='codetoken'>3</text><path d=\"M30.0 21.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='55.0' y='16.0' class='codetoken'>b:</text>\n" + 
        "<rect x='80.0' y='3.0' width='30' height='18' class='fillinblank'/>\n" + 
        "<path d=\"M80.0 21.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<text x='5.0' y='37.0' class='codetoken'>nameExprOneLine:</text>\n" + 
        "<rect x='170.0' y='24.0' width='20.0' height='18' class='codetoken'/><text x='175.0' y='37.0' class='codetoken'>8</text><path d=\"M170.0 42.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='195.0' y='37.0' class='codetoken'>c:</text>\n" + 
        "<rect x='220.0' y='24.0' width='30' height='18' class='fillinblank'/>\n" + 
        "<path d=\"M220.0 42.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<text x='5.0' y='58.0' class='codetoken'>exprSeparateFromName:</text>\n" + 
        "<rect x='45.0' y='63.0' width='160.0' height='18' class='codetoken'/><text x='50.0' y='76.0' class='codetoken'>\"longer string\"</text><path d=\"M45.0 81.0 l160.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='205.0' y='63.0' width='20.0' height='18' class='codetoken'/><text x='210.0' y='76.0' class='codetoken'>5</text><path d=\"M205.0 81.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='225.0' y='63.0' width='20.0' height='18' class='codetoken'/><text x='230.0' y='76.0' class='codetoken'>+</text><path d=\"M225.0 81.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='245.0' y='63.0' width='20.0' height='18' class='codetoken'/><text x='250.0' y='76.0' class='codetoken'>6</text><path d=\"M245.0 81.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='265.0' y='63.0' width='20.0' height='18' class='codetoken'/><text x='270.0' y='76.0' class='codetoken'>+</text><path d=\"M265.0 81.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='45.0' y='81.0' width='160.0' height='18' class='codetoken'/><text x='50.0' y='94.0' class='codetoken'>\"longer string\"</text><path d=\"M45.0 99.0 l160.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='5.0' y='115.0' class='codetoken'>e:</text>\n" + 
        "<rect x='30.0' y='102.0' width='30' height='18' class='fillinblank'/>\n" + 
        "<path d=\"M30.0 120.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='0.0' y='123.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='136.0' class='codetoken'>1</text>\n" + 
        "", returned.svgString);
  }

  @Test
  public void testWrappingParameterTokenChained()
  {
    // wrapping parameter token with another token after it (should start a new line)
    // nested parameter token
    // parameter name is longest thing and should extend size of parameter token rectangle
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".this parameter name is longer than expression so it should determine width of token:b:", Symbol.DotVariable, 
                new TokenContainer(
                    new Token.SimpleToken("1", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("2", Symbol.Number)
                    ),
                new TokenContainer()
                ),
            new Token.SimpleToken("+", Symbol.Plus),
            Token.ParameterToken.fromContents(".a:b:c:", Symbol.DotVariable, 
                new TokenContainer(
                    new Token.SimpleToken("1", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    new Token.SimpleToken("2", Symbol.Number)
                    ),
                new TokenContainer(),
                new TokenContainer(
                    new Token.SimpleToken("1", Symbol.Number),
                    new Token.SimpleToken("+", Symbol.Plus),
                    Token.ParameterToken.fromContents(".nested token:that wraps:", Symbol.DotVariable, 
                        new TokenContainer(
                            new Token.SimpleToken("5", Symbol.Number)
                            ),
                        new TokenContainer(
                            new Token.SimpleToken("7", Symbol.Number)
                            )
                        ),
                    new Token.SimpleToken("2", Symbol.Number)
                    )
                ),
            new Token.SimpleToken("1", Symbol.Number)
            )            
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, THIN_CANVAS_WIDTH);
    Assert.assertEquals("<rect x='0.0' y='0.0' width='865.0' height='54.0' class='codetoken'/>\n" + 
        "<rect x='0.0' y='0.0' width='2' height='54.0' class=\"plomMultilineAccent\"/>\n" + 
        "<text x='5.0' y='19.0' class='codetoken'>.this parameter name is longer than expression so it should determine width of token:</text>\n" + 
        "<rect x='45.0' y='27.0' width='20.0' height='24' class='codetoken'/><text x='50.0' y='43.0' class='codetoken'>1</text><path d=\"M45.0 51.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='65.0' y='27.0' width='20.0' height='24' class='codetoken'/><text x='70.0' y='43.0' class='codetoken'>+</text><path d=\"M65.0 51.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='85.0' y='27.0' width='20.0' height='24' class='codetoken'/><text x='90.0' y='43.0' class='codetoken'>2</text><path d=\"M85.0 51.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='110.0' y='43.0' class='codetoken'>b:</text>\n" + 
        "<rect x='135.0' y='27.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<path d=\"M135.0 51.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='25.0' y='54.0' width='20.0' height='30' class='codetoken'/><text x='30.0' y='73.0' class='codetoken'>+</text>\n" + 
        "<rect x='25.0' y='84.0' width='275.0' height='150.0' class='codetoken'/>\n" + 
        "<rect x='25.0' y='84.0' width='2' height='150.0' class=\"plomMultilineAccent\"/>\n" + 
        "<text x='30.0' y='103.0' class='codetoken'>.a:</text>\n" + 
        "<rect x='65.0' y='87.0' width='20.0' height='24' class='codetoken'/><text x='70.0' y='103.0' class='codetoken'>1</text><path d=\"M65.0 111.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='85.0' y='87.0' width='20.0' height='24' class='codetoken'/><text x='90.0' y='103.0' class='codetoken'>+</text><path d=\"M85.0 111.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='105.0' y='87.0' width='20.0' height='24' class='codetoken'/><text x='110.0' y='103.0' class='codetoken'>2</text><path d=\"M105.0 111.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='130.0' y='103.0' class='codetoken'>b:</text>\n" + 
        "<rect x='155.0' y='87.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<path d=\"M155.0 111.0 l30 0\" class=\"tokenslot\"/>\n" + 
        "<text x='30.0' y='130.0' class='codetoken'>c:</text>\n" + 
        "<rect x='70.0' y='138.0' width='20.0' height='24' class='codetoken'/><text x='75.0' y='154.0' class='codetoken'>1</text><path d=\"M70.0 162.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='90.0' y='138.0' width='20.0' height='24' class='codetoken'/><text x='95.0' y='154.0' class='codetoken'>+</text><path d=\"M90.0 162.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='70.0' y='162.0' width='225.0' height='45.0' class='codetoken'/>\n" + 
        "<path d=\"M70.0 207.0 l225.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='70.0' y='162.0' width='2' height='45.0' class=\"plomMultilineAccent\"/>\n" + 
        "<text x='75.0' y='178.0' class='codetoken'>.nested token:</text>\n" + 
        "<rect x='220.0' y='165.0' width='20.0' height='18' class='codetoken'/><text x='225.0' y='178.0' class='codetoken'>5</text><path d=\"M220.0 183.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<text x='75.0' y='199.0' class='codetoken'>that wraps:</text>\n" + 
        "<rect x='190.0' y='186.0' width='20.0' height='18' class='codetoken'/><text x='195.0' y='199.0' class='codetoken'>7</text><path d=\"M190.0 204.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='70.0' y='207.0' width='20.0' height='24' class='codetoken'/><text x='75.0' y='223.0' class='codetoken'>2</text><path d=\"M70.0 231.0 l20.0 0\" class=\"tokenslot\"/>\n" + 
        "<rect x='25.0' y='234.0' width='20.0' height='30' class='codetoken'/><text x='30.0' y='253.0' class='codetoken'>1</text>\n" + 
        "", returned.svgString);
    // To test:
    //   placement of extra space after last parameter expression and how it wraps
  }
  
  @Test
  public void testFunctionLiteral()
  {
    // Tests a function literal (i.e. a wide token that can be mixed in with non-wide tokens in an expression)
    StatementContainer codeList = new StatementContainer(
        new TokenContainer(
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral)
            // Despite being a wide token, there should be no empty line afterwards
            ),            
        new TokenContainer(
            new Token.SimpleToken("1", Symbol.Number),
            // Should wrap and indent to the next line
            new Token.OneExpressionOneBlockToken("lambda", Symbol.FunctionLiteral),
            // Subsequent tokens should also appear on their own line and wrap
            new Token.SimpleToken("1", Symbol.Number)
            )            
        );
    SvgCodeRenderer.TokenRendererReturn returned = renderPlain(codeList, THIN_CANVAS_WIDTH);
    Assert.assertEquals("<path d='M0.0 0.0 l 70.0 0 l 0 18.0 l -50.0 0 L 20.0 54.0 L 0.0 54.0 z' class='codetoken'/><text x='5.0' y='13.0' class='codetoken'>\u03bb</text><text x='55.0' y='13.0' class='codetoken'>{</text><rect x='20.0' y='3.0' width='30' height='12' class='fillinblank'/>\n" + 
        "<path d=\"M20.0 15.0 l30 0\" class=\"wideexpressionslot\"/>\n" + 
        "<text x='5.0' y='49.0' class='codetoken'>}</text>\n" + 
        "<rect x='0.0' y='54.0' width='20.0' height='18' class='codetoken'/><text x='5.0' y='67.0' class='codetoken'>1</text>\n" + 
        "<path d='M40.0 72.0 l 70.0 0 l 0 18.0 l -50.0 0 L 60.0 126.0 L 40.0 126.0 z' class='codetoken'/><text x='45.0' y='85.0' class='codetoken'>\u03bb</text><text x='95.0' y='85.0' class='codetoken'>{</text><rect x='60.0' y='75.0' width='30' height='12' class='fillinblank'/>\n" + 
        "<path d=\"M60.0 87.0 l30 0\" class=\"wideexpressionslot\"/>\n" + 
        "<text x='45.0' y='121.0' class='codetoken'>}</text>\n" + 
        "<rect x='40.0' y='126.0' width='20.0' height='12' class='codetoken'/><text x='45.0' y='136.0' class='codetoken'>1</text>\n" + 
        "", returned.svgString);
    // Hitbox for the cursor position at the end of the function literal 
    // (unlike normal wide tokens where the hitbox refers to the blank line 
    // after the token, the hitbox for the function literal should refer to the space after the '}' )
    RenderedHitBox endHitBox = returned.hitBox.children.get(0).children.get(1);
    Assert.assertEquals(36, endHitBox.getOffsetTop());
    Assert.assertEquals(20, endHitBox.getOffsetLeft());
    Assert.assertEquals(0, endHitBox.getOffsetWidth());
    Assert.assertEquals(18, endHitBox.getOffsetHeight());
  }
}
