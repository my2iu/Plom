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
  SuggesterClient(Suggester suggester, boolean shouldShowAllSuggestions)
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
}
