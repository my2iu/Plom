package org.programmingbasics.plom.core.interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the variables that exist in a certain scope.
 */
public class VariableScope
{
  public Map<String, Value> values = new HashMap<>();
  
  public Value lookup(String name)
  {
    return values.get(name);
  }
  public VariableScope addVariable(String name, Value val)
  {
    values.put(name, val);
    return this;
  }
}
