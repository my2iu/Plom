package org.programmingbasics.plom.core.ast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Reading and writing code as text
 */
public class PlomTextReader
{
  public static class StringTextReader
  {
    public StringTextReader(String text) { this.text = text; }
    String text;
    int pos = 0;
    public int peek()
    {
      // TODO: Handle code points
      if (pos < text.length())
        return text.charAt(pos);
      return -1;
    }
    public int read()
    {
      // TODO: Handle code points
      if (pos < text.length())
      {
        int toReturn = text.charAt(pos);
        pos++;
        return toReturn;
      }
      return -1;
    }
  }
  
  static final Map<Symbol, String> symbolTokenMap = PlomTextWriter.symbolTokenMap;
  static final List<String> validLexerTokens = new ArrayList<>();
  static {
    // The ones that directly map onto symbols
    for (String tok: symbolTokenMap.values())
      validLexerTokens.add(tok);
    // Some extra ones
    validLexerTokens.add("{");
    validLexerTokens.add("}");
    // This sort assumes that "ab" comes before "abc"
    validLexerTokens.sort(Comparator.naturalOrder());
  }
  
  // Scans the input and returns the next chunk of text
  public void lexInput(StringTextReader in) throws IOException
  {
    // Skip whitespace
    if (in.peek() < 0)
      return;
    while (Character.isWhitespace(in.peek()))
      in.read();
    if (in.peek() < 0)
      return;
    
    // Check for special cases
    int peek = in.peek();
    if (peek == '\"')
    {
      return;
    }
    else if (peek >= '0' && peek <= '9')
    {
      return;
    }
    
    String token = "";
    String possibleMatch = null;
  }
  
  // Check if it matches one of the expected tokens (we'll do this inefficiently for now)
  private String prefixMatch(String toMatch)
  {
    // Assume that validLexerTokens is sorted so that "ab" comes before "abc"
    for (String str: validLexerTokens)
    {
      if (!str.startsWith(toMatch))
        continue;
      return str;
    }
    return null;
  }
}
