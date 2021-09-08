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
  private List<String> bestSuggestions = new ArrayList<>();
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
    // Right now we prioritize matches that start with the search string
    if (bestSuggestions.contains(suggestion)) return;
    if (suggestion.startsWith(search))
    {
      bestSuggestions.add(suggestion);
      return;
    }
    // Do a simple text match for now to find the best matches
    if (suggestions.contains(suggestion)) return;
    if (suggestion.contains(search))
    {
      suggestions.add(suggestion);
      return;
    }
    // Potentially handle inexact matches or general keyword matching too
    
    // Save out other bunch of possibilities
    if (genericSuggestions.contains(suggestion)) return;
    genericSuggestions.add(suggestion);
  }

  public void addAllSuggestions(Collection<String> suggestions)
  {
    for (String suggestion: suggestions)
      addSuggestion(suggestion);
  }
  
  public List<String> mergeFinalSuggestions()
  {
    // Pad out suggestions with weaker suggestions up to 500 entries
    if (bestSuggestions.size() < 500)
      bestSuggestions.addAll(suggestions.subList(0, Math.min(suggestions.size(), 500)));
    if (bestSuggestions.size() < 500)
      bestSuggestions.addAll(genericSuggestions.subList(0, Math.min(genericSuggestions.size(), 500)));
    return bestSuggestions;
  }

}
