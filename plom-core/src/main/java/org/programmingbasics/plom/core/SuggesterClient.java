package org.programmingbasics.plom.core;

import java.util.List;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.StaticMemberSuggester;
import org.programmingbasics.plom.core.suggestions.Suggester;
import org.programmingbasics.plom.core.suggestions.TypeSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;

import elemental.client.Browser;

/**
 * Just a temporary class that will eventually abstract out the client
 * communication with the language server worker thread to calculate
 * suggestions
 */
public class SuggesterClient
{
  private SuggesterClient(Suggester suggester, boolean shouldShowAllSuggestions)
  {
    this.suggester = suggester;
    this.shouldShowAllSuggestions = shouldShowAllSuggestions;
  }
  private Suggester suggester;
  private boolean shouldShowAllSuggestions = false;
  void gatherSuggestions(String val, Consumer<List<String>> callback)
  {
    List<String> suggestions = suggester.gatherSuggestions(val);
    Browser.getWindow().setTimeout(() -> {
      callback.accept(suggestions);
    }, 0);
  }
  
  public boolean shouldShowAllSuggestions() { return shouldShowAllSuggestions; }

  
  public static SuggesterClient makeMemberSuggester(CodeCompletionContext context)
  {
    return new SuggesterClient(new MemberSuggester(context), true);
  }
  
  public static SuggesterClient makeStaticMemberSuggester(CodeCompletionContext context, boolean includeNonConstructors, boolean includeConstructors) 
  {
    return new SuggesterClient(new StaticMemberSuggester(context, includeNonConstructors, includeConstructors), true);
  }

  public static SuggesterClient makeTypeSuggester(CodeCompletionContext suggestionContext, boolean allowVoid)
  {
    return new SuggesterClient(new TypeSuggester(suggestionContext, allowVoid), false);
  }
  
  public static SuggesterClient makeVariableSuggester(CodeCompletionContext context)
  {
    return new SuggesterClient(new VariableSuggester(context), false);
  }
}
