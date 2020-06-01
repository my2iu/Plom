package org.programmingbasics.plom.core.ast;

import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * A production rule that was identified by the parser.
 */
public class AstTokenNode extends AstNode
{
  public AstTokenNode(Token token)
  {
    this.token = token;
    if (token instanceof Token.TokenWithSymbol)
      baseSymbol = ((Token.TokenWithSymbol)token).getType();
    else
      baseSymbol = null;
  }
  public final Token token;
  public final Symbol baseSymbol;
  
  public Symbol getSymbol()
  {
    return baseSymbol;
  }

}
