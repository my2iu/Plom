package org.programmingbasics.plom.core.interpreter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

import jsinterop.annotations.JsType;

/**
 * Tracks the variables that exist in a certain scope. A scope holds the 
 * bindings of names to values where variables can be looked up.
 */
@JsType
public class VariableScope
{
  private Map<String, Value> values = new HashMap<>();
  private Map<String, Type> types = new HashMap<>();
  private VariableScope parent;
//  private Value thisValue;
  
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
      throw new RunException("Cannot find value " + name);
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
      throw new RunException("Cannot find value " + name);
    }
    return LValue.readFromScope(this, name, val);
  }
  /** Looks up the type of a variable */
  public Type lookupType(String name)
  {
    Type type = types.get(name);
    if (type == null)
    {
      if (parent != null)
        return parent.lookupType(name);
      return null;
    }
    return type;
  }
  public void lookupSuggestions(GatheredSuggestions suggestions)
  {
    for (String name: values.keySet())
    {
      suggestions.addSuggestion(name);
    }
  }
  /**
   * Returns the Type object used to represent a type within this 
   * interpreter for the given textual Type description
   */
  public Type typeFromToken(Token typeToken) throws RunException
  {
    if (parent != null)
      return parent.typeFromToken(typeToken);
    if (typeToken instanceof Token.ParameterToken)
      throw new RunException("Unknown type " + ((Token.ParameterToken)typeToken).getLookupName());
    throw new RunException();
  }

  /**
   * Returns all the types available at this point in the code (including
   * generic parameter types). This is an expensive operation and should
   * only be used for type suggestions when coding.
   */
  public List<Type> getAllKnownTypes()
  {
    if (parent != null)
      return parent.getAllKnownTypes();
    return Collections.emptyList();
  }
  
  /**
   * Looks up the value of "this" i.e. the current object
   */
  public Value lookupThis() throws RunException
  {
//    Value val = thisValue;
//    if (val == null)
//    {
      if (parent != null)
        return parent.lookupThis();
      throw new RunException("Cannot find a this value");
//    }
//    return val;
  }
//  public void setThis(Value thisValue)
//  {
//    this.thisValue = thisValue;
//  }
  
  /**
   * In constructors, we may need to overwrite the "this" value 
   * so that it can later be returned by the constructor
   */
  public void overwriteThis(Value thisValue) throws RunException
  {
    if (parent != null)
    {
      parent.overwriteThis(thisValue);
      return;
    }
    throw new RunException("Cannot find a this value");
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
