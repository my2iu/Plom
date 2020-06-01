package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * A production rule that was identified by the parser.
 */
public class AstProductionNode extends AstNode
{
  AstProductionNode(Symbol baseSymbol)
  {
    this.baseSymbol = baseSymbol;
  }
  final Symbol baseSymbol;
  
  public Symbol getSymbol()
  {
    return baseSymbol;
  }
  public List<Symbol> prodRule = new ArrayList<>();

  public List<AstNode> expansion = new ArrayList<>();
}
