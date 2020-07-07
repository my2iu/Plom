package org.programmingbasics.plom.core.interpreter;

import java.util.HashMap;
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
  
  Map<String, PrimitiveFunction.PrimitiveMethod> methods = new HashMap<>();
  Map<String, Type> methodTypeSigs = new HashMap<>();
  public void addPrimitiveMethod(String name, PrimitiveFunction.PrimitiveMethod fn, Type returnType, Type...args)
  {
    methods.put(name, fn);
    methodTypeSigs.put(name, makePrimitiveMethodType(returnType, args));
  }
  public PrimitiveFunction.PrimitiveMethod lookupPrimitiveMethod(String name)
  {
    return methods.get(name);
  }
  
  static Type makeFunctionType(Type returnType, Type...args)
  {
    Type t = new Type("Function");
    return t;
  }
  static Type makePrimitiveFunctionType(Type returnType, Type...args)
  {
    Type t = new Type("PrimitiveFunction");
    return t;
  }
  static Type makePrimitiveBlockingFunctionType(Type returnType, Type...args)
  {
    Type t = new Type("PrimitiveBlockingFunction");
    return t;
  }
  static Type makePrimitiveMethodType(Type returnType, Type...args)
  {
    Type t = new Type("PrimitiveMethod");
    return t;
  }
  public boolean isFunction()
  {
    return "Function".equals(name) || "PrimitiveFunction".equals(name) || "PrimitiveBlockingFunction".equals(name);
  }
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
}
