package org.programmingbasics.plom.core.interpreter;

import java.util.List;
import java.util.Optional;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.StatementContainer;

/**
 * Holds information about a specific runtime instance of a lambda
 * function or anonymous function or function literal.
 */
public class LambdaFunction
{
  public AstNode functionBody;
  public CodeUnitLocation codeUnit;
  public Optional<StatementContainer> sourceLookup;
  public List<String> argPosToName;
  public VariableScope closureScope;
  // public Value self;  // Not actually needed since it's implicitly stored in the closureScope
  
  public ExecutableFunction toExecutableFunction()
  {
    ExecutableFunction toReturn = ExecutableFunction.forCode(codeUnit, functionBody, sourceLookup, argPosToName); 
    return toReturn;
  }
}
