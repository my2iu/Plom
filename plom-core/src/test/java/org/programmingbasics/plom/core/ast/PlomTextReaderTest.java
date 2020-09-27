package org.programmingbasics.plom.core.ast;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class PlomTextReaderTest extends TestCase
{
  @Test
  public void testWriteToken() throws IOException
  {
    PlomTextWriter io = new PlomTextWriter();
    StringBuilder out = new StringBuilder();
    io.writeToken(out, new Token.SimpleToken("return", Symbol.Return));
    io.writeToken(out, new Token.SimpleToken("var", Symbol.Var));
    io.writeToken(out, new Token.SimpleToken("(", Symbol.OpenParenthesis));
    Assert.assertEquals(" return var (", out.toString());
  }
}
