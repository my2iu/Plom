package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;

/**
 * Holds information about a specific runtime instance of a lambda
 * function or anonymous function or function literal.
 */
public class LambdaFunction
{
  public AstNode functionBody;
  public CodeUnitLocation codeUnit;
  public List<String> argPosToName;
  public VariableScope closureScope;
  // public Value self;  // Not actually needed since it's implicitly stored in the closureScope
  
  public ExecutableFunction toExecutableFunction()
  {
    ExecutableFunction toReturn = ExecutableFunction.forCode(codeUnit, functionBody, argPosToName); 
    return toReturn;
  }
}
