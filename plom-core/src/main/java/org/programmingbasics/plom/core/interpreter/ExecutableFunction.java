package org.programmingbasics.plom.core.interpreter;

import java.util.List;
import java.util.Optional;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.StatementContainer;

/**
 * Information needed about a function for it to be executed in a MachineContext
 */
public class ExecutableFunction
{
  public AstNode code;
  // Mapping from position of an argument to the name it should appear
  // as in the function
  public List<String> argPosToName;
  
  public CodeUnitLocation codeUnit;
  
  /** 
   * Optional source reference for a function that can be used for
   * looking up error locations
   */
  public Optional<StatementContainer> sourceLookup = Optional.empty();
  
  public static ExecutableFunction forCode(CodeUnitLocation codeUnit, AstNode code, Optional<StatementContainer> sourceLookupForDebug, List<String> argPosToName)
  {
    ExecutableFunction toReturn = new ExecutableFunction();
    toReturn.codeUnit = codeUnit;
    toReturn.sourceLookup = sourceLookupForDebug;
    toReturn.code = code;
    toReturn.argPosToName = argPosToName;
    return toReturn;
  }
}
