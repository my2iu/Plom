package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class MethodArgumentExtractorTest extends TestCase
{
  @Test
  public void testSimpleDeclarations()
  {
    TokenContainer code = new TokenContainer(
        Token.ParameterToken.fromContents(".test1", Symbol.DotVariable),
        Token.ParameterToken.fromContents("@number", Symbol.AtType));
    
    List<String> declaredNames = new ArrayList<>();
    List<UnboundType> declaredTypes = new ArrayList<>();
    MethodArgumentExtractor.fromParameterField(code, 
        (name, t) -> {
          declaredNames.add(name);
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        null);
    
    Assert.assertEquals(1, declaredNames.size());
    Assert.assertEquals("test1", declaredNames.get(0));
    Assert.assertEquals("number", declaredTypes.get(0).mainToken.getLookupName());
  }
  
  @Test
  public void testSkipErrors()
  {
    // Incomplete information
    TokenContainer code = new TokenContainer(
        Token.ParameterToken.fromContents(".test1", Symbol.DotVariable));
    
    List<String> declaredNames = new ArrayList<>();
    List<UnboundType> declaredTypes = new ArrayList<>();
    MethodArgumentExtractor.fromParameterField(code, 
        (name, t) -> {
          declaredNames.add(name);
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        null);

    Assert.assertEquals(0, declaredNames.size());

    // Extra stuff at the end
    code = new TokenContainer(
        Token.ParameterToken.fromContents(".test1", Symbol.DotVariable),
        Token.ParameterToken.fromContents("@number", Symbol.AtType),
        Token.ParameterToken.fromContents(".test1", Symbol.DotVariable));

    MethodArgumentExtractor.fromParameterField(code, 
        (name, t) -> {
          declaredNames.add(name);
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        null);

    Assert.assertEquals(0, declaredNames.size());
  }
  
}
