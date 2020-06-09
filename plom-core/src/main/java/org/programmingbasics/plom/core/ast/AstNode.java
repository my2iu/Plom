package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Simple node for the abstract syntax tree
 */
public class AstNode
{
  /** 
   * First symbol is the symbol represented by the node, and the latter ones
   * are the expansion of the symbol (if applicable)
   */
  public List<Symbol> symbols = new ArrayList<>();
  /**
   * Any child AstNodes for the expansion of the symbol
   */
  public List<AstNode> children = new ArrayList<>();
  /**
   * Terminal symbols will have a token that matches the symbol
   */
  public Token token;
  
  public Symbol getSymbol()
  {
    return symbols.get(0);
  }
  
  public AstNode(Symbol baseSymbol)
  {
    symbols.add(baseSymbol);
  }
  
  public static AstNode fromToken(Token token)
  {
    if (!(token instanceof Token.TokenWithSymbol))
      return null;
    AstNode node = new AstNode(((Token.TokenWithSymbol)token).getType());
    node.token = token;
    return node;
  }
  
  public boolean matchesRule(List<Symbol> rule)
  {
    return symbols.equals(rule);
  }

  public <U, V, E extends Throwable> void recursiveVisit(VisitorTriggers<U, V, E> triggers, U param1, V param2) throws E
  {
    // See if we have a visitor registered for this production rule of symbols
    RecursiveWalkerVisitor<U, V, E> match = triggers.get(symbols);
    if (match != null)
      match.visit(triggers, this, param1, param2);
    else
      // Visit the children
      recursiveVisitChildren(triggers, param1, param2);
  }
  
  public <U, V, E extends Throwable> void recursiveVisitChildren(VisitorTriggers<U, V, E> triggers, U param1, V param2) throws E
  {
    for (AstNode child: children)
      child.recursiveVisit(triggers, param1, param2);
  }

  public static class VisitorTriggers<U, V, E extends Throwable> extends HashMap<List<Symbol>, RecursiveWalkerVisitor<U, V, E>>
  {
    private static final long serialVersionUID = 1L;
    public VisitorTriggers<U, V, E> add(List<Symbol> match, RecursiveWalkerVisitor<U, V, E> callback)
    {
      put(match, callback);
      return this;
    }
  }
  
  /** 
   * Defines a lambda that is triggered when the recursive walker hits
   * a node of interest. Return true if something was handled.
   */
  @FunctionalInterface public static interface RecursiveWalkerVisitor<U, V, E extends Throwable>
  {
    public boolean visit(VisitorTriggers<U, V, E> triggers, AstNode node, U param1, V param2) throws E;
  }
}
