package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

public class TypeSuggester implements Suggester
{
  boolean allowVoid = false;
  
  public TypeSuggester(boolean allowVoid)
  {
    this.allowVoid = allowVoid;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    List<String> toReturn = new ArrayList<>();
    if (allowVoid)
      toReturn.add("void");
    toReturn.add("string");
    toReturn.add("number");
    toReturn.add("object");
    toReturn.add("boolean");
    return toReturn;
  }
}
