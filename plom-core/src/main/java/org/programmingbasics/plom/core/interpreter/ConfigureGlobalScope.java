package org.programmingbasics.plom.core.interpreter;

import jsinterop.annotations.JsFunction;

/**
 * Callback for configuring standard libraries and global scope.
 */
@JsFunction
public interface ConfigureGlobalScope
{
  public void configure(VariableScope scope, CoreTypeLibrary coreTypes);
}
