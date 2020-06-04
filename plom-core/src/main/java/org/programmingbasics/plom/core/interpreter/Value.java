package org.programmingbasics.plom.core.interpreter;

public class Value
{
  public Type type;
  public Object val;
  public double getNumberValue()
  {
    return ((Double)val).doubleValue();
  }
  public String getStringValue()
  {
    return (String)val;
  }
}
