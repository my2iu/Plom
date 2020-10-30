package org.programmingbasics.plom.core.interpreter;


public class Value
{
  public Type type;
  public Object val;
  public Value() {}
  public double getNumberValue()
  {
    return ((Double)val).doubleValue();
  }
  public String getStringValue()
  {
    return (String)val;
  }
  public boolean getBooleanValue()
  {
    return ((Boolean)val).booleanValue();
  }
  public void copyFrom(Value v)
  {
    val = v.val;
    type = v.type;
  }
  public static Value createNumberValue(CoreTypeLibrary coreTypes, double val)
  {
    Value newVal = new Value();
    newVal.type = coreTypes.getNumberType();
    newVal.val = val;
    return newVal;
  }
  public static Value createStringValue(CoreTypeLibrary coreTypes, String val)
  {
    Value newVal = new Value();
    newVal.type = coreTypes.getStringType();
    newVal.val = val;
    return newVal;
  }
  public static Value createBooleanValue(CoreTypeLibrary coreTypes, boolean val)
  {
    return val ? coreTypes.getTrueValue() : coreTypes.getFalseValue();
  }
  /** void isn't really a value, but just to be consistent over all function calls, void functions will still return a value */
  public static Value createVoidValue(CoreTypeLibrary coreTypes)
  {
    Value newVal = new Value();
    newVal.type = coreTypes.getVoidType();
    newVal.val = null;
    return newVal;
  }
  public static Value create(Object val, Type type)
  {
    Value newVal = new Value();
    newVal.type = type;
    newVal.val = val;
    return newVal;
  }
  public static Value createEmptyObject(CoreTypeLibrary coreTypes, Type type)
  {
    Value newVal = new Value();
    newVal.type = type;
    newVal.val = new PlomObject(coreTypes, type.numValueSlots);
    return newVal;
  }
  public static Value createCopy(Value v)
  {
    Value newVal = new Value();
    newVal.copyFrom(v);
    return newVal;
  }
  public static Value readFromScope(VariableScope scope, String binding, Value val)
  {
    Value newVal = new Value();
    newVal.type = val.type;
    newVal.val = val.val;
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
