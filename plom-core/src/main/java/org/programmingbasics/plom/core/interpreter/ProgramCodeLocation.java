package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.CodePosition;

/**
 * Used to refer to different code locations in a program in log 
 * messages.
 */
public class ProgramCodeLocation
{

  /** Name of a class */
  String className;
  
  /** Name of a function (if no class is specified) or method (if a 
   * class is specified) */
  String functionMethodName;
  
  /** If a class and method is specified, this lists whether the method 
   * if static */
  boolean isStatic;
  
  /** Position within a function/method */
  CodePosition position;
  
  public ProgramCodeLocation(String className, String functionMethodName, boolean isStatic, CodePosition pos)
  {
    this.className = className;
    this.functionMethodName = functionMethodName;
    this.isStatic = isStatic;
    this.position = pos == null ? null : pos.clone();
  }

  public static ProgramCodeLocation forFunction(String name)
  {
    return new ProgramCodeLocation(null, name, true, null);
  }

  public static ProgramCodeLocation forFunction(String name, CodePosition pos)
  {
    return new ProgramCodeLocation(null, name, true, pos);
  }

  public static ProgramCodeLocation forClass(String name)
  {
    return new ProgramCodeLocation(name, null, true, null);
  }

  public static ProgramCodeLocation forMethod(String className, String methodName, boolean isStatic)
  {
    return new ProgramCodeLocation(className, methodName, isStatic, null);
  }

  public static ProgramCodeLocation forMethod(String className, String methodName, boolean isStatic, CodePosition pos)
  {
    return new ProgramCodeLocation(className, methodName, isStatic, pos);
  }

  public static ProgramCodeLocation fromCodeUnit(CodeUnitLocation codeUnit, CodePosition pos)
  {
    return new ProgramCodeLocation(codeUnit.className, codeUnit.functionName, codeUnit.isConstructor || codeUnit.isStatic, pos);
  }
  
  public String getClassName()
  {
    return className;
  }

  public String getFunctionMethodName()
  {
    return functionMethodName;
  }

  public boolean isStatic()
  {
    return isStatic;
  }

  public CodePosition getPosition()
  {
    return position;
  }
}
