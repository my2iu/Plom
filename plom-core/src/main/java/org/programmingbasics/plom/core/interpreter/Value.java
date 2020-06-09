package org.programmingbasics.plom.core.interpreter;

public class Value
{
  public Type type;
  public Object val;
  public VariableScope sourceScope;
  public String sourceBinding;
  public double getNumberValue()
  {
    return ((Double)val).doubleValue();
  }
  public String getStringValue()
  {
    return (String)val;
  }
  public static Value createNumberValue(double val)
  {
    Value newVal = new Value();
    newVal.type = Type.NUMBER;
    newVal.val = val;
    return newVal;
  }
  public static Value createStringValue(String val)
  {
    Value newVal = new Value();
    newVal.type = Type.STRING;
    newVal.val = val;
    return newVal;
  }
  public static Value readFromScope(VariableScope scope, String binding, Value val)
  {
    Value newVal = new Value();
    newVal.type = val.type;
    newVal.val = val.val;
    newVal.sourceScope = scope;
    newVal.sourceBinding = binding;
    return newVal;
  }
}
