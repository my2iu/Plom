package org.programmingbasics.plom.core.suggestions;

import java.util.List;

import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.Type;

public class MemberSuggester implements Suggester
{
  CodeCompletionContext context;
  
  public MemberSuggester(CodeCompletionContext context)
  {
    this.context = context;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    GatheredSuggestions gatheredSuggestions = new GatheredSuggestions();
    gatheredSuggestions.setStringMatch(val);
    Type type = context.getLastTypeUsed();
    int depth = 0;
    final int MAX_DEPTH = 10000;
    while (type != null && depth < MAX_DEPTH)
    {
      type.lookupMethodSuggestions(gatheredSuggestions);
      type = type.parent;
      depth++;
    }
    return gatheredSuggestions.mergeFinalSuggestions();
  }
  
  @Override
  public boolean shouldShowAllSuggestions()
  {
    return true;
  }
}
