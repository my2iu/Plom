package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class LL1Parser implements TokenVisitor<Void>
{
  public List<Symbol> stack = new ArrayList<>();
  public boolean isError = false;
  Parser parser = new Parser();
  {
  }
  @Override public Void visitSimpleToken(SimpleToken token)
  {
    if (isError) return null;
    if (!matchSymbol(stack, parser, token.type))
      isError = true;
    return null;
  }

  @Override
  public Void visitWideToken(WideToken token)
  {
    if (isError) return null;
    if (!matchSymbol(stack, parser, token.type))
      isError = true;
    return null;
  }

  @Override
  public Void visitOneBlockToken(OneBlockToken token)
  {
    return visitWideToken(token);
  }

  @Override
  public Void visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token)
  {
    return visitWideToken(token);
  }

  /**
   * Tries to parse a single symbol using the stack given
   * @return false on error
   */
  static boolean matchSymbol(List<Symbol> stack, Parser parser, Symbol sym)
  {
    if (stack.isEmpty())
    {
      return false;
    }
    Symbol topOfStack = stack.get(stack.size() - 1);
    while (topOfStack != sym)
    {
      if (parser.parsingTable.get(topOfStack) == null)
        return false;
      Symbol[] expansion = parser.parsingTable.get(topOfStack).get(sym);
      if (expansion == null)
      {
        return false;
      }
      stack.remove(stack.size() - 1);
      for (int n = expansion.length - 1; n >= 0; n--)
      {
        stack.add(expansion[n]);
      }
      topOfStack = stack.get(stack.size() - 1);
    }

    stack.remove(stack.size() - 1);
    return true;
  }

  /** Returns a set of symbols that could be allowed in this context, but
   * doesn't actually try to parse the symbols to see if it's valid for
   * this particular context.
   */
  public Set<Symbol> allowedNextSymbols()
  {
    Set<Symbol> allowed = new HashSet<>();
    if (isError) return allowed;
    if (stack.isEmpty()) return allowed;
    Symbol topOfStack = stack.get(stack.size() - 1);
    if (topOfStack.isTerminal()) allowed.add(topOfStack);
    if (parser.parsingTable.get(topOfStack) == null) return allowed;
    // Go through each possible token and run through a sample parse to
    // see if it resolves to something useful.
    return parser.parsingTable.get(topOfStack).keySet();
  }

  /**
   * Peeks ahead by trying to parse the given symbol and returning true
   * if it parses correctly.
   */
  public boolean peekParseSymbol(Symbol sym)
  {
    List<Symbol> stackCopy = new ArrayList<>(stack);
    if (matchSymbol(stackCopy, parser, sym))
      return true;
    return false;
  }

}
