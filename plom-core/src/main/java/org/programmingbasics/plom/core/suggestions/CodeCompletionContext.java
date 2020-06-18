package org.programmingbasics.plom.core.suggestions;

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
  public void pushNewScope()
  {
    VariableScope scope = new VariableScope();
    scope.setParent(topScope);
    topScope = scope;
  }
  public VariableScope currentScope() { return topScope; }
}
