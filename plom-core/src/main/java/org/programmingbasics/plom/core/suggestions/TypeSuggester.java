package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

public class TypeSuggester implements Suggester
{
  @Override
  public List<String> gatherSuggestions(String val)
  {
    List<String> toReturn = new ArrayList<>();
    toReturn.add("string");
    toReturn.add("number");
    toReturn.add("object");
    toReturn.add("boolean");
    return toReturn;
  }
}
