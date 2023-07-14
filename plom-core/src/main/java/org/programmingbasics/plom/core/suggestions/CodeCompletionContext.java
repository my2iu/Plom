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
//  private List<Type> knownTypes;
  private VariableScope globalScope = new VariableScope();
  private VariableScope topScope = globalScope;
  private List<Type> typeStack = new ArrayList<>();
  private Type lastTypeUsed;
  private Type lastTypeForStaticCall;
  /** Type that should be returned from an expression (mainly used for function literals) */
  private Type expectedType;
  
  /** Used internally as a scratchpad to track the function signature of the last method/function called when "executing" tokens of a line of code */
  public Type.TypeSignature lastSignatureCalled;
  
  /** Class where the code is defined on (or null if not applicable) */
  private Type definedClassOfMethod;
  /** If the code is from a static method */
  private boolean isStaticMethod;
  /** If the code is from a constructor method */
  private boolean isConstructorMethod;
  
  public CodeCompletionContext()
  {
    
  }

  // Provides easy access to the core types
  CoreTypeLibrary coreTypes = new CoreTypeLibrary();
  public CoreTypeLibrary coreTypes() { return coreTypes; }
  
  public static Builder builder() { return new Builder(); }
  public static class Builder
  {
    private CodeCompletionContext instance = new CodeCompletionContext();
    
    public void pushNewScope()
    {
      VariableScope scope = new VariableScope();
      scope.setParent(instance.topScope);
      instance.topScope = scope;
    }
    public void pushObjectScope(Value thisValue)
    {
      VariableScope scope = new ObjectScope(thisValue, thisValue.type);
      scope.setParent(instance.topScope);
      instance.topScope = scope;
    }
    public VariableScope currentScope() { return instance.topScope; }
    public CoreTypeLibrary coreTypes() { return instance.coreTypes; }
    
    public void setDefinedClassOfMethod(Type t) { instance.definedClassOfMethod = t; }
    public void setIsStaticMethod(boolean val) { instance.isStaticMethod = val; }
    public void setIsConstructorMethod(boolean val) { instance.isConstructorMethod = val; }
    public CodeCompletionContext build() { return instance; }
  }
  
  
  
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
  
  /**
   * The expected type of an expression (e.g. some very limited
   * type inference, so what is the expected type of a return, or
   * when assigning to a variable of known type, or passing in a 
   * parameter)
   */
  public void setExpectedExpressionType(Type val)
  {
    expectedType = val;
  }
  public Type getExpectedExpressionType()
  {
    return expectedType;
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
  
  // TODO: Instead of reusing the same context and resetting it,
  // it might make more sense to make copies of the context each
  // time we use it.
  public void resetState()
  {
    // This is not a full reset. It only resets the variables used to
    // trace the types used tokens when executing instructions. The scope
    // and variables are not reset.
    typeStack.clear();
    setExpectedExpressionType(null);
    setLastTypeForStaticCall(null);
    clearLastTypeUsed();
  }
  
  // The below methods are used to configure the context of the code where suggestions are gathered from
  // Whereas the above methods are for executing and analysing the code to actually gather types and context
  // for gathering suggestions

  public VariableScope currentScope() { return topScope; }
  
  public Type getDefinedClassOfMethod() { return definedClassOfMethod; }
  public boolean getIsStaticMethod() { return isStaticMethod; }
  public boolean getIsConstructorMethod() { return isConstructorMethod; }
}
