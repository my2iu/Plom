package org.programmingbasics.plom.core;

public abstract class Token
{
   public abstract <S> S visit(TokenVisitor<S> visitor);
   
   public static class SimpleToken extends Token
   {
      String contents;
      public SimpleToken(String contents)
      {
         this.contents = contents;
      }
      public <S> S visit(TokenVisitor<S> visitor)
      {
         return visitor.visitSimpleToken(this);
      }
   }
   
   static interface TokenVisitor<S>
   {
      public S visitSimpleToken(SimpleToken token);
   }
}
