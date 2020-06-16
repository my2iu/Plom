package org.programmingbasics.plom.core.interpreter;

import java.util.HashMap;
import java.util.Map;

import org.programmingbasics.plom.core.interpreter.Value.LValue;

/**
 * Tracks the variables that exist in a certain scope.
 */
public class VariableScope
{
  private Map<String, Value> values = new HashMap<>();
  private Map<String, Type> types = new HashMap<>();
  private VariableScope parent;
  
  public void setParent(VariableScope parentScope)
  {
    parent = parentScope;
  }
  public VariableScope getParent()
  {
    return parent;
  }
  public Value lookup(String name) throws RunException
  {
    Value val = values.get(name);
    if (val == null)
    {
      if (parent != null)
        return parent.lookup(name);
      throw new RunException();
    }
    return val;
  }
  public LValue lookupLValue(String name) throws RunException
  {
    Value val = values.get(name);
    if (val == null) 
    {
      if (parent != null)
        return parent.lookupLValue(name);
      throw new RunException();
    }
    return LValue.readFromScope(this, name, val);
  }

  // Overwrites a variable in this scope
  public void assignTo(String name, Value val) throws RunException
  {
    if (!values.containsKey(name))
    {
      if (parent != null)
        parent.assignTo(name, val);
      else
        throw new RunException();
    }
    values.put(name, val);
  }
  // For testing
  public VariableScope addVariable(String name, Type type, Value val)
  {
    values.put(name, val);
    types.put(name, type);
    return this;
  }
}
