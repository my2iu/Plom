package org.programmingbasics.plom.core.ast;

import org.programmingbasics.plom.core.ast.gen.Symbol;

public abstract class Token
{
   public abstract <S> S visit(TokenVisitor<S> visitor);
   
   public static class SimpleToken extends Token
   {
      public String contents;
      Symbol type;
      public SimpleToken(String contents, Symbol type)
      {
         this.contents = contents;
         this.type = type;
      }
      public <S> S visit(TokenVisitor<S> visitor)
      {
         return visitor.visitSimpleToken(this);
      }
   }
   
   public static class OneExpressionOneBlockToken extends Token
   {
      public String contents;
      Symbol type;
      public TokenContainer expression = new TokenContainer();
      public StatementContainer block = new StatementContainer();
      public OneExpressionOneBlockToken(String contents, Symbol type)
      {
         this.contents = contents;
         this.type = type;
      }
      public <S> S visit(TokenVisitor<S> visitor)
      {
         return visitor.visitOneExpressionOneBlockToken(this);
      }
   }
   
   public static interface TokenVisitor<S>
   {
      public S visitSimpleToken(SimpleToken token);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token);
   }
}
