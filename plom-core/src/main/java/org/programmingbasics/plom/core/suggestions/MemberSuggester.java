package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

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
    List<String> suggestions = new ArrayList<>();
    Type type = context.lastTypeUsed;
    while (type != null)
    {
      type.lookupMethodSuggestions(val, suggestions);
      type = type.parent;
    }
    return suggestions;
  }
}
