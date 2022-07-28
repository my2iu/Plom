package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import junit.framework.TestCase;

public class VariableDeclarationInterpreterTest extends TestCase
{
  @Test
  public void testSimpleDeclarations()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test1", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@object", Symbol.AtType)),
        new TokenContainer(
            new Token.WideToken("This is a comment", Symbol.DUMMY_COMMENT),
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test2", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test5", Symbol.DotVariable),
            Token.ParameterToken.fromContents("f@call:with:\u2192", Symbol.FunctionType,
                new TokenContainer(Token.ParameterToken.fromContents("@number", Symbol.AtType)), 
                new TokenContainer(), new TokenContainer())));
    
    ErrorList errors = new ErrorList();
    List<String> declaredNames = new ArrayList<>();
    List<UnboundType> declaredTypes = new ArrayList<>();
    VariableDeclarationInterpreter.fromStatements(code, 
        (name, t) -> {
          declaredNames.add(name);
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        errors);
    
    Assert.assertEquals(3, declaredNames.size());
    Assert.assertEquals("test1", declaredNames.get(0));
    Assert.assertEquals("test2", declaredNames.get(1));
    Assert.assertEquals("test5", declaredNames.get(2));
    Assert.assertEquals("object", declaredTypes.get(0).mainToken.getLookupName());
    Assert.assertEquals("number", declaredTypes.get(1).mainToken.getLookupName());
    Assert.assertEquals("call:with:\u2192", declaredTypes.get(2).mainToken.getLookupName());
    Assert.assertEquals("number", ((Token.ParameterToken)declaredTypes.get(2).mainToken.parameters.get(0).tokens.get(0)).getLookupName());
  }
  
  @Test
  public void testSkipErrors()
  {
    StatementContainer code = new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test1", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@object", Symbol.AtType)),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var)),
        new TokenContainer(
            Token.ParameterToken.fromContents(".test3", Symbol.DotVariable)),
        new TokenContainer(
            new Token.WideToken("This is a comment", Symbol.DUMMY_COMMENT),
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test2", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@number", Symbol.AtType)));
    
    ErrorList errors = new ErrorList();
    List<String> declaredNames = new ArrayList<>();
    List<UnboundType> declaredTypes = new ArrayList<>();
    VariableDeclarationInterpreter.fromStatements(code, 
        (name, t) -> {
          declaredNames.add(name);
          declaredTypes.add(t);
        },
//        (unboundType) -> {return new Type(unboundType.mainToken.getLookupName());},
        errors);
    
    Assert.assertEquals(2, declaredNames.size());
    Assert.assertEquals("test1", declaredNames.get(0));
    Assert.assertEquals("test2", declaredNames.get(1));
    Assert.assertEquals("object", declaredTypes.get(0).mainToken.getLookupName());
    Assert.assertEquals("number", declaredTypes.get(1).mainToken.getLookupName());
  }
  
}
