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

import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.MethodArgumentExtractor;
import org.programmingbasics.plom.core.interpreter.ReturnTypeExtractor;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibClass;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibMethod;
import org.programmingbasics.plom.core.interpreter.UnboundType;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import jsinterop.annotations.JsType;

@JsType
public class ModuleCodeRepository
{
  public static class FunctionSignature
  {
    public List<String> nameParts = new ArrayList<>();
    public List<String> argNames = new ArrayList<>();
    private List<UnboundType> argTypes = new ArrayList<>();
    private List<TokenContainer> argCode = new ArrayList<>();
    private UnboundType returnType;
    private TokenContainer returnTypeCode = new TokenContainer();
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
            name += argTypes.get(n).mainToken.getLookupName();
          name += ")";
        }
      }
      if (getReturnType() != null)
      {
        name += " @" + getReturnType().mainToken.getLookupName();
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
    public int getNumArgs() { return argTypes.size(); }
    public String getArgName(int idx) { return argNames.get(idx); }
    public UnboundType getArgType(int idx) { return argTypes.get(idx); }
    public void addNewArgCode() 
    { 
      argCode.add(new TokenContainer());
      if (argNames.size() < argCode.size())
      {
        argNames.add("val");
        argTypes.add(UnboundType.forClassLookupName("object"));
      }
    }
    public void removeArgCode(int idx) 
    {
      argNames.remove(idx);
      argTypes.remove(idx);
      argCode.remove(idx); 
    }
    public TokenContainer getArgCode(int idx) { return argCode.get(idx); }
    public void setArgCode(int idx, TokenContainer argCode)
    {
      this.argCode.set(idx, argCode);
      class NameAndType {
        String name;
        UnboundType type;
      }
      NameAndType nameAndType = new NameAndType();
      MethodArgumentExtractor.fromParameterField(argCode, 
          (name, unboundType) -> {
            nameAndType.name = name;
            nameAndType.type = unboundType;
          },
//          (unboundType) -> {
//            // Just pass the raw type name through unchanged
//            return new Type(unboundType.mainToken.getLookupName());
//          },
          null);
      if (nameAndType.name != null)
      {
        argNames.set(idx, nameAndType.name);
        argTypes.set(idx, nameAndType.type);
      }
    }
    public UnboundType getReturnType() { return returnType; }
    public TokenContainer getReturnTypeCode() { return returnTypeCode; }
    public void setReturnTypeCode(TokenContainer code)
    {
      returnTypeCode = code;
      returnType = ReturnTypeExtractor.fromReturnFieldTokenContainer(code);
    }
    // Does a function signature match another one (i.e. do they refer to the same function)
    public boolean canReplace(FunctionSignature fn)
    {
//      if (isBuiltIn != fn.isBuiltIn) return false;
      if (isStatic != fn.isStatic) return false; 
      if (isConstructor != fn.isConstructor) return false;
      if (!getReturnTypeCode().equals(fn.getReturnTypeCode())) return false;
      if (nameParts.size() != fn.nameParts.size()) return false;
      if (getNumArgs() != fn.getNumArgs()) return false;
      for (int n = 0; n < nameParts.size(); n++)
      {
        if (!nameParts.get(n).equals(fn.nameParts.get(n))) return false;
      }
      for (int n = 0; n < getNumArgs(); n++)
      {
        if (!getArgName(n).equals(fn.getArgName(n))) return false;
        if (!getArgType(n).equals(fn.getArgType(n))) return false;
        
      }
      return true;
    }
    public static FunctionSignature from(UnboundType returnType, String name)
    {
      return from(returnType, Arrays.asList(name.split(":")), new ArrayList<>(), new ArrayList<>(), null);
    }
    public static FunctionSignature from(UnboundType returnType, String name, String arg1Name, UnboundType arg1Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name), Arrays.asList(arg1Type), null);
    }
    public static FunctionSignature from(UnboundType returnType, String name, String arg1Name, UnboundType arg1Type, String arg2Name, UnboundType arg2Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name), Arrays.asList(arg1Type, arg2Type), null);
    }
    public static FunctionSignature from(UnboundType returnType, String name, String arg1Name, UnboundType arg1Type, String arg2Name, UnboundType arg2Type, String arg3Name, UnboundType arg3Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name, arg3Name), Arrays.asList(arg1Type, arg2Type, arg3Type), null);
    }
    public static FunctionSignature from(UnboundType returnType, String name, List<String> argNames, List<UnboundType> argTypes)
    {
      return from(returnType, Arrays.asList(name.split(":")), argNames, argTypes, null);
    }
    public static FunctionSignature from(UnboundType returnType, List<String> nameParts, List<String> argNames, List<UnboundType> argTypes, FunctionSignature oldSig)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.setReturnTypeCode(new TokenContainer(returnType.mainToken));
      sig.nameParts = nameParts;
      sig.argNames = argNames;
      sig.argTypes = argTypes;
      for (int n = 0; n < argNames.size(); n++)
      {
        TokenContainer code = argCodeFromNameType(argNames.get(n), argTypes.get(n));
        sig.addNewArgCode();
        sig.setArgCode(n, code);
      }
      if (oldSig != null)
      {
        sig.isConstructor = oldSig.isConstructor;
        sig.isBuiltIn = oldSig.isBuiltIn;
        sig.isStatic = oldSig.isStatic;
      }
      return sig;
    }
    public static FunctionSignature from(TokenContainer returnTypeCode, List<String> nameParts, List<TokenContainer> argCodes, FunctionSignature oldSig)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.setReturnTypeCode(returnTypeCode);
      sig.nameParts = nameParts;
      for (int n = 0; n < argCodes.size(); n++)
      {
        sig.addNewArgCode();
        sig.setArgCode(n, argCodes.get(n).copy());
      }
      if (oldSig != null)
      {
        sig.isConstructor = oldSig.isConstructor;
        sig.isBuiltIn = oldSig.isBuiltIn;
        sig.isStatic = oldSig.isStatic;
      }
      return sig;
    }
    public static FunctionSignature copyOf(FunctionSignature oldSig)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.setReturnTypeCode(oldSig.returnTypeCode);
      sig.nameParts = new ArrayList<>(oldSig.nameParts);
      sig.argNames = new ArrayList<>(oldSig.argNames);
      sig.argTypes = new ArrayList<>();
      for (UnboundType argType: oldSig.argTypes)
        sig.argTypes.add(argType.copy());
      sig.argCode = new ArrayList<>();
      for (TokenContainer argCode: oldSig.argCode)
        sig.argCode.add(argCode.copy());
      sig.isConstructor = oldSig.isConstructor;
      sig.isBuiltIn = oldSig.isBuiltIn;
      sig.isStatic = oldSig.isStatic;
      return sig;
    }
    public static TokenContainer argCodeFromNameType(String argName, UnboundType argType)
    {
      return new TokenContainer(
          Token.ParameterToken.fromContents("." + argName, Symbol.DotVariable),
          argType.mainToken);
    }
  }

  public static class DescriptionWithId
  {
    public int id;
  }
  
  @JsType
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
    public ModuleCodeRepository module;
    public FunctionDescription setImported(boolean isImported) { this.isImported = isImported; return this;}
  }

  public static class VariableDescription extends DescriptionWithId
  {
    public String name;
    public ModuleCodeRepository module;
    public Token.ParameterToken type;
    public boolean isImported;
  }

  public static class ClassDescription extends DescriptionWithId
  {
    private String name;
    private String originalName;
    UnboundType parent;
    List<FunctionDescription> methods = new ArrayList<>();
    List<VariableDescription> variables = new ArrayList<>();
    
    /** Code for class variable declarations */
    StatementContainer variableDeclarationCode = new StatementContainer();

    public boolean isBuiltIn = false;
    public boolean isImported;
    ModuleCodeRepository module;
    public ClassDescription(String name, String originalName)
    {
      this.name = name;
      this.originalName = originalName;
    }
    public List<FunctionDescription> getInstanceMethods()
    {
      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
          .stream().filter(fn -> !fn.sig.isConstructor && !fn.sig.isStatic).collect(Collectors.toList());
    }
//    public List<FunctionDescription> getStaticAndConstructorMethods()
//    {
//      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
//          .stream().filter(fn -> fn.sig.isConstructor || fn.sig.isStatic).collect(Collectors.toList());
//    }
    public List<FunctionDescription> getStaticMethods()
    {
      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
          .stream().filter(fn -> fn.sig.isStatic).collect(Collectors.toList());
    }
    public List<FunctionDescription> getConstructorMethods()
    {
      return getSortedWithIds(methods, Comparator.comparing((FunctionDescription v) -> v.sig.getLookupName()))
          .stream().filter(fn -> fn.sig.isConstructor).collect(Collectors.toList());
    }
    public void addMethod(FunctionDescription f)
    {
      f.id = methods.size();
      methods.add(f);
    }
    public void deleteMethodAndResetIds(int id)
    {
      methods.remove(id);
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
    public int addVarAndResetIds(String name, Token.ParameterToken type)
    {
      // Add a new variable at the beginning of the list so that it's more likely
      // to appear near the top of the variable list
      VariableDescription v = new VariableDescription();
      v.name = name;
      v.type = type;
      variables.add(0, v);
      return 0;
    }
    public void deleteVarAndResetIds(int id)
    {
      variables.remove(id);
    }
    public List<VariableDescription> getAllVars()
    {
      return getSortedWithIds(variables, Comparator.comparing((VariableDescription v) -> v.name));
    }
    public void updateVariable(VariableDescription v)
    {
      variables.set(v.id, v);
    }
    public StatementContainer getVariableDeclarationCode()
    {
      return variableDeclarationCode;
    }
    
    public void setVariableDeclarationCode(StatementContainer code)
    {
      variableDeclarationCode = code;
    }
    public void updateMethod(FunctionDescription m)
    {
      // No change needed currently
    }
    public String getName()
    {
      return name;
    }
    public void setName(String name)
    {
      this.name = name;
    }
    public String getOriginalName()
    {
      return originalName;
    }
    public void setSuperclass(UnboundType parent)
    {
      this.parent = parent;
    }
    public ClassDescription setBuiltIn(boolean isBuiltIn) { this.isBuiltIn = isBuiltIn; return this; }
    public boolean hasNonBuiltInMethods()
    {
      return !methods.stream().allMatch(fn -> fn.sig.isBuiltIn);
    }
    public ClassDescription setImported(boolean isImported) { this.isImported = isImported; return this; }
  }
  
  /**
   * Extra files can also be stored in a project (right now, just used for storing web files,
   * but I suppose other resources could be stored in a project as well later on). For the
   * web version of Plom, these files are stored in memory, but for app versions of Ploms,
   * these files are stored externally on disk, but we just keep a listing of them in memory
   * so that it's easy to show them in the UI. 
   */
  @JsType
  public static class FileDescription 
  {
    /** Use '/' as a path separator? */
    String filePath;
    public boolean isImported;
    public FileDescription setImported(boolean isImported) { this.isImported = isImported; return this;}
    
    public String getPath() { return filePath; }
    public void setPath(String newPath) { filePath = newPath; }
  }
  
  private List <FunctionDescription> functions = new ArrayList<>();
  
  /** Lists global variables and their types */
  List<VariableDescription> globalVars = new ArrayList<>();

  /** All classes */
  List<ClassDescription> classes = new ArrayList<>();
  
  /** Tracks classes that have been deleted so that we can remove their files when the project is saved */
  List<ClassDescription> deletedClasses = new ArrayList<>();
  
  /** Special flag for when this module is being used to develop the standard library */
  public boolean isNoStdLibFlag = false;

  /** Code for global variable declarations */
  StatementContainer variableDeclarationCode = new StatementContainer();
  
  /** Tracks all the extra non-Plom files stored in the module */
  List<FileDescription> extraFiles = new ArrayList<>();
  
  /** Handles operations on extra files */
  ExtraFilesManager fileManager;
  
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
    FunctionDescription fn = getFunctionWithName(name);
    if (fn == null && chainedRepository != null)
      return chainedRepository.getFunctionDescription(name);
    return fn;
  }
  
  public FunctionDescription getFunctionWithName(String name)
  {
    for (FunctionDescription fn: functions)
    {
      if (fn.sig.getLookupName().equals(name)) 
        return fn;
    }
    return null;
  }
  
  public void addFunctionAndResetIds(FunctionDescription func)
  {
    func.module = this;
    functions.add(0, func);
  }
  
  public void deleteFunctionAndResetIds(ModuleCodeRepository module, int id)
  {
    // This only works if only the current module can delete things, and the current module's functions are inserted into the merged function list first 
    if (module != this)
      throw new IllegalArgumentException("Not deleting class from correct chained module");
    functions.remove(id);
  }

  private void fillChainedFunctionNames(List<String> mergedFunctions)
  {
    for (FunctionDescription fn: functions)
      mergedFunctions.add(fn.sig.getLookupName());
  }

  private void fillChainedFunctions(List<FunctionDescription> mergedFunctions)
  {
    for (FunctionDescription fn: functions)
      mergedFunctions.add(fn);
  }

  public List<FunctionDescription> getAllFunctionSorted()
  {
    List<FunctionDescription> toReturn = new ArrayList<>();
    for (FunctionDescription fn: functions)
      toReturn.add(fn);
    if (chainedRepository != null)
      chainedRepository.fillChainedFunctions(toReturn);
    return getSortedWithIds(toReturn, Comparator.comparing(f -> f.sig.getLookupName()));
  }

  public List<String> getAllFunctionNamesSorted()
  {
    List<String> names = new ArrayList<>();
    for (FunctionDescription fn: functions)
      names.add(fn.sig.getLookupName());
    if (chainedRepository != null)
      chainedRepository.fillChainedFunctionNames(names);
    names.sort(Comparator.naturalOrder());
    return names;
  }
  
  public void changeFunctionSignature(FunctionSignature newSig, FunctionDescription oldSig)
  {
//    FunctionDescription func = functions.remove(oldSig.getLookupName());
    if (oldSig.id < functions.size())
      functions.get(oldSig.id).sig = newSig;
//    functions.put(func.sig.getLookupName(), func);
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
  
  public int addGlobalVarAndResetIds(String name, Token.ParameterToken type)
  {
    // Add a new variable at the beginning of the list so that it's more likely
    // to appear near the top of the variable list
    VariableDescription v = new VariableDescription();
    v.name = name;
    v.type = type;
    v.module = this;
    globalVars.add(0, v);
    return 0;
  }
  
  public void updateGlobalVariable(VariableDescription v)
  {
    if (v.id < globalVars.size() && v.module == this)
      globalVars.set(v.id, v);
  }
  
  public void deleteGlobalVarAndResetIds(ModuleCodeRepository module, int id)
  {
    // This only works if only the current module can delete things, and the current module is inserted into the merged global variable list first 
    if (module != this)
      throw new IllegalArgumentException("Not deleting global var from correct chained module");
    globalVars.remove(id);
  }
  
  public StatementContainer getVariableDeclarationCode()
  {
    return variableDeclarationCode;
  }
  
  public void setVariableDeclarationCode(StatementContainer code)
  {
    variableDeclarationCode = code;
  }

  private void fillChainedVariableDeclarationCode(StatementContainer mergedCode)
  {
    if (chainedRepository != null)
      chainedRepository.fillChainedVariableDeclarationCode(mergedCode);
    for (TokenContainer tokens: variableDeclarationCode.statements)
      mergedCode.statements.add(tokens.copy());
  }
  
  public StatementContainer getImportedVariableDeclarationCode()
  {
    StatementContainer mergedCode = new StatementContainer();
    if (chainedRepository != null)
      chainedRepository.fillChainedVariableDeclarationCode(mergedCode);
    return mergedCode;
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
    return getSortedWithIds(mergedClassList, Comparator.comparing((ClassDescription v) -> v.getName()));
  }

  public ClassDescription addClassAndResetIds(String name)
  {
    ClassDescription cls = new ClassDescription(name, name);
    cls.module = this;
    cls.parent = UnboundType.forClassLookupName("object");
    classes.add(0, cls);
    cls.id = 0;
    return cls;
  }
  
  public void deleteClassAndResetIds(ModuleCodeRepository module, int id)
  {
    // This only works if only the current module can delete things, and the current module's classes are inserted into the merged class list first 
    if (module != this)
      throw new IllegalArgumentException("Not deleting class from correct chained module");
    deletedClasses.add(classes.get(id));
    classes.remove(id);
  }
  
  public boolean hasClassWithName(String name)
  {
    for (ClassDescription c: classes)
    {
      if (c.getName().equals(name)) 
        return true;
    }
    return false;
  }
  
  private void fillChainedExtraFiles(List<FileDescription> mergedFiles)
  {
    for (FileDescription f: extraFiles)
      mergedFiles.add(f);
  }

  public List<FileDescription> getAllExtraFilesSorted()
  {
    List<FileDescription> toReturn = new ArrayList<>();
    for (FileDescription f: extraFiles)
      toReturn.add(f);
    if (chainedRepository != null)
      chainedRepository.fillChainedExtraFiles(toReturn);
    toReturn.sort(Comparator.comparing(f -> f.getPath()));
    return toReturn;
  }
  
  public boolean hasExtraFile(String path)
  {
    return extraFiles.stream().anyMatch((file) -> file.getPath().equals(path));
  }

  /** Refresh the internal list of extra files in the module */
  public void refreshExtraFiles(ExtraFilesManager.EmptyCallback callback)
  {
    if (fileManager == null) 
    {
      callback.call();
      return;
    }
    fileManager.getFileList(filenames -> {
      extraFiles.clear();
      for (String name: filenames)
      {
        FileDescription file = new FileDescription();
        file.filePath = name;
        // TODO: handle imported status
        extraFiles.add(file);
      }
      callback.call();
    });

  }
  
  public void setExtraFilesManager(ExtraFilesManager newFileManager)
  {
    fileManager = newFileManager; 
  }
  
  public ExtraFilesManager getExtraFilesManager() { return fileManager; }
  
  /**
   * Marks all the contents of this module as being imported
   */
  public void markAsImported()
  {
    for (ClassDescription c: classes)
      c.setImported(true);
    for (FunctionDescription fn: functions)
      fn.setImported(true);
    for (VariableDescription v: globalVars)
      v.isImported = true;
    for (FileDescription f: extraFiles)
      f.setImported(true);
  }
  
  public void loadBuiltInPrimitives(List<StdLibClass> stdLibClasses, List<StdLibMethod> stdLibMethods)
  {
    Map<String, ClassDescription> classMap = new HashMap<>();
    for (ClassDescription c: classes)
      classMap.put(c.getName(), c);
    for (StdLibClass clsdef: stdLibClasses)
    {
      if (classMap.containsKey(clsdef.name)) continue;
      ClassDescription c = addClassAndResetIds(clsdef.name)
          .setBuiltIn(true);
      classMap.put(clsdef.name, c);
      if (clsdef.parent != null)
        c.setSuperclass(UnboundType.forClassLookupName(clsdef.parent));
      else
        c.setSuperclass(null);
    }
    
    for (StdLibMethod mdef: stdLibMethods)
    {
      ClassDescription c = classMap.get(mdef.className);
      FunctionSignature sig = FunctionSignature.from(UnboundType.forClassLookupName(mdef.returnType), mdef.methodName, mdef.argNames, mdef.argTypes.stream().map(typeStr -> UnboundType.forClassLookupName(typeStr)).collect(Collectors.toList()));
      if (mdef.isStatic)
        sig.setIsStatic(true);
      else if (mdef.isConstructor)
        sig.setIsConstructor(true);
      sig.isBuiltIn = true;
      c.addMethod(new FunctionDescription(sig, mdef.code));
    }
  }
  
  public void saveModule(PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses) throws IOException
  {
    // Save out the module contents
    saveOpenModule(out, saveClasses);
    
    // Close out the module
    out.token("}");
  }
  
  public WebHelpers.Promise<Void> saveModuleWithExtraFiles(final PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses, WebHelpers.PromiseCreator promiseCreator, WebHelpers.Promise.All promiseAll, Function<ArrayBuffer, Uint8Array> bufToUint8Array) throws IOException
  {
    // Save out the module contents
    saveOpenModule(out, saveClasses);

    // Launch promises to get contents of files
    List<String> filePaths = new ArrayList<>();
    ArrayOf<WebHelpers.Promise<ArrayBuffer>> fileContentPromises = Collections.arrayOf();
    for (FileDescription f: extraFiles)
    {
      filePaths.add(f.getPath());
      fileContentPromises.push(promiseCreator.create((resolve, reject) -> {
        fileManager.getFileContents(f.getPath(), contents -> {
          resolve.accept(contents);
        });
      }));
    }
    
    // Wait until all the file contents have been retrieved
    return promiseAll.all(fileContentPromises).thenNow(fileContents -> {
      for (int n = 0; n < filePaths.size(); n++)
      {
        out.token("file");

        out.append(" ");
        out.append("\"");
        out.append(PlomTextWriter.escapeStringLiteral(filePaths.get(n)));
        out.append("\"");

        out.token("{");
        ArrayBuffer buf = fileContents.get(n);
        out.append(WebHelpers.Base64EncoderDecoder.encodeToString(bufToUint8Array.apply(buf), false));
        out.append("}");
        out.newline();
      }
      
      // Close out the module
      out.token("}");

      return null;
    });
    
  }
  
  /** 
   * Saves out a module, but leaves it "open" so that more stuff can
   * be added to the end of the module afterwards
   */
  private void saveOpenModule(PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses) throws IOException
  {
    out.token("module");
    out.token(".{program}");
    out.token("{");
    out.newline();

    // Output stdlib development flag
    if (isNoStdLibFlag)
    {
      out.token("stdlib");
      out.token("{");
      out.token("-1");
      out.token("}");
      out.newline();
    }
    
    // Output global variables
    List<VariableDescription> sortedGlobalVars = new ArrayList<>(globalVars);
    sortedGlobalVars.sort(Comparator.comparing((VariableDescription v) -> v.name));
    for (VariableDescription v: sortedGlobalVars)
    {
      saveVariable(out, v);
    }
    out.token("vardecls");
    out.token("{");
    out.newline();
    PlomTextWriter.writeStatementContainer(out, variableDeclarationCode);
    out.token("}");
    out.newline();
    
    // Output global functions
    List<FunctionDescription> sortedFunctions = new ArrayList<>(functions);
    sortedFunctions.sort(Comparator.comparing(f -> f.sig.getLookupName()));
    for (FunctionDescription fn: sortedFunctions)
    {
      saveFunction(out, fn);
    }
    
    // Output classes
    if (saveClasses)
    {
      List<ClassDescription> sortedClasses = new ArrayList<>(classes);
      sortedClasses.sort(Comparator.comparing((ClassDescription v) -> v.getName()));
      for (ClassDescription cls: sortedClasses)
      {
        if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
          saveClass(out, cls);
      }
    }
  }
  
  public static void saveClass(PlomTextWriter.PlomCodeOutputFormatter out, ClassDescription c) throws IOException
  {
    out.token("class");
    out.token("@");
    out.token("{");
    out.token(c.getName());
    out.token("}");
    if (c.parent != null)
    {
      out.token("extends");
      PlomTextWriter.writeToken(out, c.parent.mainToken);
    }
    
    out.token("{");
    out.newline();

    out.token("vardecls");
    out.token("{");
    out.newline();
    PlomTextWriter.writeStatementContainer(out, c.getVariableDeclarationCode());
    out.token("}");
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

    for (FunctionDescription fn: c.getConstructorMethods())
    {
      if (fn.sig.isBuiltIn) continue;
      saveFunction(out, fn);
    }

    for (FunctionDescription fn: c.getStaticMethods())
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
    if (fn.sig.getNumArgs() == 0)
    {
      out.append(fn.sig.nameParts.get(0));
    }
    else
    {
      for (int n = 0; n < fn.sig.getNumArgs(); n++)
      {
        out.append(fn.sig.nameParts.get(n) + ":");
        out.token("{");
        PlomTextWriter.writeTokenContainer(out, fn.sig.getArgCode(n));
        out.token("}");
      }
    }
    out.token("}");
    
    if (!fn.sig.isConstructor)
    {
      out.token("{");
      PlomTextWriter.writeTokenContainer(out, fn.sig.getReturnTypeCode());
      out.token("}");
    }

    // An extra empty place where I might put comments later on
    out.token("{");
    out.token("}");
    
    out.token("{");
    out.newline();
    PlomTextWriter.writeStatementContainer(out, fn.code);
    out.token("}");
    out.newline();
  }
  
  public WebHelpers.Promise<Void> loadModule(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    lexer.expectToken("module");
    lexer.expectToken(".");
    lexer.expectToken("{");
    String moduleName = lexer.lexParameterTokenPartOrEmpty();
    lexer.expectToken("}");
    
    lexer.expectToken("{");
    lexer.swallowOptionalNewlineToken();
    
    // Loading extra files from a module is done asynchronously, so
    // we use promises to keep track of when loading is done.
    ArrayOf<WebHelpers.Promise<String>> extraFilesPromises = Collections.arrayOf();
    
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
        addFunctionAndResetIds(fn);
      }
      else if ("class".equals(peek))
      {
        loadClassIntoModule(lexer);
      }
      else if ("stdlib".equals(peek))
      {
        loadModuleStdLibFlag(lexer);
      }
      else if ("vardecls".equals(peek))
      {
        lexer.expectToken("vardecls");
        lexer.swallowOptionalNewlineToken();
        lexer.expectToken("{");
        lexer.swallowOptionalNewlineToken();
        StatementContainer code = PlomTextReader.readStatementContainer(lexer);
        variableDeclarationCode = code;
        lexer.expectToken("}");
        lexer.swallowOptionalNewlineToken();
      }
      else if ("file".equals(peek))
      {
        lexer.expectToken("file");
        lexer.swallowOptionalNewlineToken();
        String fileNameString = lexer.lexInput();
        if (!fileNameString.startsWith("\""))
          throw new PlomReadException("Expecting a string for file name", lexer);
        if (!fileNameString.endsWith("\""))
          throw new PlomReadException("Expecting a string for file name", lexer);
        final String filePath = fileNameString.substring(1, fileNameString.length() - 1);
        lexer.swallowOptionalNewlineToken();
        lexer.expectToken("{");
        String fileData = lexer.lexBase64();
        lexer.swallowOptionalNewlineToken();
        lexer.expectToken("}");
        lexer.swallowOptionalNewlineToken();
        final Uint8Array rawData = Base64EncoderDecoder.decodeBase64ToUint8Array(fileData);
        extraFilesPromises.push(WebHelpersShunt.newPromise((resolve, reject) -> {
          getExtraFilesManager().insertFile(filePath, rawData.getBuffer(), () -> {
            resolve.accept(filePath);
          });
        }));
      }
      else
        throw new PlomReadException("Unexpected module contents", lexer);
    }
    
    lexer.expectToken("}");
    
    return WebHelpersShunt.promiseAll(extraFilesPromises).thenNow(doneArray -> null);
  }
  
  public void loadModuleStdLibFlag(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    lexer.expectToken("stdlib");
    lexer.expectToken("{");
    String value = lexer.lexParameterTokenPartOrEmpty();
    lexer.expectToken("}");
    lexer.expectNewlineToken();
    if (!"-1".equals(value))
      throw new PlomReadException("Unexpected module stdlib value", lexer);
    isNoStdLibFlag = true;
  }
  
  public void loadClassIntoModule(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    ClassDescription loaded = loadClass(lexer);
    ClassDescription cls = null;
    boolean augmentClass = false;
    // See if we have a built-in class with the same name. If so,
    // we'll just fill-in the extra defined methods. Otherwise, we'll
    // actually create a new class
    for (ClassDescription c: classes)
    {
      if (c.isBuiltIn && c.getName().equals(loaded.getName()))
      {
        cls = c;
        augmentClass = true;
        break;
      }
    }
    if (cls == null)
      cls = addClassAndResetIds(loaded.getName());
    if (!augmentClass)
    {
      cls.setSuperclass(loaded.parent);
      for (VariableDescription v: loaded.variables)
      {
        cls.addVarAndResetIds(v.name, v.type);
      }
      cls.setVariableDeclarationCode(loaded.getVariableDeclarationCode());
    }
new_methods:
    for (FunctionDescription fn: loaded.methods)
    {
      if (augmentClass)
      {
        // Check if we're replacing an existing method or adding a new one to
        // a built-in class
        for (FunctionDescription m: cls.methods)
        {
          if (m.sig.canReplace(fn.sig))
          {
            m.code = fn.code;
            continue new_methods;
          }
        }
        
      }
      cls.addMethod(fn);
    }
  }
  
  public static ClassDescription loadClass(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    lexer.expectToken("class");
    lexer.expectToken("@");
    lexer.expectToken("{");
    String className = lexer.lexParameterTokenPart();
    if (className == null)
      throw new PlomReadException("Cannot read class name", lexer);
    lexer.expectToken("}");
    
    ClassDescription cls = new ClassDescription(className, className);
    
    if ("extends".equals(lexer.peekLexInput()))
    {
      lexer.expectToken("extends");
      cls.setSuperclass(UnboundType.fromToken(PlomTextReader.readToken(lexer)));
    }
    
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
      else if ("vardecls".equals(peek))
      {
        lexer.expectToken("vardecls");
        lexer.swallowOptionalNewlineToken();
        lexer.expectToken("{");
        lexer.swallowOptionalNewlineToken();
        StatementContainer code = PlomTextReader.readStatementContainer(lexer);
        cls.setVariableDeclarationCode(code);
        lexer.expectToken("}");
        lexer.swallowOptionalNewlineToken();
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
    String varName = lexer.lexParameterTokenPartOrEmpty();
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
    List<TokenContainer> args = new ArrayList<>();
//    List<String> argNames = new ArrayList<>();
//    List<Token.ParameterToken> argTypes = new ArrayList<>();
    String namePart = lexer.lexParameterTokenPart();
    if (namePart == null)
      throw new PlomReadException("Expecting function name", lexer);
    if (namePart.endsWith(":"))
    {
      while (namePart != null)
      {
        fnName += namePart;
        lexer.expectToken("{");
        args.add(PlomTextReader.readTokenContainer(lexer));
        lexer.expectToken("}");
        namePart = lexer.lexParameterTokenPart();
      }
    }
    else
    {
      fnName += namePart;
    }
    lexer.expectToken("}");
    
    TokenContainer returnType;
    if (!isConstructor)
    {
      lexer.expectToken("{");
      returnType = PlomTextReader.readTokenContainer(lexer);
      lexer.expectToken("}");
    }
    else
      returnType = new TokenContainer(Token.ParameterToken.fromContents("@void", Symbol.AtType));

    // Reserved empty section that I might use for comments or other data
    lexer.expectToken("{");
    lexer.expectToken("}");
    
    FunctionSignature sig = FunctionSignature.from(returnType, Arrays.asList(fnName.split(":")), args, null);
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
