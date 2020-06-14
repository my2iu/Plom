package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class LL1Parser implements TokenVisitor<Void>
{
//  public List<Symbol> stack = new ArrayList<>();
  public List<ContextStack> stack = new ArrayList<>();
  public boolean isError = false;
  Parser parser = new Parser();
  {
  }

  /**
   * Just using the normal LL1 parsing stack simply tells us what
   * the next token to match is, but we also want additional context
   * about which productions we needed to use. That way, we can determine
   * whether the DotVariable is used in the context of being a variable
   * name or variable type or member access. (In a real parser, we would
   * use different terminal symbols/tokens for the different usages, but
   * we want to reuse the same tokens for Plom because these different 
   * usages look the same in the UI, and we want to let programmers cut&paste
   * tokens from one place to another, where the programmers would
   * expect the tokens to work if they look correct, even if the underlying
   * tokens are different).  
   */
  static class ContextStack
  {
    Symbol context;
    List<Symbol> toMatch = new ArrayList<>();
    public ContextStack copy() {
      ContextStack copy = new ContextStack();
      copy.context = context;
      copy.toMatch.addAll(toMatch);
      return copy;
    }
  }

  // Adds a symbol to match against onto the parsing stack
  public void addToParse(Symbol sym)
  {
    ContextStack context = new ContextStack();
    context.toMatch.add(sym);
    stack.add(context);
  }

  // For testing. Returns the next symbol in the parsing stack that we're trying to match against
  Symbol nextOnParsingStack(int offset)
  {
    int contextStackPos = stack.size() - 1;
    int toMatchPos = stack.get(contextStackPos).toMatch.size() - 1;
    while(offset > 0)
    {
      toMatchPos--;
      while (toMatchPos < 0)
      {
        contextStackPos--;
        toMatchPos = stack.get(contextStackPos).toMatch.size() - 1;
      }
      offset--;
    }
    return stack.get(contextStackPos).toMatch.get(toMatchPos);
  }
  Symbol topOfParsingStack()
  {
    return nextOnParsingStack(0);
  }

  private Void visitSymbol(Symbol tokenType)
  {
    if (isError) return null;
    if (!matchSymbol(stack, parser, tokenType, null))
      isError = true;
    return null;
  }

  @Override public Void visitSimpleToken(SimpleToken token) { return visitSymbol(token.type); }
  @Override public Void visitParameterToken(ParameterToken token) { return visitSymbol(token.type); }
  @Override public Void visitWideToken(WideToken token) { return visitSymbol(token.type); }
  @Override public Void visitOneBlockToken(OneBlockToken token) { return visitSymbol(token.type); }
  @Override public Void visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token) { return visitSymbol(token.type); }

  /**
   * Tries to parse a single symbol using the stack given. Also returns the symbols that needed
   * to be expanded to match the symbol if expandedSymbols is not null 
   * @return false on error
   */
  static boolean matchSymbol(List<ContextStack> stack, Parser parser, Symbol sym, List<Symbol> expandedSymbols)
  {
    if (stack.isEmpty())
    {
      return false;
    }
    ContextStack topContext = stack.get(stack.size() - 1);
    Symbol topOfStack = topContext.toMatch.get(topContext.toMatch.size() - 1);
    while (topOfStack != sym)
    {
      if (parser.parsingTable.get(topOfStack) == null)
        return false;
      Symbol[] expansion = parser.parsingTable.get(topOfStack).get(sym);
      if (expansion == null)
      {
        return false;
      }
      topContext.toMatch.remove(topContext.toMatch.size() - 1);
      if (expansion.length == 0)
      {
        while(stack.get(stack.size() - 1).toMatch.size() == 0)
          stack.remove(stack.size() - 1);
      }
      else
      {
        ContextStack newContextLevel = new ContextStack();
        newContextLevel.context = topOfStack;
        for (int n = expansion.length - 1; n >= 0; n--)
        {
          newContextLevel.toMatch.add(expansion[n]);
        }
        stack.add(newContextLevel);
      }
      topContext = stack.get(stack.size() - 1);
      topOfStack = topContext.toMatch.get(topContext.toMatch.size() - 1);
    }

    if (expandedSymbols != null)
    {
      for (ContextStack context: stack)
      {
        if (context.context != null)
          expandedSymbols.add(context.context);
      }
    }
    topContext.toMatch.remove(topContext.toMatch.size() - 1);
    while (stack.get(stack.size() - 1).toMatch.isEmpty())
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
    ContextStack topContext = stack.get(stack.size() - 1);
    Symbol topOfStack = topContext.toMatch.get(topContext.toMatch.size() - 1);
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
    List<ContextStack> stackCopy = new ArrayList<>();
    for (ContextStack context: stack)
      stackCopy.add(context.copy());
    if (matchSymbol(stackCopy, parser, sym, null))
      return true;
    return false;
  }

  /**
   * If a symbol if provided that matches what the grammar expects, then it
   * will return a list of the non-terminal symbols that were expanded to
   * match the symbol
   */
  public List<Symbol> peekExpandedSymbols(Symbol sym)
  {
    List<ContextStack> stackCopy = new ArrayList<>();
    for (ContextStack context: stack)
      stackCopy.add(context.copy());
    List<Symbol> expanded = new ArrayList<>();
    if (matchSymbol(stackCopy, parser, sym, expanded))
      return expanded;
    return null;
  }
}
