package org.programmingbasics.plom.core.interpreter;

import java.util.HashMap;
import java.util.Map;

import org.programmingbasics.plom.core.interpreter.Value.LValue;

/**
 * Tracks the variables that exist in a certain scope.
 */
public class VariableScope
{
  public Map<String, Value> values = new HashMap<>();
  
  public Value lookup(String name) throws RunException
  {
    Value val = values.get(name);
    if (val == null) throw new RunException();
    return val;
  }
  public LValue lookupLValue(String name) throws RunException
  {
    Value val = values.get(name);
    if (val == null) throw new RunException();
    return LValue.readFromScope(this, name, val);
  }

  // Overwrites a variable in this scope
  public void assignTo(String name, Value val) throws RunException
  {
    if (!values.containsKey(name))
      throw new RunException();
    values.put(name, val);
  }
  // For testing
  public VariableScope addVariable(String name, Value val)
  {
    values.put(name, val);
    return this;
  }
}
