package org.programmingbasics.plom.core.interpreter;

/**
 * Describes the location of a function or method (used for looking
 * up primitives and for displaying debug information in exceptions
 * etc)
 */
public class CodeUnitLocation
{
  public String functionName;
  
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((functionName == null) ? 0 : functionName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CodeUnitLocation other = (CodeUnitLocation) obj;
    if (functionName == null)
    {
      if (other.functionName != null) return false;
    }
    else if (!functionName.equals(other.functionName)) return false;
    return true;
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
