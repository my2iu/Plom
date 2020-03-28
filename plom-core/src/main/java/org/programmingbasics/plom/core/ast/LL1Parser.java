package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class LL1Parser implements TokenVisitor<Void>
{
   public List<Symbol> stack = new ArrayList<>();
   public boolean isError = false;
   Parser parser = new Parser();
   {
   }
   @Override public Void visitSimpleToken(SimpleToken token)
   {
      if (isError) return null;
      if (stack.isEmpty())
      {
         isError = true;
         return null;
      }
      Symbol topOfStack = stack.get(stack.size() - 1);
      if (parser.parsingTable.get(topOfStack) == null)
      {
         isError = true;
         return null;
      }
      while (topOfStack != token.type)
      {
         Symbol[] expansion = parser.parsingTable.get(topOfStack).get(token.type);
         if (expansion == null)
         {
            isError = true;
            return null;
         }
         stack.remove(stack.size() - 1);
         for (int n = expansion.length - 1; n >= 0; n--)
         {
            stack.add(expansion[n]);
         }
         topOfStack = stack.get(stack.size() - 1);
      }
      
      stack.remove(stack.size() - 1);
      return null;
   }
   
   public Set<Symbol> allowedNextSymbols()
   {
      Set<Symbol> allowed = new HashSet<>();
      if (isError) return allowed;
      if (stack.isEmpty()) return allowed;
      Symbol topOfStack = stack.get(stack.size() - 1);
      if (parser.parsingTable.get(topOfStack) == null) return allowed;
      allowed.addAll(parser.parsingTable.get(topOfStack).keySet());
      return allowed;
   }

}
