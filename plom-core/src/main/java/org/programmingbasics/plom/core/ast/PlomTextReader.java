package org.programmingbasics.plom.core.ast;

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
    public boolean isEnd;
    public void setToEof()
    {
      str = null;
      sym = null;
      isEnd = true;
    }
    public void setToken(String str, Symbol sym)
    {
      this.str = str;
      this.sym = sym;
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
    validLexerTokens.add("\r");
    validLexerTokens.add("\r\n");
    validLexerTokens.add("\n");
    // This sort assumes that "ab" comes before "abc"
    validLexerTokens.sort(Comparator.naturalOrder());
  }

  private static boolean isNumberMatch(String toMatch)
  {
    return toMatch.matches("[-]?([0-9]+|[0-9]+[.][0-9]*)");
  }

  private static Symbol classifyTokenString(String str)
  {
    if (str.startsWith("\""))
      return Symbol.String;
    else if (isNumberMatch(str))
      return Symbol.Number;
    else
      return reverseSymbolTokenMap.get(str);
  }

  public static class PlomTextScanner
  {
    public PlomTextScanner(StringTextReader in)
    {
      this.in = in;
    }
    private StringTextReader in;
    
    private String lexStringLiteral() throws PlomReadException
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
        throw new PlomReadException("Unterminated string", in);
      }
      str += (char)next;
      return str;
    }

    private String peekedLex;
    
    public String peekLexInput() throws PlomReadException
    {
      if (peekedLex != null)
        return peekedLex;
      peekedLex = lexInput();
      return peekedLex;
    }
    
    // Scans the input and returns the next chunk of text
    public String lexInput() throws PlomReadException
    {
      if (peekedLex != null)
      {
        String toReturn = peekedLex;
        peekedLex = null;
        return toReturn;
      }
      
      // Skip whitespace
      if (in.peek() < 0)
      {
        return null;
      }
      while (Character.isWhitespace(in.peek()) && in.peek() != '\r' && in.peek() != '\n')
        in.read();
      if (in.peek() < 0)
      {
        return null;
      }
      
      // Check for special cases
      int peek = in.peek();
      if (peek == '\"')
      {
        return lexStringLiteral();
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
        return matched;
      }
      throw new PlomReadException("Unknown keyword", in);
    }

    // Scans the input for part of a parameter token. Returns the string, or null for eof or nothing matching
    public String lexParameterTokenPart() throws PlomReadException
    {
      // Skip whitespace
      if (in.peek() < 0)
      {
        return null;
      }
      while (Character.isWhitespace(in.peek()) && in.peek() != '\r' && in.peek() != '\n')
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
    
    public String lexComment() throws PlomReadException
    {
      String toReturn = "";
      while (in.peek() >= 0 && in.peek() != '\r' && in.peek() != '\n')
      {
        int next = in.read();
        if (next == '\\')
        {
          if (in.peek() == '\\')
            next = '\\';
          else if (in.peek() == 'n')
            next = '\n';
          else if (in.peek() == 'r')
            next = '\r';
          else if (in.peek() == '\n')
            next = '\n';
          else if (in.peek() == '\r')
            next = '\r';
          else
            throw new PlomReadException("Unknown or unfinished escape in comment", in);
          in.read();
        }
        toReturn += (char)next;
      }
      return toReturn;
    }
    
    // Check if it matches one of the expected tokens (we'll do this inefficiently for now)
    private static String prefixMatch(String toMatch)
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
  
  public static class PlomReadException extends Exception
  {
    public PlomReadException() {}
    public PlomReadException(String msg, StringTextReader readState) { super(msg); }
    public PlomReadException(String msg, PlomTextScanner readState) { super(msg); }
    public PlomReadException(Throwable chain) { super(chain); }
  }

  private static void expectToken(PlomTextScanner lexer, String expected) throws PlomReadException
  {
    if (!lexer.lexInput().equals(expected))
      throw new PlomReadException("Expecting a " + expected, lexer);
  }

  private static void expectNewlineToken(PlomTextScanner lexer) throws PlomReadException
  {
    if (!isNewline(lexer.lexInput()))
      throw new PlomReadException("Expecting a newline", lexer);
  }

  private static void swallowOptionalNewlineToken(PlomTextScanner lexer) throws PlomReadException
  {
    if (isNewline(lexer.peekLexInput()))
      lexer.lexInput();
  }

  private static boolean isNewline(String toMatch)
  {
    return toMatch.equals("\r") || toMatch.equals("\r\n") || toMatch.equals("\n");
  }
  

  public static Token readToken(PlomTextScanner lexer) throws PlomReadException
  {
    String peek = lexer.peekLexInput();
    if (peek == null) return null;
    Symbol sym = classifyTokenString(peek);
    if (sym == null) return null;
    if (sym.isWide())
    {
      switch (sym)
      {
      case COMPOUND_IF:
      case COMPOUND_ELSEIF:
      case COMPOUND_WHILE:
      {
        lexer.lexInput();
        expectToken(lexer, "{");
        TokenContainer expression = readTokenContainer(lexer);
        expectToken(lexer, "}");
        expectToken(lexer, "{");
        swallowOptionalNewlineToken(lexer);
        StatementContainer block = readStatementContainer(lexer);
        expectToken(lexer, "}");
        swallowOptionalNewlineToken(lexer);
        return new Token.OneExpressionOneBlockToken(peek, sym, expression, block);
      }
      case COMPOUND_ELSE:
      {
        lexer.lexInput();
        expectToken(lexer, "{");
        swallowOptionalNewlineToken(lexer);
        StatementContainer block = readStatementContainer(lexer);
        expectToken(lexer, "}");
        swallowOptionalNewlineToken(lexer);
        return new Token.OneBlockToken(peek, sym, block);
      }
      case DUMMY_COMMENT:
        lexer.lexInput();
        String str = lexer.lexComment();
        expectNewlineToken(lexer);
        return new Token.WideToken("//" + str, sym);
      }
    }
    else
    {
      if (sym == Symbol.AtType || sym == Symbol.DotVariable)
      {
        String prefix = (sym == Symbol.AtType ? "@" : "."); 
        lexer.lexInput();
        expectToken(lexer, "{");
        String name = "";
        List<TokenContainer> params = new ArrayList<>();
        String nextPart = lexer.lexParameterTokenPart();
        if (nextPart.endsWith(":"))
        {
          while (true)
          {
            name += nextPart;
            expectToken(lexer, "{");
            params.add(readTokenContainer(lexer));
            expectToken(lexer, "}");
            nextPart = lexer.lexParameterTokenPart();
            if (nextPart == null) break;
            if (!nextPart.endsWith(":"))
              throw new PlomReadException("Expecting identifier part to end with :", lexer); 
          }
          expectToken(lexer, "}");
          return Token.ParameterToken.fromContents(prefix + name, sym, params.toArray(new TokenContainer[0]));
        }
        else
        {
          expectToken(lexer, "}");
          return Token.ParameterToken.fromContents(prefix + nextPart, sym);
        }
      }
      else
      {
        lexer.lexInput();
        return new Token.SimpleToken(peek, sym);
      }
    }
    return null;
  }
  
  public static TokenContainer readTokenContainer(PlomTextScanner lexer) throws PlomReadException
  {
    TokenContainer tokens = new TokenContainer();
    for (Token next = readToken(lexer); next != null; next = readToken(lexer))
    {
      tokens.tokens.add(next);
    }
    return tokens;
  }
  
  public static StatementContainer readStatementContainer(PlomTextScanner lexer) throws PlomReadException
  {
    StatementContainer container = new StatementContainer();
    TokenContainer tokens = readTokenContainer(lexer);
    container.statements.add(tokens);
    while (true)
    {
      String peek = lexer.peekLexInput();
      if (peek == null || !isNewline(peek))
        break;
      lexer.lexInput();
      tokens = readTokenContainer(lexer);
      container.statements.add(tokens);
    }
    // The last line might have a newline at the end (signaling a new line),
    // but we don't want a blank line there.
    if (!container.statements.isEmpty() && container.statements.get(container.statements.size() - 1).tokens.isEmpty())
      container.statements.remove(container.statements.size() - 1);
    return container;
  }
}
