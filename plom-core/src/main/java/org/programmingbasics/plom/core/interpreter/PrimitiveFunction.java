package org.programmingbasics.plom.core.interpreter;

import java.util.List;

public interface PrimitiveFunction
{
  public Value call(List<Value> args);
  
  public static interface PrimitiveBlockingFunction
  {
    public void call(MachineContext.PrimitiveBlockingFunctionReturn blockWait, List<Value> args);
  }
  
  public static interface PrimitiveMethod
  {
    public Value call(Value self, List<Value> args);
  }
}
