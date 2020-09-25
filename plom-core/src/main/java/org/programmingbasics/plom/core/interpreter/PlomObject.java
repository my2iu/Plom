package org.programmingbasics.plom.core.interpreter;

public class PlomObject
{
  public PlomObject(CoreTypeLibrary coreTypes, int numSlots)
  {
    slots = new Value[numSlots];
    for (int n = 0; n < numSlots; n++)
      slots[n] = Value.createCopy(coreTypes.getNullValue());
  }
  public Value [] slots;
}
