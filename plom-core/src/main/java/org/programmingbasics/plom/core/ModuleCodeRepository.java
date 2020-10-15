package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibClass;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibMethod;

public class ModuleCodeRepository
{
  public static class FunctionSignature
  {
    public List<String> nameParts = new ArrayList<>();
    public List<String> argNames = new ArrayList<>();
    public List<Token.ParameterToken> argTypes = new ArrayList<>();
    public Token.ParameterToken returnType;
    public boolean isBuiltIn = false;
    public boolean isStatic = false;
    public boolean isConstructor = false;
    public String getLookupName()
    {
      String name = "";
      for (int n = 0; n < nameParts.size(); n++)
      {
        name += nameParts.get(n);
        if (!argNames.isEmpty())
          name += ":";
      }
      return name;
    }
    
    public String getDisplayName()
    {
      String name = "";
      for (int n = 0; n < nameParts.size(); n++)
      {
        if (n != 0)
          name += " ";
        name += nameParts.get(n);
        if (!argNames.isEmpty())
        {
          name += ": (.";
          name += argNames.get(n);
          name += " @";
          if (argTypes.get(n) != null)
            name += argTypes.get(n).getLookupName();
          name += ")";
        }
      }
      if (returnType != null)
      {
        name += " @" + returnType.getLookupName();
      }
      return name;
    }
    public FunctionSignature setIsConstructor(boolean val)
    {
      isConstructor = val;
      return this;
    }
    public FunctionSignature setIsStatic(boolean val)
    {
      isStatic = val;
      return this;
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name)
    {
      return from(returnType, Arrays.asList(name.split(":")), new ArrayList<>(), new ArrayList<>(), null);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name), Arrays.asList(arg1Type), null);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name), Arrays.asList(arg1Type, arg2Type), null);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type, String arg3Name, Token.ParameterToken arg3Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name, arg3Name), Arrays.asList(arg1Type, arg2Type, arg3Type), null);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, List<String> argNames, List<Token.ParameterToken> argTypes)
    {
      return from(returnType, Arrays.asList(name.split(":")), argNames, argTypes, null);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, List<String> nameParts, List<String> argNames, List<Token.ParameterToken> argTypes, FunctionSignature oldSig)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.returnType = returnType;
      sig.nameParts = nameParts;
      sig.argNames = argNames;
      sig.argTypes = argTypes;
      if (oldSig != null)
      {
        sig.isConstructor = oldSig.isConstructor;
        sig.isBuiltIn = oldSig.isBuiltIn;
        sig.isStatic = oldSig.isStatic;
      }
      return sig;
    }
  }

  public static class DescriptionWithId
  {
    public int id;
  }
  
  public static class FunctionDescription extends DescriptionWithId
  {
    public FunctionDescription(FunctionSignature sig, StatementContainer code)
    {
      this.sig = sig;
      this.code = code;
    }
    public FunctionSignature sig;
    public StatementContainer code;
    public boolean isImported;
    public FunctionDescription setImported(boolean isImported) { this.isImported = isImported; return this;}
  }

  public static class VariableDescription extends DescriptionWithId
  {
    public String name;
    public Token.ParameterToken type;
    public boolean isImported;
  }

  public static class ClassDescription extends DescriptionWithId
  {
    public String name;
    List<FunctionDescription> methods = new ArrayList<>();
    List<VariableDescription> variables = new ArrayList<>();
    public boolean isBuiltIn = false;
    public boolean isImported;
    public List<FunctionDescription> getInstanceMethods()
    {
      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
          .stream().filter(fn -> !fn.sig.isConstructor && !fn.sig.isStatic).collect(Collectors.toList());
    }
    public List<FunctionDescription> getStaticAndConstructorMethods()
    {
      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
          .stream().filter(fn -> fn.sig.isConstructor || fn.sig.isStatic).collect(Collectors.toList());
    }
    public void addMethod(FunctionDescription f)
    {
      f.id = methods.size();
      methods.add(f);
    }
    public boolean hasMethodWithName(String name)
    {
      for (FunctionDescription m: methods)
      {
        if (m.sig.getLookupName().equals(name)) 
          return true;
      }
      return false;
    }
    public void addVarAndResetIds(String name, Token.ParameterToken type)
    {
      // Add a new variable at the beginning of the list so that it's more likely
      // to appear near the top of the variable list
      VariableDescription v = new VariableDescription();
      v.name = name;
      v.type = type;
      variables.add(0, v);
    }
    public List<VariableDescription> getAllVars()
    {
      return getSortedWithIds(variables, Comparator.comparing((VariableDescription v) -> v.name));
    }
    public void updateVariable(VariableDescription v)
    {
      variables.set(v.id, v);
    }
    public void updateMethod(FunctionDescription m)
    {
      // No change needed currently
    }
    public void setName(String name)
    {
      this.name = name;
    }
    public ClassDescription setBuiltIn(boolean isBuiltIn) { this.isBuiltIn = isBuiltIn; return this; }
    public boolean hasNonBuiltInMethods()
    {
      return !methods.stream().allMatch(fn -> fn.sig.isBuiltIn);
    }
    public ClassDescription setImported(boolean isImported) { this.isImported = isImported; return this; }
  }
  
  private Map<String, FunctionDescription> functions = new HashMap<>();
  
  /** Lists global variables and their types */
  List<VariableDescription> globalVars = new ArrayList<>();

  /** All classes */
  List<ClassDescription> classes = new ArrayList<>();
  
  public ModuleCodeRepository()
  {
//    // Create a basic main function that can be filled in
//    FunctionDescription func = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.SimpleToken("var", Symbol.Var),
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":", Symbol.Colon),
//                Token.ParameterToken.fromContents("@string", Symbol.AtType)
//                ),
//            new TokenContainer(
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":=", Symbol.Assignment),
//                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
//                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
//                ),
//            new TokenContainer(
//                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
//                    new TokenContainer(
//                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                        new Token.SimpleToken("=", Symbol.Eq),
//                        new Token.SimpleToken("\"8\"", Symbol.String)
//                        ),
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
//                                    ))
//                            ))
//                    ),
//                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
//                                    ))
//                            ))
//                    )
//                )
//            )
//        );
//    functions.put(func.sig.getLookupName(), func);
    
