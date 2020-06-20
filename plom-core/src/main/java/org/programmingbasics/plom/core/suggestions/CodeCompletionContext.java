package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.interpreter.Type;
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
  public Type getLastTypeUsed()
  {
    return lastTypeUsed;
  }
  public void pushType(Type type)
  {
    typeStack.add(type);
    lastTypeUsed = type;
  }
  public Type popType()
  {
    return typeStack.remove(typeStack.size() - 1);
  }
  public void clearLastTypeUsed()
  {
    lastTypeUsed = null;
  }
  public void pushNewScope()
  {
    VariableScope scope = new VariableScope();
    scope.setParent(topScope);
    topScope = scope;
  }
  public VariableScope currentScope() { return topScope; }
}
