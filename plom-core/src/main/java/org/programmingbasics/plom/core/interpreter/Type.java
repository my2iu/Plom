package org.programmingbasics.plom.core.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    if (parent != null)
      numValueSlots = parent.numValueSlots;
    else
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
  public void lookupMethodSuggestions(String val, List<String> suggestions)
  {
    for (String memberName: methodTypeSigs.keySet())
    {
      if (!suggestions.contains(memberName))
        suggestions.add(memberName);
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
  public void lookupStaticMemberSuggestions(String val, List<String> suggestions)
  {
    for (String memberName: staticMethodTypeSigs.keySet())
    {
      if (!suggestions.contains(memberName))
        suggestions.add(memberName);
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
  public void lookupMemberVarSuggestions(String name, List<String> suggestions)
  {
    for (String memberName: memberVarSlots.keySet())
    {
      if (!suggestions.contains(memberName))
        suggestions.add(memberName);
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
}
