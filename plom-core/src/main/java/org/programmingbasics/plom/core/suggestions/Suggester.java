package org.programmingbasics.plom.core.suggestions;

import java.util.List;

public interface Suggester
{
  public List<String> gatherSuggestions(String val);
  
  /**
   * When displaying the suggestions in the UI, should the UI
   * try to show the complete list of suggestions. (For example,
   * when listing the methods of a class, the programmer might
   * want to scroll through the complete list to find something,
   * so it's useful to try harder to show everything if possible)
   */
  public default boolean shouldShowAllSuggestions() { return false; }
}
