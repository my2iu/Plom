package org.programmingbasics.plom.core.interpreter;


public class Value
{
  public Type type;
  public Object val;
  // TODO: Remove sourceBinding and sourceScope stuff
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
  
  
  
  /**
   * When doing an assignment into a variable, we don't want to track
   * the value of the variable, but its location, so we'll use a separate
   * data structure and stack for tracking and storing that information.
   */
  public static class LValue
  {
    public Type type;
    public VariableScope sourceScope;
    public String sourceBinding;
    public static LValue readFromScope(VariableScope scope, String binding, Value val)
    {
      LValue newVal = new LValue();
      newVal.sourceScope = scope;
      newVal.sourceBinding = binding;
      newVal.type = val.type;
      return newVal;
    }
  }
}
