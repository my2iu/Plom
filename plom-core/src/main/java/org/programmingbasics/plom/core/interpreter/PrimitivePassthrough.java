package org.programmingbasics.plom.core.interpreter;

public interface PrimitivePassthrough
{
  public void call(MachineContext.PrimitiveBlockingFunctionReturn blockWait, MachineContext machine) throws RunException;
}
