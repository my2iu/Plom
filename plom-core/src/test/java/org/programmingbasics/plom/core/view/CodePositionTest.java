package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.CodePosition;

import junit.framework.TestCase;

public class CodePositionTest extends TestCase
{
  @Test
  public void testIsBefore()
  {
    Assert.assertFalse(CodePosition.fromOffsets(1).isBefore(CodePosition.fromOffsets(0)));
    Assert.assertTrue(CodePosition.fromOffsets(0).isBefore(CodePosition.fromOffsets(1)));
    Assert.assertFalse(CodePosition.fromOffsets(0,1,2).isBefore(CodePosition.fromOffsets(0,1)));
    Assert.assertTrue(CodePosition.fromOffsets(0,1).isBefore(CodePosition.fromOffsets(0,1,0)));
    Assert.assertFalse(CodePosition.fromOffsets(0,1).isBefore(CodePosition.fromOffsets(0,1)));
    Assert.assertTrue(CodePosition.fromOffsets(0,1,5).isBefore(CodePosition.fromOffsets(0,1,10)));
    Assert.assertFalse(CodePosition.fromOffsets(0,1,15).isBefore(CodePosition.fromOffsets(0,1,10)));
  }
  
  @Test
  public void testIsBetweenNullable()
  {
    Assert.assertTrue(CodePosition.fromOffsets(0, 3).isBetweenNullable(
        CodePosition.fromOffsets(0, 3), CodePosition.fromOffsets(0, 5)));
    Assert.assertFalse(CodePosition.fromOffsets(0, 3).isBetweenNullable(
        CodePosition.fromOffsets(0, 3), CodePosition.fromOffsets(0, 3)));
    Assert.assertFalse(CodePosition.fromOffsets(0, 2).isBetweenNullable(
        CodePosition.fromOffsets(0, 3), CodePosition.fromOffsets(0, 5)));
    Assert.assertFalse(CodePosition.fromOffsets(0, 7).isBetweenNullable(
        CodePosition.fromOffsets(0, 3), CodePosition.fromOffsets(0, 5)));
    Assert.assertFalse(CodePosition.fromOffsets(0, 3).isBetweenNullable(
        null, CodePosition.fromOffsets(0, 5)));
    Assert.assertFalse(CodePosition.fromOffsets(0, 3).isBetweenNullable(
        CodePosition.fromOffsets(0, 2), null));
    Assert.assertTrue(CodePosition.fromOffsets(0, 5).isBetweenNullable(
        CodePosition.fromOffsets(0, 3), CodePosition.fromOffsets(0, 6)));
    Assert.assertTrue(CodePosition.fromOffsets(0, 3).isBetweenNullable(
        CodePosition.fromOffsets(0, 5), CodePosition.fromOffsets(0, 3)));
  }
}
