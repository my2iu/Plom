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
  }
  public Type(String name, Type parent)
  {
    this.name = name;
    this.parent = parent;
  }
  public Type parent;
  
  private Map<String, PrimitiveFunction.PrimitiveMethod> methods = new HashMap<>();
  private Map<String, TypeSignature> methodTypeSigs = new HashMap<>();
  public void addPrimitiveMethod(String name, PrimitiveFunction.PrimitiveMethod fn, Type returnType, Type...args)
  {
    methods.put(name, fn);
    methodTypeSigs.put(name, makePrimitiveMethodType(returnType, args));
  }
  public PrimitiveFunction.PrimitiveMethod lookupPrimitiveMethod(String name)
  {
    PrimitiveFunction.PrimitiveMethod m = null;
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
  public void lookupMemberSuggestions(String val, List<String> suggestions)
  {
    for (String memberName: methodTypeSigs.keySet())
    {
      if (!suggestions.contains(memberName))
        suggestions.add(memberName);
    }
  }
  
  static TypeSignature makeFunctionType(Type returnType, Type...args)
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
