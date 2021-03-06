package org.programmingbasics.plom.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;

import junit.framework.TestCase;

public class ModuleCodeRepositoryTest extends TestCase
{
  static ModuleCodeRepository repository = new ModuleCodeRepository();
  static {
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.addGlobalVarAndResetIds("var", Token.ParameterToken.fromContents("@string", Symbol.AtType));
    repository.addFunctionAndResetIds(new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), "get"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));
    ClassDescription testClass = repository.addClassAndResetIds("test class");
    testClass.setSuperclass(null);
    testClass.addMethod(new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), "at x:y:",
            "x", Token.ParameterToken.fromContents("@number", Symbol.AtType),
            "y", Token.ParameterToken.fromContents("@number", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                Token.ParameterToken.fromContents(".x", Symbol.DotVariable)))));
    testClass.addMethod(new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "new")
            .setIsConstructor(true),
        new StatementContainer()));
    testClass.addVarAndResetIds("test var", Token.ParameterToken.fromContents("@test class", Symbol.AtType));
  }
  
  @Test
  public void testSaveClass() throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    ModuleCodeRepository.saveClass(out, repository.getAllClassesSorted().stream().filter(cls -> cls.getName().equals("test class")).findFirst().get());
    Assert.assertEquals(" class @ { test class } {\n" + 
        " var . { test var } @ {test class }\n" +
        " function . {at x: . { x } @ {number }y: . { y } @ {number } } @ {number } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } {\n" + 
        " }\n" + 
        " }\n", 
        strBuilder.toString());
  }

  @Test
  public void testSaveClassWithSupertype() throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    ClassDescription c = repository.getAllClassesSorted().stream().filter(cls -> cls.getName().equals("test class")).findFirst().get();
    c.setSuperclass(Token.ParameterToken.fromContents("@object", Symbol.AtType));
    ModuleCodeRepository.saveClass(out, c);
    Assert.assertEquals(" class @ { test class } extends @ {object } {\n" + 
        " var . { test var } @ {test class }\n" +
        " function . {at x: . { x } @ {number }y: . { y } @ {number } } @ {number } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } {\n" + 
        " }\n" + 
        " }\n", 
        strBuilder.toString());
    c.setSuperclass(null);
  }

  @Test
  public void testLoadClass() throws PlomReadException
  {
    String codeStr = "class @{test class} {\n"
        + "  var .{test var} @{test class}\n"
        + "  function . {at x:.{x} @{number} y: .{ y} @{number} } @{number} {\n" 
        + "    return . {x }\n" 
        + "  }\n" 
        + "  constructor . {new } {\n" 
        + "  }\n" 
        + "}\n";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ClassDescription cls = ModuleCodeRepository.loadClass(lexer);
    Assert.assertEquals("test class", cls.getName());
    Assert.assertNull(cls.parent);
    Assert.assertEquals(1, cls.variables.size());
    Assert.assertEquals("test var", cls.variables.get(0).name);
    Assert.assertEquals("test class", cls.variables.get(0).type.getLookupName());
    Assert.assertEquals(2, cls.methods.size());
    Assert.assertEquals("at x:y:", cls.methods.get(0).sig.getLookupName());
    Assert.assertEquals("y", cls.methods.get(0).sig.argNames.get(1));
    Assert.assertEquals("number", cls.methods.get(0).sig.argTypes.get(1).getLookupName());
    Assert.assertEquals("number", cls.methods.get(0).sig.returnType.getLookupName());
    Assert.assertEquals("new", cls.methods.get(1).sig.getLookupName());
    Assert.assertEquals("void", cls.methods.get(1).sig.returnType.getLookupName());
  }

  @Test
  public void testLoadClassWithSupertype() throws PlomReadException
  {
    String codeStr = "class @{test class} extends @{object} {\n"
        + "  constructor . {new } {\n" 
        + "  }\n" 
        + "}\n";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ClassDescription cls = ModuleCodeRepository.loadClass(lexer);
    Assert.assertEquals("test class", cls.getName());
    Assert.assertEquals("object", cls.parent.getLookupName());
    Assert.assertEquals(1, cls.methods.size());
    Assert.assertEquals("new", cls.methods.get(0).sig.getLookupName());
    Assert.assertEquals("void", cls.methods.get(0).sig.returnType.getLookupName());
  }

  @Test
  public void testSaveModule() throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    repository.saveModule(out, true);
    Assert.assertEquals(" module .{program} {\n" + 
        " var . { var } @ {string }\n" + 
        " function . {get } @ {number } {\n" + 
        " return 3\n" + 
        " }\n" + 
        " class @ { test class } {\n" + 
        " var . { test var } @ {test class }\n" + 
        " function . {at x: . { x } @ {number }y: . { y } @ {number } } @ {number } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } {\n" + 
        " }\n" + 
        " }\n" + 
        " }",
        strBuilder.toString());
  }
  
  @Test
  public void testLoadModule() throws PlomReadException
  {
    String codeStr = " module .{program} {\n" + 
        " var . { variable } @ {string }\n" + 
        " function . {get } @ {number } {\n" + 
        " return 3\n" + 
        " }\n" + 
        " function . {test: . { arg1 } @ {number } } @ {number } {\n" + 
        " }\n" + 
        " class @ { test class 2} {\n" + 
        " }\n" + 
        " class @ { test class } {\n" + 
        " var . { test var } @ {test class }\n" + 
        " function . {at x: . { x } @ {number }y: . { y } @ {number } } @ {number } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } {\n" + 
        " }\n" + 
        " }\n" + 
        " }";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ModuleCodeRepository loaded = new ModuleCodeRepository();
    loaded.addClassAndResetIds("test class 2").setBuiltIn(true);
    loaded.loadModule(lexer);
    
    Assert.assertTrue(loaded.getFunctionWithName("get") != null);
    Assert.assertTrue(loaded.getAllGlobalVarsSorted().stream().anyMatch(v -> v.name.equals("variable")));
    Assert.assertTrue(loaded.getAllGlobalVarsSorted().stream().anyMatch(v -> v.name.equals("variable") && v.type.getLookupName().equals("string")));
    Assert.assertTrue(loaded.hasClassWithName("test class"));
    // If a class already exists, just augment that class, don't create a completely new class with the same name
    Assert.assertEquals(1, loaded.getAllClassesSorted().stream().filter(c -> c.getName().equals("test class 2")).count());
  }
  
  @Test
  public void testLoadModuleWithStdLibFlag() throws PlomReadException
  {
    String codeStr = " module .{program} {\n" + 
        " stdlib {-1 } \n" + 
        " }";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ModuleCodeRepository loaded = new ModuleCodeRepository();
    loaded.loadModule(lexer);
    
    Assert.assertTrue(loaded.isNoStdLibFlag);
  }
}
