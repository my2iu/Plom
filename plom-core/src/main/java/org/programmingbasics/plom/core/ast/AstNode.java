package org.programmingbasics.plom.core.ast;

import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Simple node for the abstract syntax tree
 */
public abstract class AstNode
{
  public abstract Symbol getSymbol();
}
