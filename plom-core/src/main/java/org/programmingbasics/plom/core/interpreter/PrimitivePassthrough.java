package org.programmingbasics.plom.core.interpreter;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface PrimitivePassthrough
{
  public void call(MachineContext.PrimitiveBlockingFunctionReturn blockWait, MachineContext machine) throws RunException;
}
