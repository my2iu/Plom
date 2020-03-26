package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

public class TokenContainer
{
   List<Token> tokens = new ArrayList<>();
   
   TokenContainer()
   {
      
   }
   
   TokenContainer(List<Token> contents)
   {
      this.tokens.addAll(contents);
   }
}
