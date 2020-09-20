package org.programmingbasics.plom.core.suggestions;

import java.util.ArrayList;
import java.util.List;

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
    List<String> toReturn = new ArrayList<>();
    if (allowVoid)
      toReturn.add("void");
    for (Type t: context.currentScope().getAllKnownTypes())
    {
      toReturn.add(t.name);
    }
    return toReturn;
  }
}
