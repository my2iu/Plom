package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.view.HitDetectTest.MockHitBox;
import org.programmingbasics.plom.core.view.RenderedCursorPosition.CursorRect;

import junit.framework.TestCase;

public class RenderedCursorPositionTest extends TestCase
{
  static void assertCursorRectEquals(CursorRect r1, CursorRect r2)
  {
    Assert.assertEquals(r1.left, r2.left, 0.001);
    Assert.assertEquals(r1.top, r2.top, 0.001);
    Assert.assertEquals(r1.bottom, r2.bottom, 0.001);
  }
  
  
  @Test
  public void testSimpleToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("1", Symbol.Number))
          );
    RenderedHitBox hitBoxes = new MockHitBox(-1, -1, -1, -1,
        new MockHitBox(0, 0, 339, 31,
            new MockHitBox(0, 4, 16, 27))
        );
    // Before token
    CursorRect cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 4, 31), cursor);
    
    // After token
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 1), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(16, 4, 31), cursor);
  }
  
  @Test
  public void testEmpty()
  {
    StatementContainer container = new StatementContainer();
    RenderedHitBox hitBoxes = new MockHitBox(-1, -1, -1, -1);
    // No hit box for the container
    CursorRect cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0), 0, hitBoxes);
    assertNull(cursor);
    // No hit box for the container and going into the first line
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0), 0, hitBoxes);
    assertNull(cursor);
    
    // Redid the rendering to always put in an extra hit box for the blank line, so let's see how it goes
    RenderedHitBox newHitBoxes = new MockHitBox(-1, -1, -1, -1, 
        new MockHitBox(0, 0, 790, 26));
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0), 0, newHitBoxes);
    assertCursorRectEquals(new CursorRect(0, 0, 26), cursor);
    // Go into first line even though there's no tokencontainer there
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0), 0, newHitBoxes);
    assertCursorRectEquals(new CursorRect(0, 0, 26), cursor);
  }
  
  @Test
  public void testBlankLines()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(),
            new TokenContainer(),
            new TokenContainer()
          );
    RenderedHitBox hitBoxes = new MockHitBox(-1, -1, -1, -1, 
        new MockHitBox(0, 0, 790, 18), 
        new MockHitBox(0, 18, 790, 18), 
        new MockHitBox(0, 36, 790, 26));    

    // First line
    CursorRect cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 0, 18), cursor);
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 0, 18), cursor);

    // Second line
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 18, 36), cursor);
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 18, 36), cursor);
  }
  
  @Test
  public void testBlockToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(new Token.SimpleToken("Z", Symbol.Number)),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF, 
                    new TokenContainer(new Token.SimpleToken("A", Symbol.Number)),
                    new StatementContainer(
                        new TokenContainer(new Token.SimpleToken("1", Symbol.Number), new Token.SimpleToken("2", Symbol.Number)))
                ),
                new Token.WideToken("comment", Symbol.DUMMY_COMMENT)
            )
        );
    RenderedHitBox hitBoxes = new MockHitBox(-1, -1, -1, -1,
        new MockHitBox(0, 0, 202, 26,
            new MockHitBox(0, 0, 18, 26)  // Z
        ),
        new MockHitBox(0, 26, 202, 132,
            new MockHitBox(0, 26, 202, 79,  // if
                new MockHitBox(4, 35, 19, 17),
                new MockHitBox(23, 35, 20, 17,
                    new MockHitBox(23, 31, 20, 26)),  // A
                new MockHitBox(4, 57, 194, 26,
                    new MockHitBox(20, 57, 177, 26,
                        new MockHitBox(20, 57, 16, 26),  // 1
                        new MockHitBox(37, 57, 17, 26)   // 2
                        ))
            ),
            new MockHitBox(0, 106, 202, 27,
                new MockHitBox(4, 110, 45, 17),  // Comment
                null, null),
            new MockHitBox(0, 133, 202, 26)   // Empty line added after a wide token
        ));

    // Before a wide token on second line
    CursorRect cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(4, 35, 35 + 17), cursor);

    // Expression of the if
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1, 0, 1, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(23, 31, 31 + 26), cursor);
    
    // In the block of the if
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1, 0, 2, 0, 1), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(37, 57, 57 + 26), cursor);
    
    // After a wide token with nothing afterwards
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(1, 2), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 133, 133 + 26), cursor);
    
    // TODO: test multi-line expressions inside the if
  }
  
  @Test
  public void testOneLineParameterToken()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(Token.ParameterToken.fromContents(".print:at x:y:", Symbol.DotVariable, 
                new TokenContainer(new Token.SimpleToken("\"Hello\"", Symbol.String)),
                new TokenContainer(),
                new TokenContainer(new Token.SimpleToken("20", Symbol.Number))
                ))
        );
    RenderedHitBox hitBoxes = new MockHitBox(-1, -1, -1, -1, 
        new MockHitBox(0, 0, 495, 31, 
            new MockHitBox(0, 4, 177, 26, 
                new MockHitBox(-1, -1, -1, -1,  // PARAMTOK_POS_TEXTS 
                    new MockHitBox(4, 8, 39, 17), 
                    new MockHitBox(100, 8, 32, 17), 
                    new MockHitBox(132, 8, 16, 17), 
                    new MockHitBox(173, 8, 0, 17)), 
                new MockHitBox(-1, -1, -1, -1,  // PARAMTOK_POS_EXPRS 
                    new MockHitBox(43, 8, 57, 17, 
                        new MockHitBox(43, 4, 57, 27)), 
                    new MockHitBox(132, 8, 0, 17), 
                    new MockHitBox(148, 8, 25, 17, 
                        new MockHitBox(148, 4, 25, 27)))))); 
    // Before a parameter token
    CursorRect cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(0, 4, 4 + 26), cursor);

    // start of first parameter
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(43, 4, 4 + 27), cursor);

    // after first parameter
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 0, 1), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(43 + 57, 4, 4 + 27), cursor);
    
    // second parameter that is empty
    cursor = RenderedCursorPosition.inStatements(container, CodePosition.fromOffsets(0, 0, CodeRenderer.PARAMTOK_POS_EXPRS, 1, 0), 0, hitBoxes);
    assertCursorRectEquals(new CursorRect(132, 8, 8 + 17), cursor);

    // TODO: Before/after a multi-line parameter token
  }
  
  // TODO: Write hit detect test for multi-line parameter token
}
