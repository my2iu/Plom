package org.programmingbasics.plom.core.ast;

import org.programmingbasics.plom.core.ast.gen.Symbol;

public abstract class Token
{
   public abstract <S> S visit(TokenVisitor<S> visitor);
   public abstract <S, T> S visit(TokenVisitor1<S, T> visitor, T param1);
   public abstract <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2);
   public abstract <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3);
   public abstract <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4);
   public abstract <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5);
   public boolean isWide() { return false; }
   
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
      public <S, T> S visit(TokenVisitor1<S, T> visitor, T param1)
      {
         return visitor.visitSimpleToken(this, param1);
      }
      public <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2)
      {
         return visitor.visitSimpleToken(this, param1, param2);
      }
      public <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3)
      {
         return visitor.visitSimpleToken(this, param1, param2, param3);
      }
      public <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4)
      {
         return visitor.visitSimpleToken(this, param1, param2, param3, param4);
      }
      public <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5)
      {
         return visitor.visitSimpleToken(this, param1, param2, param3, param4, param5);
      }
   }

   /** A token that is rendered to take up the full-width of the display */
   public static class WideToken extends Token
   {
      public String contents;
      Symbol type;
      public WideToken(String contents, Symbol type)
      {
         this.contents = contents;
         this.type = type;
      }
      public boolean isWide() { return true; }
      public <S> S visit(TokenVisitor<S> visitor)
      {
         return visitor.visitWideToken(this);
      }
      public <S, T> S visit(TokenVisitor1<S, T> visitor, T param1)
      {
         return visitor.visitWideToken(this, param1);
      }
      public <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2)
      {
         return visitor.visitWideToken(this, param1, param2);
      }
      public <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3)
      {
         return visitor.visitWideToken(this, param1, param2, param3);
      }
      public <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4)
      {
         return visitor.visitWideToken(this, param1, param2, param3, param4);
      }
      public <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5)
      {
         return visitor.visitWideToken(this, param1, param2, param3, param4, param5);
      }
   }
   
   public static class OneBlockToken extends WideToken
   {
     public StatementContainer block = new StatementContainer();
     public OneBlockToken(String contents, Symbol type)
     {
       super(contents, type);
     }
     public <S> S visit(TokenVisitor<S> visitor)
     {
        return visitor.visitOneBlockToken(this);
     }
     public <S, T> S visit(TokenVisitor1<S, T> visitor, T param1)
     {
        return visitor.visitOneBlockToken(this, param1);
     }
     public <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2)
     {
        return visitor.visitOneBlockToken(this, param1, param2);
     }
     public <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3)
     {
        return visitor.visitOneBlockToken(this, param1, param2, param3);
     }
     public <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4)
     {
        return visitor.visitOneBlockToken(this, param1, param2, param3, param4);
     }
     public <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5)
     {
        return visitor.visitOneBlockToken(this, param1, param2, param3, param4, param5);
     }
   }

   public static class OneExpressionOneBlockToken extends OneBlockToken
   {
      public TokenContainer expression = new TokenContainer();
      public OneExpressionOneBlockToken(String contents, Symbol type)
      {
        super(contents, type);
      }
      public <S> S visit(TokenVisitor<S> visitor)
      {
         return visitor.visitOneExpressionOneBlockToken(this);
      }
      public <S, T> S visit(TokenVisitor1<S, T> visitor, T param1)
      {
         return visitor.visitOneExpressionOneBlockToken(this, param1);
      }
      public <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2)
      {
         return visitor.visitOneExpressionOneBlockToken(this, param1, param2);
      }
      public <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3)
      {
         return visitor.visitOneExpressionOneBlockToken(this, param1, param2, param3);
      }
      public <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4)
      {
         return visitor.visitOneExpressionOneBlockToken(this, param1, param2, param3, param4);
      }
      public <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5)
      {
         return visitor.visitOneExpressionOneBlockToken(this, param1, param2, param3, param4, param5);
      }
   }
   
   public static interface TokenVisitor<S>
   {
      public S visitSimpleToken(SimpleToken token);
      public S visitWideToken(WideToken token);
      public S visitOneBlockToken(OneBlockToken token);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token);
   }
   
   public static interface TokenVisitor1<S, T>
   {
      public S visitSimpleToken(SimpleToken token, T param1);
      public S visitWideToken(WideToken token, T param1);
      public S visitOneBlockToken(OneBlockToken token, T param1);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1);
   }
   
   public static interface TokenVisitor2<S, T, U>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2);
      public S visitWideToken(WideToken token, T param1, U param2);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2);
   }
   public static interface TokenVisitor3<S, T, U, V>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3);
      public S visitWideToken(WideToken token, T param1, U param2, V param3);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3);
   }
   public static interface TokenVisitor4<S, T, U, V, W>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3, W param4);
      public S visitWideToken(WideToken token, T param1, U param2, V param3, W param4);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3, W param4);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3, W param4);
   }
   public static interface TokenVisitor5<S, T, U, V, W, X>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitWideToken(WideToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3, W param4, X param5);
   }

}
