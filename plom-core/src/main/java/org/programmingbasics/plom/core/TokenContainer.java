package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.Token;

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
