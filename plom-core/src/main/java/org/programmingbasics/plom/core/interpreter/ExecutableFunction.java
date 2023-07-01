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
  /**
   * Because we're interpreting the code, we need to know the class
   * where a method is defined (if available), to make sure we 
   * access the member variables correctly. We can't use the concrete
   * type of the instance because it may be a subtype of the class
   * where the method was defined, so it may refer to member variables
   * that don't exist on the actual owning class of the method
   */
  public Type owningClass;
  
  public CodeUnitLocation codeUnit;
  
  /** 
   * Optional source reference for a function that can be used for
   * looking up error locations
   */
  public Optional<StatementContainer> sourceLookup = Optional.empty();
  
  private ExecutableFunction() {}
  
  public static ExecutableFunction forCode(CodeUnitLocation codeUnit, AstNode code, Type classOfMethod, Optional<StatementContainer> sourceLookupForDebug, List<String> argPosToName)
  {
    ExecutableFunction toReturn = new ExecutableFunction();
    toReturn.codeUnit = codeUnit;
    toReturn.sourceLookup = sourceLookupForDebug;
    toReturn.code = code;
    toReturn.owningClass = classOfMethod;
    toReturn.argPosToName = argPosToName;
    return toReturn;
  }
}
