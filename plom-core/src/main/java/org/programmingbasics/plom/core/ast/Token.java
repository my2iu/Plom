package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

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
   
   public static interface TokenWithSymbol
   {
     public Symbol getType();
   }
   public static interface TokenWithEditableTextContent
   {
     public String getTextContent();
   }
   
   public static class SimpleToken extends Token implements TokenWithSymbol, TokenWithEditableTextContent
   {
      public String contents;
      public final Symbol type;
      public SimpleToken(String contents, Symbol type)
      {
         this.contents = contents;
         this.type = type;
      }
      @Override public Symbol getType() { return type; }
      @Override public String getTextContent() { return contents; }
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
      @Override
      public int hashCode()
      {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((contents == null) ? 0 : contents.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
      }
      @Override
      public boolean equals(Object obj)
      {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SimpleToken other = (SimpleToken) obj;
        if (contents == null)
        {
          if (other.contents != null) return false;
        }
        else if (!contents.equals(other.contents)) return false;
        if (type != other.type) return false;
        return true;
      }
   }
   
   // Token that may have parameters for it (used for variable identifiers
   // and method calls)
   public static class ParameterToken extends Token implements TokenWithSymbol, TokenWithEditableTextContent
   {
     public List<String> contents;
     public String postfix;
     public final Symbol type;
     public List<TokenContainer> parameters;
     public ParameterToken(List<String> contents, String postfix, Symbol type)
     {
        this.contents = contents;
        parameters = new ArrayList<>();
        for (int n = 0; n < contents.size(); n++)
          parameters.add(new TokenContainer());
        this.postfix = postfix;
        this.type = type;
     }
     @Override public Symbol getType() { return type; }
     @Override public String getTextContent() { return String.join("", contents); }
     public <S> S visit(TokenVisitor<S> visitor)
     {
        return visitor.visitParameterToken(this);
     }
     public <S, T> S visit(TokenVisitor1<S, T> visitor, T param1)
     {
        return visitor.visitParameterToken(this, param1);
     }
     public <S, T, U> S visit(TokenVisitor2<S, T, U> visitor, T param1, U param2)
     {
        return visitor.visitParameterToken(this, param1, param2);
     }
     public <S, T, U, V> S visit(TokenVisitor3<S, T, U, V> visitor, T param1, U param2, V param3)
     {
        return visitor.visitParameterToken(this, param1, param2, param3);
     }
     public <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4)
     {
        return visitor.visitParameterToken(this, param1, param2, param3, param4);
     }
     public <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5)
     {
        return visitor.visitParameterToken(this, param1, param2, param3, param4, param5);
     }
     @Override
     public int hashCode()
     {
       final int prime = 31;
       int result = 1;
       result = prime * result + ((contents == null) ? 0 : contents.hashCode());
       result = prime * result + ((type == null) ? 0 : type.hashCode());
       return result;
     }
     @Override
     public boolean equals(Object obj)
     {
       if (this == obj) return true;
       if (obj == null) return false;
       if (getClass() != obj.getClass()) return false;
       ParameterToken other = (ParameterToken) obj;
       if (contents == null)
       {
         if (other.contents != null) return false;
       }
       else if (!contents.equals(other.contents)) return false;
       if (type != other.type) return false;
       return true;
     }
   }

   /** A token that is rendered to take up the full-width of the display */
   public static class WideToken extends Token implements TokenWithSymbol, TokenWithEditableTextContent
   {
      public String contents;
      public Symbol type;
      public WideToken(String contents, Symbol type)
      {
         this.contents = contents;
         this.type = type;
      }
      public boolean isWide() { return true; }
      @Override public Symbol getType() { return type; }
      @Override public String getTextContent() { return contents; }
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
      @Override
      public int hashCode()
      {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((contents == null) ? 0 : contents.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
      }
      @Override
      public boolean equals(Object obj)
      {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WideToken other = (WideToken) obj;
        if (contents == null)
        {
          if (other.contents != null) return false;
        }
        else if (!contents.equals(other.contents)) return false;
        if (type != other.type) return false;
        return true;
      }
   }
   
   public static class OneBlockToken extends WideToken
   {
     public StatementContainer block = new StatementContainer();
     public OneBlockToken(String contents, Symbol type)
     {
       super(contents, type);
     }
     public OneBlockToken(String contents, Symbol type, StatementContainer block)
     {
       super(contents, type);
       this.block = block;
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
    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((block == null) ? 0 : block.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      OneBlockToken other = (OneBlockToken) obj;
      if (block == null)
      {
        if (other.block != null) return false;
      }
      else if (!block.equals(other.block)) return false;
      return true;
    }
   }

   public static class OneExpressionOneBlockToken extends OneBlockToken
   {
      public TokenContainer expression = new TokenContainer();
      public OneExpressionOneBlockToken(String contents, Symbol type)
      {
        super(contents, type);
      }
      public OneExpressionOneBlockToken(String contents, Symbol type, TokenContainer expression, StatementContainer block)
      {
        super(contents, type, block);
        this.expression = expression;
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
      @Override
      public int hashCode()
      {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((expression == null) ? 0 : expression.hashCode());
        return result;
      }
      @Override
      public boolean equals(Object obj)
      {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        OneExpressionOneBlockToken other = (OneExpressionOneBlockToken) obj;
        if (expression == null)
        {
          if (other.expression != null) return false;
        }
        else if (!expression.equals(other.expression)) return false;
        return true;
      }
   }
   
   public static interface TokenVisitor<S>
   {
      public S visitSimpleToken(SimpleToken token);
      public S visitParameterToken(ParameterToken token);
      public S visitWideToken(WideToken token);
      public S visitOneBlockToken(OneBlockToken token);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token);
   }
   
   public static interface TokenVisitor1<S, T>
   {
      public S visitSimpleToken(SimpleToken token, T param1);
      public S visitParameterToken(ParameterToken token, T param1);
      public S visitWideToken(WideToken token, T param1);
      public S visitOneBlockToken(OneBlockToken token, T param1);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1);
   }
   
   public static interface TokenVisitor2<S, T, U>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2);
      public S visitParameterToken(ParameterToken token, T param1, U param2);
      public S visitWideToken(WideToken token, T param1, U param2);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2);
   }
   public static interface TokenVisitor3<S, T, U, V>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3);
      public S visitParameterToken(ParameterToken token, T param1, U param2, V param3);
      public S visitWideToken(WideToken token, T param1, U param2, V param3);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3);
   }
   public static interface TokenVisitor4<S, T, U, V, W>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3, W param4);
      public S visitParameterToken(ParameterToken token, T param1, U param2, V param3, W param4);
      public S visitWideToken(WideToken token, T param1, U param2, V param3, W param4);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3, W param4);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3, W param4);
   }
   public static interface TokenVisitor5<S, T, U, V, W, X>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitParameterToken(ParameterToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitWideToken(WideToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3, W param4, X param5);
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3, W param4, X param5);
   }

}
