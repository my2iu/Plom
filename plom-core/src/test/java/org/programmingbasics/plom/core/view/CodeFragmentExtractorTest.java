package org.programmingbasics.plom.core.view;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class CodeFragmentExtractorTest extends TestCase
{
  @Test
  public void testExtractFragmentFromLine()
  {
    StatementContainer container = 
        new StatementContainer(
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.Var),
                new Token.SimpleToken(":=", Symbol.Assignment),
                new Token.SimpleToken("1", Symbol.Number),
                new Token.SimpleToken("+", Symbol.Plus),
                new Token.SimpleToken("1", Symbol.Number)
                )
            );
    Assert.assertEquals(" := 1 +", CodeFragmentExtractor.extractFromStatements(container, CodePosition.fromOffsets(0, 1), CodePosition.fromOffsets(0, 4)));
  }
}
