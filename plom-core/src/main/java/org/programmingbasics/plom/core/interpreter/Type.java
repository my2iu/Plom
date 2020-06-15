package org.programmingbasics.plom.core.interpreter;

public class Type
{
  public String name;
  public Type(String name)
  {
    this.name = name;
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
  static Type NUMBER = new Type(".number");
  static Type STRING = new Type(".string");
  static Type BOOLEAN = new Type(".boolean");
  static Type VOID = new Type(".void");
  static Type NULL = new Type(".null");
}
