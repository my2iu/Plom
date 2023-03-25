package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.gen.Symbol;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.regexp.shared.RegExp;

import jsinterop.annotations.JsType;

/**
 * Reading and writing code as text
 */
@JsType
public class PlomTextReader
{
  @JsType
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
    public int peekN(int n)
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
      if (!isKeywordPatternMatch(tok.getValue()))
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

  private static boolean isKeywordPatternMatch(String toMatch)
  {
    RegExp regex;
    if (GWT.isClient())
      regex = RegExp.compile("^[\\p{ID_Start}][\\p{ID_Continue}]*$", "u");
    else
      // This regex doesn't match ID_Start and ID_Continue, but it's good enough for testing
      // By default, the Java version of the regex can match \n with $, so we need to use \z instead (JS version is ok though)
      regex = RegExp.compile("^[\\p{L}][\\p{L}\\p{N}]*\\z", "");
    return regex.exec(toMatch) != null;
//    if (!Character.isUnicodeIdentifierStart(toMatch.charAt(0)))
//      return false;
//    for (int n = 1; n < toMatch.length(); n++)
//    {
//      if (!Character.isUnicodeIdentifierPart(toMatch.charAt(n)))
//        return false;
//    }
//    return true;
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

  @JsType
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
        peek = in.peekN(peekAhead);
        if (peek < 0) break;
        chars = chars + (char)in.peekN(peekAhead);
        String match = null;
        if (isNumberMatch(chars))
          match = chars;
        else if (isKeywordPatternMatch(chars))
          match = chars;
        else
          match = prefixMatch(chars);
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
      while (peek != ':' && peek != '}' && peek != '.' && peek != '{' && peek >= 0)
      {
        match += (char)in.read();
        peek = in.peek();
      }
      if (peek == ':')
        match += (char)in.read();
      if (match.isEmpty()) 
        return null;
      return match.trim();
    }

    public String lexParameterTokenPartOrEmpty() throws PlomReadException
    {
      String token = lexParameterTokenPart();
      if (token == null)
        return "";
      return token;
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
    
    // Reads a base 64 string used for encoding file data (it's probably also okay to lex it 
    // using lexInput(), but I'll make a special lexer case to be safe)
    public String lexBase64() throws PlomReadException
    {
      String toReturn = "";
      while (in.peek() >= 0)
      {
        if (in.peek() >= 'A' && in.peek() <= 'Z')
          toReturn += (char)in.peek();
        else if (in.peek() >= 'a' && in.peek() <= 'z')
          toReturn += (char)in.peek();
        else if (in.peek() >= '0' && in.peek() <= '9')
          toReturn += (char)in.peek();
        else if (in.peek() == '+' || in.peek() == '/')
          toReturn += (char)in.peek();
        else if (in.peek() == '=' || Character.isWhitespace(in.peek()))
          toReturn += "";
        else
          break;
        in.read();
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
    
    public void expectToken(String expected) throws PlomReadException
    {
      if (!lexInput().equals(expected))
        throw new PlomReadException("Expecting a " + expected, this);
    }

    public void expectNewlineToken() throws PlomReadException
    {
      if (!isNewline(lexInput()))
        throw new PlomReadException("Expecting a newline", this);
    }

    public void swallowOptionalNewlineToken() throws PlomReadException
    {
      if (isNewline(peekLexInput()))
        lexInput();
    }

  }
  
  public static class PlomReadException extends Exception
  {
    public PlomReadException() {}
    public PlomReadException(String msg, StringTextReader readState) { super(msg); }
    public PlomReadException(String msg, PlomTextScanner readState) { super(msg); }
    public PlomReadException(Throwable chain) { super(chain); }
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
      case FunctionLiteral:
      {
        lexer.lexInput();
        lexer.expectToken("{");
        TokenContainer expression = readTokenContainer(lexer);
        lexer.expectToken("}");
        lexer.expectToken("{");
        lexer.swallowOptionalNewlineToken();
        StatementContainer block = readStatementContainer(lexer);
        lexer.expectToken("}");
        if (!sym.isInline())
          lexer.swallowOptionalNewlineToken();
        return new Token.OneExpressionOneBlockToken(peek, sym, expression, block);
      }
      case COMPOUND_ELSE:
      {
        lexer.lexInput();
        lexer.expectToken("{");
        lexer.swallowOptionalNewlineToken();
        StatementContainer block = readStatementContainer(lexer);
        lexer.expectToken("}");
        lexer.swallowOptionalNewlineToken();
        return new Token.OneBlockToken(peek, sym, block);
      }
      case DUMMY_COMMENT:
        lexer.lexInput();
        String str = lexer.lexComment();
        lexer.expectNewlineToken();
        return new Token.WideToken("//" + str, sym);
      }
    }
    else
    {
      if (sym == Symbol.AtType || sym == Symbol.DotVariable || sym == Symbol.FunctionTypeName)
      {
        String prefix;
        switch (sym)
        {
        case AtType:
          prefix = "@";
          break;
        case FunctionTypeName:
          prefix = "f@";
          break;
        case DotVariable:
        default:
          prefix = ".";
          break;
        }
        lexer.lexInput();
        lexer.expectToken("{");
//        String name = "";
        List<String> nameParts = new ArrayList<>();
        List<TokenContainer> params = new ArrayList<>();
        String nextPart = lexer.lexParameterTokenPartOrEmpty();
        if (nextPart.endsWith(":"))
        {
          while (true)
          {
            if (nameParts.isEmpty())
              nameParts.add(prefix + nextPart);
            else
              nameParts.add(nextPart);
            lexer.expectToken("{");
            params.add(readTokenContainer(lexer));
            lexer.expectToken("}");
            nextPart = lexer.lexParameterTokenPart();
            if (nextPart == null) break;
            if (!nextPart.endsWith(":") && !nextPart.equals("\u2192"))
              throw new PlomReadException("Expecting identifier part to end with :", lexer); 
          }
          lexer.expectToken("}");
          return Token.ParameterToken.fromPartsWithoutPostfix(nameParts, sym, params);
        }
        else
        {
          lexer.expectToken("}");
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
