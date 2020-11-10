package org.programmingbasics.plom.core.interpreter;

import jsinterop.annotations.JsType;

/**
 * Describes the location of a function or method (used for looking
 * up primitives and for displaying debug information in exceptions
 * etc)
 */
@JsType
public class CodeUnitLocation
{
  public String className;
  public String functionName;
  public boolean isStatic;
  public boolean isConstructor;
  
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((className == null) ? 0 : className.hashCode());
    result = prime * result
        + ((functionName == null) ? 0 : functionName.hashCode());
    result = prime * result + (isConstructor ? 1231 : 1237);
    result = prime * result + (isStatic ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CodeUnitLocation other = (CodeUnitLocation) obj;
    if (className == null)
    {
      if (other.className != null) return false;
    }
    else if (!className.equals(other.className)) return false;
    if (functionName == null)
    {
      if (other.functionName != null) return false;
    }
    else if (!functionName.equals(other.functionName)) return false;
    if (isConstructor != other.isConstructor) return false;
    if (isStatic != other.isStatic) return false;
    return true;
  }

  public static CodeUnitLocation forMethod(String className, String functionName)
  {
    CodeUnitLocation toReturn = new CodeUnitLocation();
    toReturn.className = className;
    toReturn.functionName = functionName;
    return toReturn;
  }

  public static CodeUnitLocation forStaticMethod(String className, String functionName)
  {
    CodeUnitLocation toReturn = new CodeUnitLocation();
    toReturn.isStatic = true;
    toReturn.className = className;
    toReturn.functionName = functionName;
    return toReturn;
  }

  public static CodeUnitLocation forConstructorMethod(String className, String functionName)
  {
    CodeUnitLocation toReturn = new CodeUnitLocation();
    toReturn.isConstructor = true;
    toReturn.className = className;
    toReturn.functionName = functionName;
    return toReturn;
  }

  public static CodeUnitLocation forFunction(String functionName)
  {
    CodeUnitLocation toReturn = new CodeUnitLocation();
    toReturn.functionName = functionName;
    return toReturn;
  }

  public static CodeUnitLocation forUnknown()
  {
    CodeUnitLocation toReturn = new CodeUnitLocation();
    return toReturn;
  }
}
