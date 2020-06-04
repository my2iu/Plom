package org.programmingbasics.plom.core.interpreter;

public class Type
{
  public String name;
  public Type(String name)
  {
    this.name = name;
  }
  static Type NUMBER = new Type(".number");
  static Type STRING = new Type(".string");
}
