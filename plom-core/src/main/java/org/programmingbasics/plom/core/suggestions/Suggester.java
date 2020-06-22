package org.programmingbasics.plom.core.suggestions;

import java.util.List;

@FunctionalInterface public interface Suggester
{
  public List<String> gatherSuggestions(String val); 
}
