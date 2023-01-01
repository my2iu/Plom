package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import jsinterop.annotations.JsType;

/** 
 * Stores information about a parsed type (but just the raw text
 * for the type and not actual references to the real types)
 */
@JsType
public class UnboundType
{
  public Token.ParameterToken mainToken;
  public TokenContainer returnType;  // For function types with a return type
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
  public static UnboundType forSimpleFunctionType(String returnClassName, String name, String...argClassNames)
  {
    UnboundType funType = new UnboundType();
    funType.returnType = new TokenContainer(Token.ParameterToken.fromContents("@" + returnClassName, Symbol.AtType));
    TokenContainer[] args = new TokenContainer[argClassNames.length];
    for (int n = 0; n < argClassNames.length; n++)
    {
      args[n] = new TokenContainer();
      args[n].tokens.add(Token.ParameterToken.fromContents(".arg" + n, Symbol.DotVariable));
      args[n].tokens.add(Token.ParameterToken.fromContents("@" + argClassNames[n], Symbol.AtType));
    }
    funType.mainToken = Token.ParameterToken.fromContents("f@" + name, Symbol.FunctionTypeName, args);
    return funType;
  }
  public UnboundType copy()
  {
    UnboundType newType = new UnboundType();
    newType.mainToken = mainToken.copy();
    return newType;
  }
}