package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.Type;

public class TypeSuggester implements Suggester
{
  CodeCompletionContext context;
  boolean allowVoid = false;
  
  public TypeSuggester(CodeCompletionContext suggestionContext, boolean allowVoid)
  {
    this.context = suggestionContext;
    this.allowVoid = allowVoid;
  }
  
  @Override
  public List<String> gatherSuggestions(String val)
  {
    GatheredSuggestions gatheredSuggestions = new GatheredSuggestions();
    gatheredSuggestions.setStringMatch(val);
    if (allowVoid)
      gatheredSuggestions.addSuggestion("void");
    for (Type t: context.currentScope().getAllKnownTypes())
    {
      gatheredSuggestions.addSuggestion(t.name);
    }
    return gatheredSuggestions.mergeFinalSuggestions();
  }
}
