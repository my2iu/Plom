package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.interpreter.VariableScope;

public class VariableSuggester implements Suggester
{
  CodeCompletionContext context;
  
  public VariableSuggester(CodeCompletionContext context)
  {
    this.context = context;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    List<String> suggestions = new ArrayList<>();
    VariableScope scope = context.currentScope();
    while (scope != null)
    {
      scope.lookupSuggestions(val, suggestions);
      scope = scope.getParent();
    }
    return suggestions;
  }

}
