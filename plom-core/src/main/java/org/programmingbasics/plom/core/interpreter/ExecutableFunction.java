package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;

/**
 * Information needed about a function for it to be executed in a MachineContext
 */
public class ExecutableFunction
{
  public AstNode code;
  // Mapping from position of an argument to the name it should appear
  // as in the function
  public List<String> argPosToName = new ArrayList<>();
}
