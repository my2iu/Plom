package org.programmingbasics.plom.core.codestore;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.UnboundType;

import junit.framework.TestCase;

public class ModuleCodeRepositoryTest extends TestCase
{
  static ModuleCodeRepository repository = new ModuleCodeRepository();
  static {
    repository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    repository.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(Arrays.asList(new Token.SimpleToken("var", Symbol.Var))),
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".var", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@string", Symbol.AtType)
            )
        ));
//    repository.addGlobalVarAndResetIds("var", Token.ParameterToken.fromContents("@string", Symbol.AtType));
    repository.addFunctionAndResetIds(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "get"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));
    ClassDescription testClass = repository.addClassAndResetIds("test class");
    testClass.setSuperclass(null);
    testClass.addMethod(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "at x:y:",
            "x", UnboundType.forClassLookupName("number"),
            "y", UnboundType.forClassLookupName("number")),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                Token.ParameterToken.fromContents(".x", Symbol.DotVariable)))));
    testClass.addMethod(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("void"), "new")
            .setIsConstructor(true),
        new StatementContainer()));
//    testClass.addVarAndResetIds("test var", Token.ParameterToken.fromContents("@test class", Symbol.AtType));
    testClass.setVariableDeclarationCode(new StatementContainer(
        new TokenContainer(
            new Token.SimpleToken("var", Symbol.Var),
            Token.ParameterToken.fromContents(".test var", Symbol.DotVariable),
            Token.ParameterToken.fromContents("@test class", Symbol.AtType)
            ),
        new TokenContainer(Arrays.asList(new Token.SimpleToken("var", Symbol.Var)))));
  }
  
  @Test
  public void testSaveClass() throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    ModuleCodeRepository.saveClass(out, repository.getAllClassesSorted().stream().filter(cls -> cls.getName().equals("test class")).findFirst().get());
    Assert.assertEquals(" class @ { test class } {\n" + 
        " vardecls {\n" + 
        " var . {test var } @ {test class }\n" +
        " var\n" +
        " }\n" +
        " function . {at x: { . {x } @ {number } }y: { . {y } @ {number } } } { @ {number } } { } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } { } {\n" + 
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
    c.setSuperclass(UnboundType.forClassLookupName("object"));
    ModuleCodeRepository.saveClass(out, c);
    Assert.assertEquals(" class @ { test class } extends @ {object } {\n" + 
        " vardecls {\n" + 
        " var . {test var } @ {test class }\n" +
        " var\n" +
        " }\n" + 
        " function . {at x: { . {x } @ {number } }y: { . {y } @ {number } } } { @ {number } } { } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } { } {\n" + 
        " }\n" + 
        " }\n", 
        strBuilder.toString());
    c.setSuperclass(null);
  }

  @Test
  public void testLoadClass() throws PlomReadException
  {
    String codeStr = "class @{test class} {\n"
//        + "  var .{test var} @{test class}\n"
        + "  vardecls {\n" 
        + "    var .{var decl} @{test class}\n"
        + "  }\n"
        + "  function . {at x:{.{x} @{number}} y: {.{ y} @{number}} } {@{number}} {} {\n" 
        + "    return . {x }\n" 
        + "  }\n" 
        + "  constructor . {new } { } {\n" 
        + "  }\n" 
        + "}\n";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ClassDescription cls = ModuleCodeRepository.loadClass(lexer);
    Assert.assertEquals("test class", cls.getName());
    Assert.assertNull(cls.parent);
    Assert.assertEquals(1, cls.getVariableDeclarationCode().statements.size());
    Assert.assertEquals(new TokenContainer(
        new Token.SimpleToken("var", Symbol.Var),
        Token.ParameterToken.fromContents(".var decl", Symbol.DotVariable),
        Token.ParameterToken.fromContents("@test class", Symbol.AtType)), cls.getVariableDeclarationCode().statements.get(0));
//    Assert.assertEquals(1, cls.variables.size());
//    Assert.assertEquals("test var", cls.variables.get(0).name);
//    Assert.assertEquals("test class", cls.variables.get(0).type.getLookupName());
    Assert.assertEquals(2, cls.methods.size());
    Assert.assertEquals("at x:y:", cls.methods.get(0).sig.getLookupName());
    Assert.assertEquals("y", cls.methods.get(0).sig.getArgName(1));
    Assert.assertEquals("number", cls.methods.get(0).sig.getArgType(1).mainToken.getLookupName());
    Assert.assertEquals("number", cls.methods.get(0).sig.getReturnType().mainToken.getLookupName());
    Assert.assertEquals("new", cls.methods.get(1).sig.getLookupName());
    Assert.assertEquals("void", cls.methods.get(1).sig.getReturnType().mainToken.getLookupName());
  }

  @Test
  public void testLoadClassWithSupertype() throws PlomReadException
  {
    String codeStr = "class @{test class} extends @{object} {\n"
        + "  constructor . {new } {} {\n" 
        + "  }\n" 
        + "}\n";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ClassDescription cls = ModuleCodeRepository.loadClass(lexer);
    Assert.assertEquals("test class", cls.getName());
    Assert.assertEquals("object", cls.parent.mainToken.getLookupName());
    Assert.assertEquals(1, cls.methods.size());
    Assert.assertEquals("new", cls.methods.get(0).sig.getLookupName());
    Assert.assertEquals("void", cls.methods.get(0).sig.getReturnType().mainToken.getLookupName());
  }

  @Test
  public void testSaveModule() throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    repository.saveModule(out, true);
    Assert.assertEquals(" module .{program} {\n" + 
        " vardecls {\n" +
        " var\n" +
        " var . {var } @ {string }\n" +
        " }\n" +
        " function . {get } { @ {number } } { } {\n" + 
        " return 3\n" + 
        " }\n" + 
        " class @ { test class } {\n" + 
        " vardecls {\n" + 
        " var . {test var } @ {test class }\n" + 
        " var\n" +
        " }\n" +
        " function . {at x: { . {x } @ {number } }y: { . {y } @ {number } } } { @ {number } } { } {\n" + 
        " return . {x }\n" + 
        " }\n" + 
        " constructor . {new } { } {\n" + 
        " }\n" + 
        " }\n" + 
        " }",
        strBuilder.toString());
  }
  
  @Test
  public void testLoadModule() throws PlomReadException
  {
    String codeStr = " module .{program} {\n" + 
//        " var . { variable } @ {string }\n" + 
        " vardecls {\n" +
        " var\n" +
        " var .{variable} @{string}\n" +
        " }\n" +
        " function . {get } {@ {number }} {} {\n" + 
        " return 3\n" + 
        " }\n" + 
        " function . {test: {. { arg1 } @ {number }} } {@ {number }} {} {\n" + 
        " }\n" + 
        " function . { new style: {.{a} @{number}} function: {.{b} @{boolean}}} {@{void}} {} {\n" +
        "   return\n" +
        " }\n" +
        " class @ { test class 2} {\n" + 
        " }\n" + 
        " class @ { test class } {\n" + 
        " vardecls {\n" +
        " var . { test var } @ {test class }\n" + 
        " var\n" +
        " }\n" +
        " function . {at x: {. { x } @ {number }}y: {. { y } @ {number } }} {@ {number }} {} {\n" + 
        " return . {x }\n" + 
        " }\n" +
        " constructor . {new } {} {\n" + 
        " }\n" + 
        " }\n" + 
        " }";
    
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    
    ModuleCodeRepository loaded = new ModuleCodeRepository();
    loaded.addClassAndResetIds("test class 2").setBuiltIn(true);
    loaded.loadModulePlain(lexer, null);
    
    Assert.assertTrue(loaded.getFunctionWithName("get") != null);
    FunctionDescription newStyleFn = loaded.getFunctionWithName("new style:function:");
    Assert.assertTrue(newStyleFn != null);
    Assert.assertEquals("new style: (.a @number) function: (.b @boolean) @void", newStyleFn.sig.getDisplayName());
//    Assert.assertTrue(loaded.getAllGlobalVarsSorted().stream().anyMatch(v -> v.name.equals("variable")));
//    Assert.assertTrue(loaded.getAllGlobalVarsSorted().stream().anyMatch(v -> v.name.equals("variable") && v.type.getLookupName().equals("string")));
    Assert.assertEquals(loaded.getVariableDeclarationCode().statements.size(), 2);
    Assert.assertEquals(loaded.getVariableDeclarationCode().statements.get(0).tokens.size(), 1);
    Assert.assertEquals(loaded.getVariableDeclarationCode().statements.get(0).tokens.get(0), new Token.SimpleToken("var", Symbol.Var));
    Assert.assertTrue(loaded.hasClassWithName("test class"));
    ClassDescription loadedTestClass = loaded.getAllClassesSorted().stream().filter(c -> c.getName().equals("test class")).findFirst().get();
    Assert.assertEquals(loadedTestClass.getVariableDeclarationCode().statements.size(), 2);
    Assert.assertEquals(loadedTestClass.getVariableDeclarationCode().statements.get(1).tokens.size(), 1);
    Assert.assertEquals(loadedTestClass.getVariableDeclarationCode().statements.get(1).tokens.get(0), new Token.SimpleToken("var", Symbol.Var));
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
    loaded.loadModulePlain(lexer, null);
    
    Assert.assertTrue(loaded.isNoStdLibFlag);
  }
  
  @Test
  public void testLoadFunction() throws PlomReadException
  {
    String oldFnStr = 
        " function . {get } {@ {number }}{} {\n" + 
        " return 3\n" + 
        " }\n";
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(oldFnStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    FunctionDescription fn = ModuleCodeRepository.loadFunction(lexer);
    Assert.assertEquals("get @number", fn.sig.getDisplayName());

    String newStyleFnStr = 
        " function . { new style: {.{a} @{number}} function: {.{b} @{boolean}}} {@{void}} {} {\n" +
        "   return\n" +
        " }\n";
    in = new PlomTextReader.StringTextReader(newStyleFnStr);
    lexer = new PlomTextReader.PlomTextScanner(in);
    fn = ModuleCodeRepository.loadFunction(lexer);
    Assert.assertEquals("new style: (.a @number) function: (.b @boolean) @void", fn.sig.getDisplayName());
  }
  
  @Test
  public void testSaveFunction() throws PlomReadException, IOException
  {
    FunctionDescription fn = new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "at x:y:",
            "x", UnboundType.forClassLookupName("number"),
            "y", UnboundType.forClassLookupName("number")),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                Token.ParameterToken.fromContents(".x", Symbol.DotVariable))));

    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

    ModuleCodeRepository.saveFunction(out, fn);
    String fnStr = strBuilder.toString();
    Assert.assertEquals(" function . {at x: { . {x } @ {number } }y: { . {y } @ {number } } } { @ {number } } { } {\n"
        + " return . {x }\n"
        + " }\n",
        fnStr);
    
    // See if constructor version can also save out
    FunctionSignature constructorSig = FunctionSignature.copyOf(fn.sig);
    constructorSig.isConstructor = true;
    FunctionDescription constructor = new FunctionDescription(
        constructorSig,
        new StatementContainer());
    strBuilder.setLength(0);
    ModuleCodeRepository.saveFunction(out, constructor);
    String constructorStr = strBuilder.toString();
    Assert.assertEquals(" constructor . {at x: { . {x } @ {number } }y: { . {y } @ {number } } } { } {\n"
        + " }\n",
        constructorStr);
    
    // Load back in and check
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(fnStr);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    FunctionDescription loadedFn = ModuleCodeRepository.loadFunction(lexer);
    loadedFn.sig.canReplace(fn.sig);

    in = new PlomTextReader.StringTextReader(constructorStr);
    lexer = new PlomTextReader.PlomTextScanner(in);
    FunctionDescription loadedConstructor = ModuleCodeRepository.loadFunction(lexer);
    loadedConstructor.sig.canReplace(constructor.sig);
  }

  
}
