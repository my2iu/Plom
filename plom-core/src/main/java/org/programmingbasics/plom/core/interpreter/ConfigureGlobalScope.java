package org.programmingbasics.plom.core.interpreter;

/**
 * Callback for configuring standard libraries and global scope.
 */
public interface ConfigureGlobalScope
{
  public void configure(VariableScope scope, CoreTypeLibrary coreTypes);
}
