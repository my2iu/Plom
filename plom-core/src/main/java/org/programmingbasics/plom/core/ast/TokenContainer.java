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

   public boolean endsWithWideToken()
   {
     if (tokens.isEmpty()) return false;
     return tokens.get(tokens.size() - 1).isWide();
   }

   public boolean endWithNonWideToken()
   {
     if (tokens.isEmpty()) return false;
     return !tokens.get(tokens.size() - 1).isWide();
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
