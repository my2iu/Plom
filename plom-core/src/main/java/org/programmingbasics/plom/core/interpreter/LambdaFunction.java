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
    // Since we're using the closure scope of the containing method,
    // we don't care about the owner of the method. That information
    // is already stored in the closure scope
    Type methodOwner = null;
    
    ExecutableFunction toReturn = ExecutableFunction.forCode(codeUnit, functionBody, methodOwner, sourceLookup, argPosToName); 
    return toReturn;
  }
}
