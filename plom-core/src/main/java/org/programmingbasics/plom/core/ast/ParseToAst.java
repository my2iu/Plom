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
  }
  
  public AstNode parse(Symbol base) throws ParseException
  {
    Symbol sym = peekNextTokenType();
    if (base.isTerminal())
    {
      if (sym != base)
        throw new ParseException();
      AstNode node = new AstTokenNode(readNextToken());
      return node;
    }

    if (parser.parsingTable.get(base) == null)
      throw new ParseException();
    Symbol[] expansion = parser.parsingTable.get(base).get(sym);
    if (expansion == null)
      throw new ParseException();
    AstProductionNode production = new AstProductionNode(base);
    for (Symbol expanded: expansion)
      production.prodRule.add(expanded);
    for (Symbol expanded: production.prodRule)
      production.expansion.add(parse(expanded));
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
