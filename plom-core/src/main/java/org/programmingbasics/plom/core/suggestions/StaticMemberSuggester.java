package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

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
    List<String> suggestions = new ArrayList<>();
    Type type = context.getLastTypeForStaticCall();
    while (type != null)
    {
      type.lookupStaticMemberSuggestions(val, suggestions);
      type = type.parent;
    }
    return suggestions;
  }
}
