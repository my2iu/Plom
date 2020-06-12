package org.programmingbasics.plom.core.interpreter;

import org.junit.Test;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class MachineContextTest extends TestCase
{
  @Test
  public void testRunInstructionsRecursively() throws ParseException, RunException
  {
    TokenContainer line = new TokenContainer(
        new Token.SimpleToken("1", Symbol.Number),
        new Token.SimpleToken("+", Symbol.Plus),
        new Token.SimpleToken("2", Symbol.Number),
        new Token.SimpleToken("-", Symbol.Minus),
        new Token.SimpleToken("2.5", Symbol.Number));
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement);
    AstNode parsed = parser.parseToEnd(Symbol.Expression);
    MachineContext machine = new MachineContext();
    machine.setStart(parsed, new MachineContext.NodeHandlers());
    machine.runToCompletion();
  }

}
