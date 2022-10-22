package org.programmingbasics.plom.core.interpreter;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.AstNode;

/**
 * Holds information about a specific runtime instance of a lambda
 * function or anonymous function or function literal.
 */
public class LambdaFunction
{
  AstNode functionBody;
  
  public ExecutableFunction toExecutableFunction()
  {
    ExecutableFunction toReturn = ExecutableFunction.forCode(CodeUnitLocation.forUnknown(), functionBody, Collections.emptyList()); 
    return toReturn;
  }
}
