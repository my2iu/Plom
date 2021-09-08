package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
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
    GatheredSuggestions gatheredSuggestions = new GatheredSuggestions();
    gatheredSuggestions.setStringMatch(val);
    VariableScope scope = context.currentScope();
    while (scope != null)
    {
      scope.lookupSuggestions(gatheredSuggestions);
      scope = scope.getParent();
    }
    return gatheredSuggestions.mergeFinalSuggestions();
  }

}
