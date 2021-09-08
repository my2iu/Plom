package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * When gathering suggestions, we might need to store suggestions with
 * different priorities from different places and then merge suggestions
 * together later, so we have a special object for that
 */
public class GatheredSuggestions
{
  private List<String> suggestions = new ArrayList<>();
  private List<String> genericSuggestions = new ArrayList<>();
  private String search = "";
  
  public void setStringMatch(String search)
  {
    this.search = search;
  }
  
  public void addSuggestion(String suggestion)
  {
    // Do a simple text match for now to find the best matches
    if (suggestion.contains(search))
    {
      if (!suggestions.contains(suggestion))
        suggestions.add(suggestion);
    }
    else
    {
      if (!genericSuggestions.contains(suggestion))
        genericSuggestions.add(suggestion);
    }
  }

  public void addAllSuggestions(Collection<String> suggestions)
  {
    for (String suggestion: suggestions)
      addSuggestion(suggestion);
  }
  
  public List<String> mergeFinalSuggestions()
  {
    return suggestions;
  }

}
