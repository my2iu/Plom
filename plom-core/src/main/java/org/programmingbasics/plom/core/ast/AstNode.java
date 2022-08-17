package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.programmingbasics.plom.core.ast.gen.Symbol;

import com.google.gwt.core.shared.GwtIncompatible;

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
   * Some tokens have contents that should be separately parsed
   */
  public List<AstNode> internalChildren;
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

  /**
   * Goes through a node and its children (but not its internal 
   * children) and tries to find a Token (used for finding a concrete
   * line number for a parse node)
   */
  public Token scanForToken()
  {
    if (token != null) return token;
    for (AstNode node: children)
    {
      Token toReturn = node.scanForToken();
      if (toReturn != null) return toReturn;
    }
    return null;
  }

  /**
   * Gather all the tokens that form the tree rooted at a node 
   */
  public void gatherTokens(List<Token> tokens)
  {
    if (token != null) tokens.add(token);
    for (AstNode node: children)
    {
      node.gatherTokens(tokens);
    }
  }
  
  public boolean matchesRule(List<Symbol> rule)
  {
    return symbols.equals(rule);
  }

  @GwtIncompatible
  public String getDebugTreeString(int indent)
  {
    String toReturn = "";
    for (int n = 0; n < indent; n++)
      toReturn += " ";
    for (int n = 0; n < symbols.size(); n++)
    {
      if (n != 0)
        toReturn += " ";
      toReturn += symbols.get(n).toString(); 
    }
    toReturn += "\n";
    for (AstNode child: children)
      toReturn += child.getDebugTreeString(indent + 1); 
    return toReturn;
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
    {
      if (child != null)
        child.recursiveVisit(triggers, param1, param2);
    }
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
