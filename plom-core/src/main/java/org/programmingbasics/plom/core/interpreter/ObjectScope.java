package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.interpreter.Value.LValue;

public class ObjectScope extends VariableScope
{
  public ObjectScope(Value thisValue)
  {
    this.thisValue = thisValue;
    if (thisValue.val instanceof PlomObject)
      obj = (PlomObject)thisValue.val;
    type = thisValue.type;
  }
  Value thisValue;
  PlomObject obj;
  Type type;

  /**
   * Looks up the value of "this" i.e. the current object
   */
  public Value lookupThis() throws RunException
  {
    return thisValue;
  }

  public Value lookup(String name) throws RunException
  {
    int slot = type.lookupMemberVariable(name);
    if (slot >= 0)
    {
      return Value.createCopy(obj.slots[slot]);
    }
    else
    {
      if (getParent() != null)
        return getParent().lookup(name);
      throw new RunException("Cannot find value " + name);
    }
  }
  public LValue lookupLValue(String name) throws RunException
  {
    int slot = type.lookupMemberVariable(name);
    if (slot >= 0)
    {
      return LValue.readFromScope(this, name, Value.createCopy(obj.slots[slot]));
    }
    else
    {
      if (getParent() != null)
        return getParent().lookupLValue(name);
      throw new RunException("Cannot find value " + name);
    }
  }
  /** Looks up the type of a variable */
  public Type lookupType(String name)
  {
    Type toReturn = type.lookupMemberVariableType(name);
    if (toReturn == null)
    {
      if (getParent() != null)
        return getParent().lookupType(name);
      return null;
    }
    return toReturn;
  }
  public void lookupSuggestions(String val, List<String> suggestions)
  {
    type.lookupMemberVarSuggestions(val, suggestions);
  }
  
  
  // Overwrites a variable in this scope
  public void assignTo(String name, Value val) throws RunException
  {
    int slot = type.lookupMemberVariable(name);
    if (slot >= 0)
    {
      obj.slots[slot] = val;
    }
    else
    {
      if (getParent() != null)
        getParent().assignTo(name, val);
      else
        throw new RunException();
    }
  }

}
