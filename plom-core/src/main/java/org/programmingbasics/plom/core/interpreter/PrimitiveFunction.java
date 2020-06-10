package org.programmingbasics.plom.core.interpreter;

import java.util.List;

public interface PrimitiveFunction
{
  public Value call(List<Value> args);
}
