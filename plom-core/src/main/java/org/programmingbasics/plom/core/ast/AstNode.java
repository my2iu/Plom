package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public <U, V> void recursiveVisit(VisitorTriggers<U, V> triggers, AstNode node, U param1, V param2)
  {
    // See if we have a visitor registered for this production rule of symbols
    RecursiveWalkerVisitor<U, V> match = triggers.get(node.symbols);
    if (match != null)
      match.visit(triggers, node, param1, param2);
    else
      // Visit the children
      recursiveVisitChildren(triggers, node, param1, param2);
  }
  
  public <U, V> void recursiveVisitChildren(VisitorTriggers<U, V> triggers, AstNode node, U param1, V param2)
  {
    for (AstNode child: node.children)
      recursiveVisit(triggers, child, param1, param2);
  }

  public static class VisitorTriggers<U, V> extends HashMap<List<Symbol>, RecursiveWalkerVisitor<U, V>>
  {
    private static final long serialVersionUID = 1L;
    public VisitorTriggers<U, V> add(List<Symbol> match, RecursiveWalkerVisitor<U, V> callback)
    {
      put(match, callback);
      return this;
    }
  }
  
  /** 
   * Defines a lambda that is triggered when the recursive walker hits
   * a node of interest.
   */
  @FunctionalInterface public static interface RecursiveWalkerVisitor<U, V>
  {
    public void visit(VisitorTriggers<U, V> triggers, AstNode node, U param1, V param2);
  }
}
