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
  public void clearLastTypeUsed()
  {
    lastTypeUsed = null;
  }
  public void setLastTypeUsed(Type val)
  {
    lastTypeUsed = val;
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
  public VariableScope currentScope() { return topScope; }
}
