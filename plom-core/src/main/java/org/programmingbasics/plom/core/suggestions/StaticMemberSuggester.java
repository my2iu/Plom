package org.programmingbasics.plom.core.suggestions;

import java.util.List;

import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.Type;

public class StaticMemberSuggester implements Suggester
{
  CodeCompletionContext context;
  boolean includeNonConstructors;
  boolean includeConstructors;
  
  public StaticMemberSuggester(CodeCompletionContext context, boolean includeNonConstructors, boolean includeConstructors)
  {
    this.context = context;
    this.includeNonConstructors = includeNonConstructors;
    this.includeConstructors = includeConstructors;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    GatheredSuggestions gatheredSuggestions = new GatheredSuggestions();
    gatheredSuggestions.setStringMatch(val);
    Type type = context.getLastTypeForStaticCall();
    if (type != null)
      type.lookupStaticMemberSuggestions(gatheredSuggestions, includeNonConstructors, includeConstructors);
    return gatheredSuggestions.mergeFinalSuggestions();
  }
  
  @Override
  public boolean shouldShowAllSuggestions()
  {
    return true;
  }
}
