package org.programmingbasics.plom.core.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Type
{
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Type other = (Type) obj;
    if (name == null)
    {
      if (other.name != null) return false;
    }
    else if (!name.equals(other.name)) return false;
    return true;
  }
  public String name;
  public Type(String name)
  {
    this.name = name;
    numValueSlots = 0;
  }
  public Type(String name, Type parent)
  {
    this.name = name;
    this.parent = parent;
//    if (parent != null)
//      numValueSlots = parent.numValueSlots;
//    else
      numValueSlots = 0;
  }
  public Type parent;
  int numValueSlots;
  
  private Map<String, PrimitiveFunction.PrimitiveMethod> primMethods = new HashMap<>();
  private Map<String, ExecutableFunction> methods = new HashMap<>();
  private Map<String, TypeSignature> methodTypeSigs = new HashMap<>();
  private Map<String, ExecutableFunction> staticMethods = new HashMap<>();
  private Map<String, TypeSignature> staticMethodTypeSigs = new HashMap<>();
  private Map<String, Integer> memberVarSlots = new HashMap<>();
  private Map<String, Type> memberVarsTypes = new HashMap<>();
  public void addPrimitiveMethod(String name, PrimitiveFunction.PrimitiveMethod fn, Type returnType, Type...args)
  {
    primMethods.put(name, fn);
    methodTypeSigs.put(name, makePrimitiveMethodType(returnType, args));
  }
  public PrimitiveFunction.PrimitiveMethod lookupPrimitiveMethod(String name)
  {
    PrimitiveFunction.PrimitiveMethod m = null;
    for (Type type = this; m == null && type != null; type = type.parent)
      m = type.primMethods.get(name);
    return m;
  }
  public void addMethod(String name, ExecutableFunction fn, Type returnType, Type...args)
  {
    methods.put(name, fn);
    methodTypeSigs.put(name, makeFunctionType(returnType, args));
  }
  public ExecutableFunction lookupMethod(String name)
  {
    ExecutableFunction m = null;
    for (Type type = this; m == null && type != null; type = type.parent)
      m = type.methods.get(name);
    return m;
  }
  public TypeSignature lookupMethodSignature(String name)
  {
    TypeSignature sig = null;
    for (Type type = this; sig == null && type != null; type = type.parent)
      sig = type.methodTypeSigs.get(name);
    return sig;
  }
  public void lookupMethodSuggestions(GatheredSuggestions suggestions)
  {
    for (String memberName: methodTypeSigs.keySet())
    {
      suggestions.addSuggestion(memberName);
    }
  }
  public void addStaticMethod(String name, ExecutableFunction fn, Type returnType, Type...args)
  {
    staticMethods.put(name, fn);
    staticMethodTypeSigs.put(name, makeFunctionType(returnType, args));
  }
  public ExecutableFunction lookupStaticMethod(String name)
  {
    return staticMethods.get(name);
  }
  public TypeSignature lookupStaticMethodSignature(String name)
  {
    return staticMethodTypeSigs.get(name);
  }
  public void lookupStaticMemberSuggestions(GatheredSuggestions suggestions, boolean includeNonConstructors, boolean includeConstructors)
  {
    for (String memberName: staticMethodTypeSigs.keySet())
    {
      if (staticMethods.get(memberName).codeUnit.isConstructor ? !includeConstructors : !includeNonConstructors)
        continue;
      suggestions.addSuggestion(memberName);
    }
  }
  public void addMemberVariable(String varName, Type type)
  {
    memberVarSlots.put(varName, numValueSlots);
    memberVarsTypes.put(varName, type);
    numValueSlots++;
  }
  public int lookupMemberVariable(String varName)
  {
    return memberVarSlots.getOrDefault(varName, -1);
  }
  public Type lookupMemberVariableType(String varName)
  {
    return memberVarsTypes.get(varName);
  }
  public void lookupMemberVarSuggestions(GatheredSuggestions suggestions)
  {
    for (String memberName: memberVarSlots.keySet())
    {
      suggestions.addSuggestion(memberName);
    }
  }
  
  public static TypeSignature makeFunctionType(Type returnType, Type...args)
  {
    TypeSignature t = new TypeSignature("Function", returnType, args);
    return t;
  }
  static TypeSignature makePrimitiveFunctionType(Type returnType, Type...args)
  {
    TypeSignature t = new TypeSignature("PrimitiveFunction", returnType, args);
    return t;
  }
  static TypeSignature makePrimitiveBlockingFunctionType(Type returnType, Type...args)
  {
    TypeSignature t = new TypeSignature("PrimitiveBlockingFunction", returnType, args);
    return t;
  }
  static TypeSignature makePrimitiveMethodType(Type returnType, Type...args)
  {
    TypeSignature t = new TypeSignature("PrimitiveMethod", returnType, args);
    return t;
  }
  public boolean isCallable() { return false; }
  public boolean isPrimitiveNonBlockingFunction()
  {
    return "PrimitiveFunction".equals(name);
  }
  public boolean isPrimitiveBlockingFunction()
  {
    return "PrimitiveBlockingFunction".equals(name);
  }
  public boolean isMethod()
  {
    return "PrimitiveMethod".equals(name);
  }
  public boolean isPrimitiveMethod()
  {
    return "PrimitiveMethod".equals(name);
  }
  public boolean isNormalFunction()
  {
    return "Function".equals(name);
  }
  
  public boolean isInstanceOf(Type otherType)
  {
    if (this == otherType) return true;
    if (parent == null) return false;
    return parent.isInstanceOf(otherType);
  }
  
  public static class TypeSignature extends Type
  {
    public Type returnType;
    public List<Type> args;
    public TypeSignature(String name, Type returnType, Type...args)
    {
      super(name);
      this.returnType = returnType;
      this.args = Arrays.asList(args);
    }
    public boolean isCallable() { return true; }
  }
  
  /**
   * Function types are a little weird in that they aren't unique. The 
   * same function type might have different Type objects that refer
   * to it. Also, the name and arg names are ignored for checking
   * for equivalence maybe?
   */
  public static class LambdaFunctionType extends Type
  {
    public Type returnType;
    public List<Type> args;
    public List<String> optionalArgNames;
    public LambdaFunctionType(String name, Type returnType, List<String> optionalArgNames, List<Type> args)
    {
      super(name);
      this.returnType = returnType;
      this.args = args;
      this.optionalArgNames = optionalArgNames;
    }
    @Override
    public void lookupMethodSuggestions(GatheredSuggestions suggestions)
    {
      suggestions.addSuggestion(name);
    }
    @Override 
    public TypeSignature lookupMethodSignature(String funName)
    {
      TypeSignature sig = null;
      if (name.equals(funName))
      {
        sig = new TypeSignature(name, returnType, args.toArray(new Type[0]));
      }
      return sig;
    }
    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Objects.hash(args, returnType);
      return result;
    }
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      LambdaFunctionType other = (LambdaFunctionType) obj;
      return Objects.equals(args, other.args)
          && Objects.equals(returnType, other.returnType);
    }
  }
}
