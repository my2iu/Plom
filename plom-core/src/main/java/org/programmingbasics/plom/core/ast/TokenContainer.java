package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

public class TokenContainer
{
   public List<Token> tokens = new ArrayList<>();
   
   public TokenContainer()
   {
      
   }
   
   public TokenContainer(List<Token> contents)
   {
      this.tokens.addAll(contents);
   }
   
   public TokenContainer(Token... initialTokens)
   {
     for (Token tok: initialTokens)
       tokens.add(tok);
   }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TokenContainer other = (TokenContainer) obj;
    if (tokens == null)
    {
      if (other.tokens != null) return false;
    }
    else if (!tokens.equals(other.tokens)) return false;
    return true;
  }
}
