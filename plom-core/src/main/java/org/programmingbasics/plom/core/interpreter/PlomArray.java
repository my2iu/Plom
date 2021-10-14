package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

public class PlomArray extends PlomObject
{
  List<Value> backingList = new ArrayList<>();
  
  public PlomArray(CoreTypeLibrary coreTypes, int numSlots)
  {
    super(coreTypes, numSlots);
  }
  
  public int size()
  {
    return backingList.size();
  }
  
  public Value get(int index)
  {
    return backingList.get(index);
  }
  
  public void expand(CoreTypeLibrary coreTypes)
  {
    backingList.add(Value.create(coreTypes.nullVal.val, coreTypes.nullVal.type));
  }
}
