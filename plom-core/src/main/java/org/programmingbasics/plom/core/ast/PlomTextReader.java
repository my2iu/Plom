package org.programmingbasics.plom.core.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    public int peek(int n)
    {
      // TODO: Handle code points
      if (pos + n < text.length())
        return text.charAt(pos + n);
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

  public static class StringToken
  {
    public String str;
    public Symbol sym;
    public boolean isErr;
    public boolean isEnd;
    public void setToEof()
    {
      str = null;
      sym = null;
      isErr = false;
      isEnd = true;
    }
    public void setToErr()
    {
      str = null;
      sym = null;
      isErr = true;
      isEnd = false;
    }
    public void setToken(String str, Symbol sym)
    {
      this.str = str;
      this.sym = sym;
      isErr = false;
      isEnd = false;
    }
  }
  
  static final Map<Symbol, String> symbolTokenMap = PlomTextWriter.symbolTokenMap;
  static final Map<String, Symbol> reverseSymbolTokenMap = new HashMap<>();
  static final List<String> validLexerTokens = new ArrayList<>();
  static {
    // The ones that directly map onto symbols
    for (Map.Entry<Symbol, String> tok: symbolTokenMap.entrySet())
    {
      validLexerTokens.add(tok.getValue());
      reverseSymbolTokenMap.put(tok.getValue(), tok.getKey());
    }
    // Some extra ones
    validLexerTokens.add("{");
    validLexerTokens.add("}");
    // This sort assumes that "ab" comes before "abc"
    validLexerTokens.sort(Comparator.naturalOrder());
  }
  
  private void lexStringLiteral(StringTextReader in, StringToken toReturn)
  {
    String str = "";
    
    int next = in.read();
    if (next != '\"') throw new IllegalArgumentException("Expecting string to start with double quotation");
    str += (char)next;
    
    next = in.read();
    while (next != '\"' && next >= 0)
    {
      // TODO: Handle escaping
      str += (char)next;
      next = in.read();
    }
    if (next < 0)
    {
      // Unterminated string
      toReturn.setToErr();
      return;
    }
    str += (char)next;
    toReturn.setToken(str, Symbol.String);
  }

  // Scans the input and returns the next chunk of text
  public void lexInput(StringTextReader in, StringToken toReturn) throws IOException
  {
    // Skip whitespace
    if (in.peek() < 0)
    {
      toReturn.setToEof();
      return;
    }
    while (Character.isWhitespace(in.peek()))
      in.read();
    if (in.peek() < 0)
    {
      toReturn.setToEof();
      return;
    }
    
    // Check for special cases
    int peek = in.peek();
    if (peek == '\"')
    {
      lexStringLiteral(in, toReturn);
      return;
    }
    
    // Read ahead until we can match a symbol
    String chars = "";
    int bestMatch = -1;
    for (int peekAhead = 0;; peekAhead++)
    {
      peek = in.peek(peekAhead);
      if (peek < 0) break;
      chars = chars + (char)in.peek(peekAhead);
      String match = prefixMatch(chars);
      if (match == null && isNumberMatch(chars))
        match = chars;
      if (match != null)
      {
        if (match.length() == chars.length())
          bestMatch = peekAhead;
      }
      else
        break;
    }
    
    // If we matched a token, then return it.
    if (bestMatch >= 0)
    {
      String matched = "";
      for (int n = 0; n <= bestMatch; n++)
        matched += (char)in.read();
      if (isNumberMatch(matched))
        toReturn.setToken(matched, Symbol.Number);
      else
        toReturn.setToken(matched, reverseSymbolTokenMap.get(matched));
      return;
    }
    toReturn.setToErr();
  }

  // Scans the input for part of a parameter token. Returns the string, or null for eof or nothing matching
  public String lexParameterTokenPart(StringTextReader in) throws IOException
  {
    // Skip whitespace
    if (in.peek() < 0)
    {
      return null;
    }
    while (Character.isWhitespace(in.peek()))
      in.read();
    if (in.peek() < 0)
    {
      return null;
    }
    
    String match = "";
    int peek = in.peek();
    while (peek != ':' && peek != '}' && peek >= 0)
    {
      match += (char)in.read();
      peek = in.peek();
    }
    if (peek == ':')
      match += (char)in.read();
    if (match.isEmpty()) 
      return null;
    return match;
  }
  
  private boolean isNumberMatch(String toMatch)
  {
    return toMatch.matches("[-]?([0-9]+|[0-9]+[.][0-9]*)");
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
