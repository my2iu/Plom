package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/** 
 * Stores information about a parsed type (but just the raw text
 * for the type and not actual references to the real types)
 */
public class UnboundType
{
  public Token.ParameterToken mainToken;
  public static UnboundType fromToken(Token tok)
  {
    UnboundType type = new UnboundType();
    type.mainToken = (Token.ParameterToken)tok;
    return type;
  }
  public static UnboundType forClassLookupName(String name)
  {
    return UnboundType.fromToken(Token.ParameterToken.fromContents("@" + name, Symbol.AtType)); 
  }
}