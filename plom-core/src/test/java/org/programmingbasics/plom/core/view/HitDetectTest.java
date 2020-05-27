package org.programmingbasics.plom.core.view;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class HitDetectTest extends TestCase
{
  class MockHitBox extends RenderedHitBox
  {
    int left, top, width, height;
    public MockHitBox(int left, int top, int width, int height, RenderedHitBox...children)
    {
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      if (children.length > 0)
        this.children = Arrays.asList(children);
    }
    @Override public int getOffsetLeft() { if (left < 0) throw new NullPointerException(); else return left; }
    @Override public int getOffsetTop() { if (top < 0) throw new NullPointerException(); else return top; }
    @Override public int getOffsetWidth() { if (width< 0) throw new NullPointerException(); else return width; }
    @Override public int getOffsetHeight() { if (height < 0) throw new NullPointerException(); else return height; }
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
    // If clicking before the code, there is no hit
    CodePosition newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(0, 0, container, hitBoxes, newPos, 0);
    Assert.assertNull(newPos);
    
    // Clicking on the token itself
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(5, 5, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(0, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));
    
    // Clicking on same line of token, but after it
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(30, 5, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(0, newPos.getOffset(0));
    Assert.assertEquals(1, newPos.getOffset(1));

    // Clicking under token (will count as clicking on the same line as the token)
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(5, 30, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(0, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));
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
                null, null)
        ));

    // Clicking before the "if"
    CodePosition newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(-1, 30, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));

    // Clicking on front part of the "if"
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(10, 30, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));
    
    // Clicking on expression part of "if"
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(25, 30, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(3));
    Assert.assertFalse(newPos.hasOffset(4));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));
    Assert.assertEquals(1, newPos.getOffset(2));
    Assert.assertEquals(0, newPos.getOffset(3));
    
    // Clicking inside the block part of "if"
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(40, 60, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(4));
    Assert.assertFalse(newPos.hasOffset(5));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(0, newPos.getOffset(1));
    Assert.assertEquals(2, newPos.getOffset(2));
    Assert.assertEquals(0, newPos.getOffset(3));
    Assert.assertEquals(1, newPos.getOffset(4));

    // Clicking on the statement following the "if"
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(3, 120, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(1, newPos.getOffset(1));
    
    // Clicking after the statement following the "if"
    newPos = new CodePosition();
    newPos = HitDetect.hitDetectStatementContainer(3, 140, container, hitBoxes, newPos, 0);
    Assert.assertTrue(newPos.hasOffset(1));
    Assert.assertFalse(newPos.hasOffset(2));
    Assert.assertEquals(1, newPos.getOffset(0));
    Assert.assertEquals(2, newPos.getOffset(1));

  }
  
  @Test
  public void testParameterToken()
  {
    Assert.assertTrue(false);
  }
}
