package org.programmingbasics.plom.core.ast;

import java.util.List;

import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class ParseToAst 
{
  Parser parser = new Parser();
  List<Token> tokens;
  Symbol endSymbol;
  int idx = 0;
  
  public ParseToAst(List<Token> tokens, Symbol endSymbol)
  {
    this.tokens = tokens;
    this.endSymbol = endSymbol;
  }

  public static class ParseException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }
  
  public AstNode parse(Symbol base) throws ParseException
  {
    Symbol sym = peekNextTokenType();
    if (base.isTerminal())
    {
      if (sym != base)
        throw new ParseException();
      AstNode node = AstNode.fromToken(readNextToken());
      return node;
    }

    if (parser.parsingTable.get(base) == null)
      throw new ParseException();
    Symbol[] expansion = parser.parsingTable.get(base).get(sym);
    if (expansion == null)
      throw new ParseException();
    AstNode production = new AstNode(base);
    for (Symbol expanded: expansion)
      production.symbols.add(expanded);
    for (Symbol expanded: expansion)
      production.children.add(parse(expanded));
    return production;
  }
  
  Symbol peekNextTokenType()
  {
    if (idx < tokens.size())
      return ((Token.TokenWithSymbol)tokens.get(idx)).getType();
    return endSymbol; 
  }
  
  Token readNextToken()
  {
    idx++;
    return tokens.get(idx - 1);
  }
}
