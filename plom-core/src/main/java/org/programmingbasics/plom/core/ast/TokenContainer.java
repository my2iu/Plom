package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

public class TokenContainer
{
   public List<Token> tokens = new ArrayList<>();
   
   TokenContainer()
   {
      
   }
   
   public TokenContainer(List<Token> contents)
   {
      this.tokens.addAll(contents);
   }
}
