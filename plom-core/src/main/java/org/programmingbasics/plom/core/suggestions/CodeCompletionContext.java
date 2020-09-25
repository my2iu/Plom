package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ObjectScope;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.interpreter.VariableScope;

/**
 * When doing code completion, we need to gather up context information
 * about what types and variables are available, and we store that
 * information here.
 */
public class CodeCompletionContext
{
  List<Type> knownTypes;
  VariableScope globalScope = new VariableScope();
  VariableScope topScope = globalScope;
  List<Type> typeStack = new ArrayList<>();
  Type lastTypeUsed;
  Type lastTypeForStaticCall;
  
  /**
   * Used internally to lookup types and do type checking when computing 
   * which types are used (returns a type object for a type with a given name).
   */
//  public Type lookupType(String name)
//  {
//    switch (name) 
//    {
//    case "number": return coreTypes().getNumberType();
//    case "string": return coreTypes().getStringType();
//    case "boolean": return coreTypes().getBooleanType();
//    case "object": return coreTypes().getObjectType();
//    default: return new Type(name);
//    }
//  }
  
  public Type getLastTypeUsed()
  {
    return lastTypeUsed;
  }
  public void clearLastTypeUsed()
  {
    lastTypeUsed = null;
  }
  public void setLastTypeUsed(Type val)
  {
    lastTypeUsed = val;
  }
  
  // This is used in a bit of a weird way. The value is not properly tracked over everywhere
  // in the code. It's only used for static method calls, so we specifically check for when
  // we need to find types for static method calls, and then when executing the code, we look
  // for an incomplete static call, and set the type that the static call was on there.
  public void setLastTypeForStaticCall(Type val)
  {
    lastTypeForStaticCall = val;
  }
  public Type getLastTypeForStaticCall()
  {
    return lastTypeForStaticCall;
  }
  
  
  // Eventually, I'll need to recreate expression evaluation in the code completer, but
  // this would mainly be for handling types for brackets. We mainly need the type for
  // doing predictions of the . (member) operator. Handling predictions of other symbols
  // is done through the grammar. The . (member) operator only needs to know the type of
  // an expression if it appears in a bracket because otherwise operator precedence means
  // that the . (member) operator will only apply to the most immediately preceding item.
  public void pushType(Type type)
  {
    typeStack.add(type);
  }
  public Type popType()
  {
    return typeStack.remove(typeStack.size() - 1);
  }
  public void pushNewScope()
  {
    VariableScope scope = new VariableScope();
    scope.setParent(topScope);
    topScope = scope;
  }
  public void pushObjectScope(Value thisValue)
  {
    VariableScope scope = new ObjectScope(thisValue);
    scope.setParent(topScope);
    topScope = scope;
  }
  public VariableScope currentScope() { return topScope; }
  // Provides easy access to the core types
  CoreTypeLibrary coreTypes = new CoreTypeLibrary();
  public CoreTypeLibrary coreTypes() { return coreTypes; }
}
