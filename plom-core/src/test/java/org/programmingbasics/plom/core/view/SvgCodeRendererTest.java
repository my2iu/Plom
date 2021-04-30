package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
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
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH);
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
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH);
    positioning.fontSize = 10;
    supplement.nesting.calculateNestingForLine(line);
    positioning.maxNestingForLine = supplement.nesting.expressionNesting.get(line);
    positioning.currentNestingInLine = 0;
    SvgCodeRenderer.renderLine(line, returned, positioning, new CodePosition(), 0, currentTokenPos, null, false, tokenRenderer, null, supplement, 0);
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
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(DEFAULT_CANVAS_WIDTH);
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRenderer tokenRenderer = new SvgCodeRenderer.TokenRenderer(null, supplementalInfo, (int)Math.ceil(positioning.fontSize), new SimpleWidthCalculator());
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
        "<rect x='0.0' y='18.0' width='60.0' height='30' class='codetoken'/><text x='5.0' y='37.0' class='codetoken'>@Type</text>\n" + 
        "<rect x='60.0' y='18.0' width='87.0' height='30' class='codetoken'/><text x='65.0' y='37.0' class='codetoken'>.a:</text>\n" + 
        "<rect x='100.0' y='21.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<rect x='147.0' y='18.0' width='264.0' height='30' class='codetoken'/><text x='152.0' y='37.0' class='codetoken'>.a:</text><text x='279.0' y='37.0' class='codetoken'>b:</text><text x='339.0' y='37.0' class='codetoken'>c:</text>\n" + 
        "<rect x='187.0' y='21.0' width='87.0' height='24' class='codetoken'/><text x='192.0' y='37.0' class='codetoken'>.d:</text>\n" + 
        "<rect x='227.0' y='24.0' width='30.0' height='18' class='codetoken'/><text x='232.0' y='37.0' class='codetoken'>12</text>\n" + 
        "<rect x='304.0' y='21.0' width='30' height='24' class='fillinblank'/>\n" + 
        "<rect x='364.0' y='21.0' width='30.0' height='24' class='codetoken'/><text x='369.0' y='37.0' class='codetoken'>32</text>\n" + 
        "<rect x='411.0' y='18.0' width='20.0' height='30' class='codetoken'/><text x='416.0' y='37.0' class='codetoken'>+</text>\n" + 
        "<rect x='431.0' y='18.0' width='130.0' height='30' class='codetoken'/><text x='436.0' y='37.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
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
        "<path d='M0.0 18.0 l 100.0 0 l 0 24.0 l -80.0 0 L 20.0 78.0 L 0.0 78.0 z' class='codetoken'/><text x='5.0' y='34.0'>if</text><text x='85.0' y='34.0'>{</text><rect x='30.0' y='21.0' width='50.0' height='18' class='codetoken'/><text x='35.0' y='34.0' class='codetoken'>true</text>\n" + 
        "<text x='5.0' y='73.0'>}</text>\n" + 
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
    Assert.assertArrayEquals(new double[] {197, 18, 42}, cursorRect.getTestDimensions(), 0.001);

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
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(canvasWidth);
    positioning.fontSize = 10;
    SvgCodeRenderer.TokenRendererReturn returned = new SvgCodeRenderer.TokenRendererReturn();
    renderPlain(codeList, positioning, returned);
    return positioning;
  }
  
  private SvgCodeRenderer.TokenRendererReturn renderPlain(StatementContainer codeList, double canvasWidth)
  {
    SvgCodeRenderer.TokenRendererPositioning positioning = new SvgCodeRenderer.TokenRendererPositioning(canvasWidth);
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
        "<path d='M0.0 54.0 l 300.0 0 l 0 60.0 l -280.0 0 L 20.0 186.0 L 0.0 186.0 z' class='codetoken'/><text x='5.0' y='70.0'>if</text><text x='25.0' y='106.0'>{</text><rect x='30.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='35.0' y='70.0' class='codetoken'>true</text>\n" + 
        "<rect x='80.0' y='57.0' width='40.0' height='18' class='codetoken'/><text x='85.0' y='70.0' class='codetoken'>AND</text>\n" + 
        "<rect x='120.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='125.0' y='70.0' class='codetoken'>true</text>\n" + 
        "<rect x='170.0' y='57.0' width='40.0' height='18' class='codetoken'/><text x='175.0' y='70.0' class='codetoken'>AND</text>\n" + 
        "<rect x='210.0' y='57.0' width='50.0' height='18' class='codetoken'/><text x='215.0' y='70.0' class='codetoken'>true</text>\n" + 
        "<rect x='25.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='30.0' y='88.0' class='codetoken'>AND</text>\n" + 
        "<rect x='65.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='70.0' y='88.0' class='codetoken'>true</text>\n" + 
        "<rect x='115.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='120.0' y='88.0' class='codetoken'>AND</text>\n" + 
        "<rect x='155.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='160.0' y='88.0' class='codetoken'>true</text>\n" + 
        "<rect x='205.0' y='75.0' width='40.0' height='18' class='codetoken'/><text x='210.0' y='88.0' class='codetoken'>AND</text>\n" + 
        "<rect x='245.0' y='75.0' width='50.0' height='18' class='codetoken'/><text x='250.0' y='88.0' class='codetoken'>true</text>\n" + 
        "<rect x='20.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='25.0' y='127.0' class='codetoken'>1</text>\n" + 
        "<rect x='40.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='45.0' y='127.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='114.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='127.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='190.0' y='114.0' width='20.0' height='18' class='codetoken'/><text x='195.0' y='127.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='132.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='145.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<rect x='190.0' y='132.0' width='20.0' height='18' class='codetoken'/><text x='195.0' y='145.0' class='codetoken'>+</text>\n" + 
        "<rect x='60.0' y='150.0' width='130.0' height='18' class='codetoken'/><text x='65.0' y='163.0' class='codetoken'>\"sdfasdfasf\"</text>\n" + 
        "<text x='5.0' y='181.0'>}</text>\n" + 
        "", returned.svgString);
  }

}
