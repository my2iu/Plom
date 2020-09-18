package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
      return from(returnType, Arrays.asList(name.split(":")), new ArrayList<>(), new ArrayList<>());
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name), Arrays.asList(arg1Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name), Arrays.asList(arg1Type, arg2Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, String arg1Name, Token.ParameterToken arg1Type, String arg2Name, Token.ParameterToken arg2Type, String arg3Name, Token.ParameterToken arg3Type)
    {
      return from(returnType, Arrays.asList(name.split(":")), Arrays.asList(arg1Name, arg2Name, arg3Name), Arrays.asList(arg1Type, arg2Type, arg3Type));
    }
    public static FunctionSignature from(Token.ParameterToken returnType, String name, List<String> argNames, List<Token.ParameterToken> argTypes)
    {
      return from(returnType, Arrays.asList(name.split(":")), argNames, argTypes);
    }
    public static FunctionSignature from(Token.ParameterToken returnType, List<String> nameParts, List<String> argNames, List<Token.ParameterToken> argTypes)
    {
      FunctionSignature sig = new FunctionSignature();
      sig.returnType = returnType;
      sig.nameParts = nameParts;
      sig.argNames = argNames;
      sig.argTypes = argTypes;
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
  }

  public static class VariableDescription extends DescriptionWithId
  {
    public String name;
    public Token.ParameterToken type;
  }

  public static class ClassDescription extends DescriptionWithId
  {
    public String name;
    List<FunctionDescription> methods = new ArrayList<>();
    List<VariableDescription> variables = new ArrayList<>();
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
  }
  
  private Map<String, FunctionDescription> functions = new HashMap<>();
  
  /** Lists global variables and their types */
  List<VariableDescription> globalVars = new ArrayList<>();

  /** All classes */
  List<ClassDescription> classes = new ArrayList<>();
  
  public ModuleCodeRepository()
  {
    // Create a basic main function that can be filled in
    FunctionDescription func = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("var", Symbol.Var),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":", Symbol.Colon),
                Token.ParameterToken.fromContents("@string", Symbol.AtType)
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
                ),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("=", Symbol.Eq),
                        new Token.SimpleToken("\"8\"", Symbol.String)
                        ),
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
                                    ))
                            ))
                    ),
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
                                    ))
                            ))
                    )
                )
            )
        );
    functions.put(func.sig.getLookupName(), func);
    
    FunctionDescription testParamFunc = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), Arrays.asList("test"), Arrays.asList("arg1"), Arrays.asList(Token.ParameterToken.fromContents("@number", Symbol.AtType))),
        new StatementContainer());
    functions.put(testParamFunc.sig.getLookupName(), testParamFunc);
    
    FunctionDescription printStringPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print string:", "value", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Prints a string to the screen", Symbol.DUMMY_COMMENT),
                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
            ));
    functions.put(printStringPrimitive.sig.getLookupName(), printStringPrimitive);

    FunctionDescription printPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print:", "value", Token.ParameterToken.fromContents("@object", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Prints a value to the screen", Symbol.DUMMY_COMMENT),
                Token.ParameterToken.fromContents(".print string:", Symbol.DotVariable, 
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".value", Symbol.DotVariable),
                        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable))
                    )
            )));
    functions.put(printPrimitive.sig.getLookupName(), printPrimitive);

    FunctionDescription inputPrimitive = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@string", Symbol.AtType), "input:", "prompt", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
        new StatementContainer(
            new TokenContainer(
                new Token.WideToken("// Displays a prompt asking for input and returns the value entered by the user", Symbol.DUMMY_COMMENT),
                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
            ));
    functions.put(inputPrimitive.sig.getLookupName(), inputPrimitive);

    addGlobalVarAndResetIds("var", Token.ParameterToken.fromContents("@object", Symbol.AtType));
    
    addClassAndResetIds("Test");
  }
  
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
    return functions.get(name);
  }
  
  public boolean hasFunctionWithName(String name)
  {
    return functions.containsKey(name);
  }
  
  public void addFunction(FunctionDescription func)
  {
    functions.put(func.sig.getLookupName(), func);
  }
  
  public List<String> getAllFunctions()
  {
    List<String> names = new ArrayList<>(functions.keySet());
    names.sort(Comparator.naturalOrder());
    return names;
  }
  
  public void changeFunctionSignature(FunctionSignature newSig, FunctionSignature oldSig)
  {
    FunctionDescription func = functions.remove(oldSig.getLookupName());
    func.sig = newSig;
    functions.put(func.sig.getLookupName(), func);
  }
  
  public List<VariableDescription> getAllGlobalVars()
  {
    return getSortedWithIds(globalVars, Comparator.comparing((VariableDescription v) -> v.name));
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
  
  public List<ClassDescription> getAllClasses()
  {
    return getSortedWithIds(classes, Comparator.comparing((ClassDescription v) -> v.name));
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

  
  
  public void loadBuiltInPrimitives(List<StdLibClass> stdLibClasses, List<StdLibMethod> stdLibMethods)
  {
    Map<String, ClassDescription> classMap = new HashMap<>();
    for (ClassDescription c: classes)
      classMap.put(c.name, c);
    for (StdLibClass clsdef: stdLibClasses)
    {
      if (classMap.containsKey(clsdef.name)) continue;
      classMap.put(clsdef.name, addClassAndResetIds(clsdef.name));
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
}
