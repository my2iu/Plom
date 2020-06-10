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
  public boolean isFunction()
  {
    return "Function".equals(name);
  }
  static Type NUMBER = new Type(".number");
  static Type STRING = new Type(".string");
  static Type BOOLEAN = new Type(".boolean");
}
