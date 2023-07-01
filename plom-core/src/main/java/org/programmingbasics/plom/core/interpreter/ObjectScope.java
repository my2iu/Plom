package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.interpreter.Value.LValue;

public class ObjectScope extends VariableScope
{
  public ObjectScope(Value thisValue)
  {
    this.thisValue = thisValue;
    // For constructors, we create an empty object scope with no
    // "this" value and then set it later in the constructor
    if (thisValue != null)
    {
      if (thisValue.val instanceof PlomObject)
        obj = (PlomObject)thisValue.val;
      type = thisValue.type;
    }
  }
  Value thisValue;
  PlomObject obj;
  Type type;

  /**
   * In a constructor, the object is not constructed until the 
   * supertype constructor is called. But for error checking, we
   * still want to check for accesses to member variables before
   * the object is fully constructed, so we just need the type
   * for that (it shouldn't be the type of the final constructed
   * type, just the type holding the constructor)
   */
  public static ObjectScope placeHolderForConstructor(Type type)
  {
    ObjectScope obj = new ObjectScope(null);
    obj.type = type;
    return obj;
  }
  
  /**
   * Looks up the value of "this" i.e. the current object
   */
  @Override public Value lookupThis() throws RunException
  {
    return thisValue;
  }

  @Override public Value lookupThisOrNull()
  {
    return thisValue;
  }

  
  @Override public void overwriteThis(Value thisValue) throws RunException
  {
    this.thisValue = thisValue;
    if (thisValue.val instanceof PlomObject)
      obj = (PlomObject)thisValue.val;
    type = thisValue.type;
  }

  public Value lookup(String name) throws RunException
  {
    if (type != null)
    {
      int slot = type.lookupMemberVariable(name);
      if (slot >= 0)
      {
        return Value.createCopy(obj.slots[slot]);
      }
    }
    if (getParent() != null)
      return getParent().lookup(name);
    throw new RunException("Cannot find value " + name);
  }
  public LValue lookupLValue(String name) throws RunException
  {
    if (type != null)
    {
      int slot = type.lookupMemberVariable(name);
      if (slot >= 0)
      {
        return LValue.readFromScope(this, name, Value.createCopy(obj.slots[slot]));
      }
    }
    if (getParent() != null)
      return getParent().lookupLValue(name);
    throw new RunException("Cannot find value " + name);
  }
  /** Looks up the type of a variable */
  public Type lookupType(String name)
  {
    Type toReturn = type != null ? type.lookupMemberVariableType(name) : null;
    if (toReturn == null)
    {
      if (getParent() != null)
        return getParent().lookupType(name);
      return null;
    }
    return toReturn;
  }
  @Override
  public void lookupSuggestions(GatheredSuggestions suggestions)
  {
    if (type != null)
      type.lookupMemberVarSuggestions(suggestions);
  }
  
  
  // Overwrites a variable in this scope
  public void assignTo(String name, Value val) throws RunException
  {
    if (type != null)
    {
      int slot = type.lookupMemberVariable(name);
      if (slot >= 0)
      {
        obj.slots[slot] = val;
      }
      return;
    }
    if (getParent() != null)
      getParent().assignTo(name, val);
    else
      throw new RunException();
  }

}
