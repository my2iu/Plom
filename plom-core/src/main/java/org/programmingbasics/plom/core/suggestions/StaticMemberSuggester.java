package org.programmingbasics.plom.core.suggestions;

import java.util.List;

import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.Type;

public class StaticMemberSuggester implements Suggester
{
  CodeCompletionContext context;
  
  public StaticMemberSuggester(CodeCompletionContext context)
  {
    this.context = context;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    GatheredSuggestions gatheredSuggestions = new GatheredSuggestions();
    gatheredSuggestions.setStringMatch(val);
    Type type = context.getLastTypeForStaticCall();
    type.lookupStaticMemberSuggestions(gatheredSuggestions);
    return gatheredSuggestions.mergeFinalSuggestions();
  }
}
