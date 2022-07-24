package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class ReturnTypeExtractorTest extends TestCase
{
  @Test
  public void testSimpleDeclarations()
  {
    TokenContainer code = new TokenContainer(
        Token.ParameterToken.fromContents("@number", Symbol.AtType));
    
    List<UnboundType> declaredTypes = new ArrayList<>();
    ReturnTypeExtractor.fromReturnField(code, 
        (t) -> {
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        null);
    
    Assert.assertEquals(1, declaredTypes.size());
    Assert.assertEquals("number", declaredTypes.get(0).mainToken.getLookupName());
  }
  
}