//    FunctionDescription testParamFunc = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), Arrays.asList("test"), Arrays.asList("arg1"), Arrays.asList(Token.ParameterToken.fromContents("@number", Symbol.AtType)), null),
//        new StatementContainer());
//    functions.put(testParamFunc.sig.getLookupName(), testParamFunc);
//    
//    FunctionDescription printStringPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print string:", "value", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Prints a string to the screen", Symbol.DUMMY_COMMENT),
//                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
//            ));
//    functions.put(printStringPrimitive.sig.getLookupName(), printStringPrimitive);
//
//    FunctionDescription printPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print:", "value", Token.ParameterToken.fromContents("@object", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Prints a value to the screen", Symbol.DUMMY_COMMENT),
//                Token.ParameterToken.fromContents(".print string:", Symbol.DotVariable, 
//                    new TokenContainer(
//                        Token.ParameterToken.fromContents(".value", Symbol.DotVariable),
//                        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable))
//                    )
//            )));
//    functions.put(printPrimitive.sig.getLookupName(), printPrimitive);
//
//    FunctionDescription inputPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@string", Symbol.AtType), "input:", "prompt", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Displays a prompt asking for input and returns the value entered by the user", Symbol.DUMMY_COMMENT),
//                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
//            ));
//    functions.put(inputPrimitive.sig.getLookupName(), inputPrimitive);
//
//    addGlobalVarAndResetIds("var", Token.ParameterToken.fromContents("@object", Symbol.AtType));
//    
//    ClassDescription testClass = addClassAndResetIds("Test");
//    testClass.addMethod(new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "new")
//            .setIsConstructor(true),
//        new StatementContainer()));
  }

  public void setChainedRepository(ModuleCodeRepository other)
  {
    chainedRepository = other;
  }
  
  ModuleCodeRepository chainedRepository;
  
  public static String findUniqueName(String base, Function<String, Boolean> isNameAvailable)
  {
    String name = base;
    int postfix = 0;
    while (!isNameAvailable.apply(name))
    {
      postfix++;
      name = base + " " + postfix;
    }
    return name;
  }

  public static <U extends DescriptionWithId> List<U> getSortedWithIds(List<U> unsorted, Comparator<U> comparator)
  {
    // Assign ids to all the items and put them in a sorted list
    List<U> toReturn = new ArrayList<>();
    for (int n = 0; n < unsorted.size(); n++)
    {
      U cls = unsorted.get(n);
      cls.id = n;
      toReturn.add(cls);
    }
    toReturn.sort(comparator);
    return toReturn;
  }

  public FunctionDescription getFunctionDescription(String name)
  {
    FunctionDescription fn = functions.get(name);
    if (fn == null && chainedRepository != null)
      return chainedRepository.getFunctionDescription(name);
    return fn;
  }
  
  public boolean hasFunctionWithName(String name)
  {
    return functions.containsKey(name);
  }
  
  public void addFunction(FunctionDescription func)
  {
    functions.put(func.sig.getLookupName(), func);
  }
  
  private void fillChainedFunctions(List<String> mergedFunctions)
  {
    mergedFunctions.addAll(functions.keySet());
  }
  
  public List<String> getAllFunctionsSorted()
  {
    List<String> names = new ArrayList<>(functions.keySet());
    if (chainedRepository != null)
      chainedRepository.fillChainedFunctions(names);
    names.sort(Comparator.naturalOrder());
    return names;
  }
  
  public void changeFunctionSignature(FunctionSignature newSig, FunctionSignature oldSig)
  {
    FunctionDescription func = functions.remove(oldSig.getLookupName());
    func.sig = newSig;
    functions.put(func.sig.getLookupName(), func);
  }
  
  private void fillChainedGlobalVars(List<VariableDescription> mergedGlobalVars)
  {
    mergedGlobalVars.addAll(globalVars);
  }
  
  public List<VariableDescription> getAllGlobalVarsSorted()
  {
    List<VariableDescription> mergedGlobalVars = new ArrayList<>(globalVars);
    if (chainedRepository != null)
      chainedRepository.fillChainedGlobalVars(mergedGlobalVars);
    return getSortedWithIds(mergedGlobalVars, Comparator.comparing((VariableDescription v) -> v.name));
  }
  
  public void addGlobalVarAndResetIds(String name, Token.ParameterToken type)
  {
    // Add a new variable at the beginning of the list so that it's more likely
    // to appear near the top of the variable list
    VariableDescription v = new VariableDescription();
    v.name = name;
    v.type = type;
    globalVars.add(0, v);
  }
  
  public void updateGlobalVariable(VariableDescription v)
  {
    globalVars.set(v.id, v);
  }
  
  private void fillChainedClasses(List<ClassDescription> mergedClassList)
  {
    mergedClassList.addAll(classes);
  }
  
  public List<ClassDescription> getAllClassesSorted()
  {
    List<ClassDescription> mergedClassList = new ArrayList<>(classes);
    if (chainedRepository != null)
      chainedRepository.fillChainedClasses(mergedClassList);
    return getSortedWithIds(mergedClassList, Comparator.comparing((ClassDescription v) -> v.name));
  }

  public ClassDescription addClassAndResetIds(String name)
  {
    ClassDescription cls = new ClassDescription();
    cls.name = name;
    classes.add(0, cls);
    cls.id = 0;
    return cls;
  }
  
  public boolean hasClassWithName(String name)
  {
    for (ClassDescription c: classes)
    {
      if (c.name.equals(name)) 
        return true;
    }
    return false;
  }

  /**
   * Marks all the contents of this module as being imported
   */
  public void markAsImported()
  {
    for (ClassDescription c: classes)
      c.setImported(true);
    for (FunctionDescription fn: functions.values())
      fn.setImported(true);
    for (VariableDescription v: globalVars)
      v.isImported = true;
  }
  
  public void loadBuiltInPrimitives(List<StdLibClass> stdLibClasses, List<StdLibMethod> stdLibMethods)
  {
    Map<String, ClassDescription> classMap = new HashMap<>();
    for (ClassDescription c: classes)
      classMap.put(c.name, c);
    for (StdLibClass clsdef: stdLibClasses)
    {
      if (classMap.containsKey(clsdef.name)) continue;
      ClassDescription c = addClassAndResetIds(clsdef.name)
          .setBuiltIn(true);
      classMap.put(clsdef.name, c);
    }
    
    for (StdLibMethod mdef: stdLibMethods)
    {
      ClassDescription c = classMap.get(mdef.className);
      FunctionSignature sig = FunctionSignature.from(Token.ParameterToken.fromContents("@" + mdef.returnType, Symbol.AtType), mdef.methodName, mdef.argNames, mdef.argTypes.stream().map(typeStr -> Token.ParameterToken.fromContents("@" + typeStr, Symbol.AtType)).collect(Collectors.toList()));
      if (mdef.isStatic)
        sig.setIsStatic(true);
      else if (mdef.isConstructor)
        sig.setIsConstructor(true);
      sig.isBuiltIn = true;
      c.addMethod(new FunctionDescription(sig, mdef.code));
    }
  }
  
  public void saveModule(PlomTextWriter.PlomCodeOutputFormatter out) throws IOException
  {
    out.token("module");
    out.token(".{program}");
    out.token("{");
    out.newline();

    // Output global variables
    for (VariableDescription v: globalVars)
    {
      saveVariable(out, v);
    }
    
    // Output global functions
    for (FunctionDescription fn: functions.values())
    {
      saveFunction(out, fn);
    }
    
    // Output classes
    for (ClassDescription cls: classes)
    {
      if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
        saveClass(out, cls);
    }

    out.token("}");
  }
  
  public static void saveClass(PlomTextWriter.PlomCodeOutputFormatter out, ClassDescription c) throws IOException
  {
    out.token("class");
    out.token(".");
    out.token("{");
    out.token(c.name);
    out.token("}");
    
    out.token("{");
    out.newline();

    for (VariableDescription v: c.getAllVars())
    {
      saveVariable(out, v);
    }
    
    for (FunctionDescription fn: c.getInstanceMethods())
    {
      if (fn.sig.isBuiltIn) continue;
      saveFunction(out, fn);
    }

    for (FunctionDescription fn: c.getStaticAndConstructorMethods())
    {
      if (fn.sig.isBuiltIn) continue;
      saveFunction(out, fn);
      
    }

    out.token("}");
    out.newline();
  }

  private static void saveVariable(PlomTextWriter.PlomCodeOutputFormatter out,
      VariableDescription v) throws IOException
  {
    out.token("var");
    out.token(".");
    out.token("{");
    out.token(v.name);
    out.token("}");
    PlomTextWriter.writeToken(out, v.type);
    out.newline();
  }
  
  static void saveFunction(PlomTextWriter.PlomCodeOutputFormatter out, FunctionDescription fn) throws IOException
  {
    if (fn.sig.isStatic)
      out.token("classfunction");
    else if (fn.sig.isConstructor)
      out.token("constructor");
    else
      out.token("function");
    
    out.token(".");
    out.token("{");
    if (fn.sig.argNames.isEmpty())
    {
      out.append(fn.sig.nameParts.get(0));
    }
    else
    {
      for (int n = 0; n < fn.sig.argNames.size(); n++)
      {
        out.append(fn.sig.nameParts.get(n) + ":");
        out.token(".");
        out.token("{");
        out.token(fn.sig.argNames.get(n));
        out.token("}");
        PlomTextWriter.writeToken(out, fn.sig.argTypes.get(n));
      }
    }
    out.token("}");
    
    if (!fn.sig.isConstructor)
    {
      PlomTextWriter.writeToken(out, fn.sig.returnType);
    }
    
    out.token("{");
    out.newline();
    PlomTextWriter.writeStatementContainer(out, fn.code);
    out.token("}");
    out.newline();
  }
  
  public void loadModule(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    lexer.expectToken("module");
    lexer.expectToken(".");
    lexer.expectToken("{");
    String moduleName = lexer.lexParameterTokenPart();
    lexer.expectToken("}");
    
    lexer.expectToken("{");
    lexer.swallowOptionalNewlineToken();
    
    while (!"}".equals(lexer.peekLexInput()))
    {
      String peek = lexer.peekLexInput();
      if ("var".equals(peek))
      {
        VariableDescription v = loadVariable(lexer);
        addGlobalVarAndResetIds(v.name, v.type);
        lexer.expectNewlineToken();
      }
      else if ("function".equals(peek))
      {
        FunctionDescription fn = loadFunction(lexer);
        addFunction(fn);
      }
      else if ("class".equals(peek))
      {
        ClassDescription loaded = loadClass(lexer);
        ClassDescription cls = null;
        boolean augmentClass = false;
        // See if we have a built-in class with the same name. If so,
        // we'll just fill-in the extra defined methods. Otherwise, we'll
        // actually create a new class
        for (ClassDescription c: classes)
        {
          if (c.isBuiltIn && c.name.equals(loaded.name))
          {
            cls = c;
            augmentClass = true;
            break;
          }
        }
        if (cls == null)
          cls = addClassAndResetIds(loaded.name);
        if (!augmentClass)
        {
          for (VariableDescription v: loaded.variables)
          {
            cls.addVarAndResetIds(v.name, v.type);
          }
        }
        for (FunctionDescription fn: loaded.methods)
        {
          cls.addMethod(fn);
        }
      }
      else
        throw new PlomReadException("Unexpected module contents", lexer);
    }
    
    lexer.expectToken("}");
  }
  
  public static ClassDescription loadClass(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    lexer.expectToken("class");
    lexer.expectToken(".");
    lexer.expectToken("{");
    String className = lexer.lexParameterTokenPart();
    if (className == null)
      throw new PlomReadException("Cannot read class name", lexer);
    lexer.expectToken("}");
    
    ClassDescription cls = new ClassDescription();
    cls.name = className;
    
    lexer.expectToken("{");
    lexer.swallowOptionalNewlineToken();
    
    while (!"}".equals(lexer.peekLexInput()))
    {
      String peek = lexer.peekLexInput();
      if ("var".equals(peek))
      {
        VariableDescription v = loadVariable(lexer);
        cls.addVarAndResetIds(v.name, v.type);
        lexer.expectNewlineToken();
      }
      else if ("classfunction".equals(peek) || "function".equals(peek) || "constructor".equals(peek))
      {
        cls.addMethod(loadFunction(lexer));
      }
      else
        throw new PlomReadException("Unexpected class contents", lexer);
    }
    
    lexer.expectToken("}");
    lexer.swallowOptionalNewlineToken();
    
    return cls;
  }

  private static VariableDescription loadVariable(
      PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    VariableDescription v = new VariableDescription();
    lexer.expectToken("var");
    lexer.expectToken(".");
    lexer.expectToken("{");
    String varName = lexer.lexParameterTokenPart();
    lexer.expectToken("}");
    
    // Read the variable type
    Token.ParameterToken type = (Token.ParameterToken)PlomTextReader.readToken(lexer);
    v.name = varName;
    v.type = type;
    return v;
  }
  
  static FunctionDescription loadFunction(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    boolean isConstructor = "constructor".equals(lexer.peekLexInput());
    boolean isStatic = "classfunction".equals(lexer.peekLexInput());
    boolean isFunction = "function".equals(lexer.peekLexInput());
    if (!isConstructor && !isStatic && !isFunction)
      throw new PlomReadException("Unknown function type", lexer);
    lexer.lexInput();

    lexer.expectToken(".");
    lexer.expectToken("{");
    String fnName = "";
    List<String> argNames = new ArrayList<>();
    List<Token.ParameterToken> argTypes = new ArrayList<>();
    String namePart = lexer.lexParameterTokenPart();
    if (namePart == null)
      throw new PlomReadException("Expecting function name", lexer);
    if (namePart.endsWith(":"))
    {
      while (namePart != null)
      {
        fnName += namePart;
        lexer.expectToken(".");
        lexer.expectToken("{");
        argNames.add(lexer.lexParameterTokenPart());
        lexer.expectToken("}");
        argTypes.add((Token.ParameterToken)PlomTextReader.readToken(lexer));
        namePart = lexer.lexParameterTokenPart();
      }
    }
    else
    {
      fnName += namePart;
    }
    lexer.expectToken("}");
    
    Token.ParameterToken returnType;
    if (!isConstructor)
      returnType = (Token.ParameterToken)PlomTextReader.readToken(lexer);
    else
      returnType = Token.ParameterToken.fromContents("@void", Symbol.AtType);
    
    FunctionSignature sig = FunctionSignature.from(returnType, fnName, argNames, argTypes);
    if (isConstructor)
      sig.isConstructor = true;
    if (isStatic)
      sig.isStatic = true;

    lexer.expectToken("{");
    lexer.swallowOptionalNewlineToken();
    StatementContainer code = PlomTextReader.readStatementContainer(lexer);
    lexer.expectToken("}");
    lexer.swallowOptionalNewlineToken();
    
    return new FunctionDescription(sig, code);
  }
}
