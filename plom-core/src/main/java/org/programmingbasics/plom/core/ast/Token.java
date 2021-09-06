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
   public abstract <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E;
   public abstract <S, T, U, V, W> S visit(TokenVisitor4<S, T, U, V, W> visitor, T param1, U param2, V param3, W param4);
   public abstract <S, T, U, V, W, X> S visit(TokenVisitor5<S, T, U, V, W, X> visitor, T param1, U param2, V param3, W param4, X param5);
   public abstract <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E;
   public boolean isWide() { return false; }
   public abstract Token copy();
   
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
      @Override public Token copy() { return new SimpleToken(contents, type); }
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
      public <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E
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
      public <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E
      {
        return visitor.visitSimpleToken(this);
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
     @Override public ParameterToken copy()
     {
       ParameterToken token = new ParameterToken(contents, postfix, type);
       for (int n = 0; n < parameters.size(); n++)
         token.parameters.set(n, parameters.get(n).copy());
       return token;
     }
     public static ParameterToken fromContents(String name, Symbol type, TokenContainer... params)
     {
       List<String> parts = splitVarAtColons(name);
       String postfix = splitVarAtColonsForPostfix(name);
       ParameterToken tok = new ParameterToken(parts, postfix, type);
       for (int n = 0; n < params.length; n++)
         tok.parameters.set(n, params[n]);
       return tok;
     }
     public void setContents(List<String> contents, String postfix)
     {
       this.contents = contents;
       this.postfix = postfix;
       while (parameters.size() < contents.size())
         parameters.add(new TokenContainer());
       while (parameters.size() > contents.size())
         parameters.remove(parameters.size() - 1);
     }
     public String getLookupName()
     {
       if (contents.isEmpty())
         return postfix.substring(1);
       return (String.join("", contents) + postfix).substring(1);
     }
     @Override public Symbol getType() { return type; }
     @Override public String getTextContent() { return String.join("", contents) + postfix; }
     public static List<String> splitVarAtColons(String val)
     {
       if (!val.contains(":"))
         return new ArrayList<>();
       List<String> params = new ArrayList<>();
       while (val.indexOf(':') >= 0)
       {
         params.add(val.substring(0, val.indexOf(':') + 1));
         val = val.substring(val.indexOf(':') + 1);
       }
       if (!val.isEmpty())
         params.add(val);
       return params;
     }
     public static String splitVarAtColonsForPostfix(String val)
     {
       if (!val.contains(":"))
         return val;
       return "";
     }
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
     public <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E
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
     public <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E
     {
       return visitor.visitParameterToken(this);
     }
    @Override
     public int hashCode()
     {
       final int prime = 31;
       int result = 1;
       result = prime * result + ((contents == null) ? 0 : contents.hashCode());
       result = prime * result
           + ((parameters == null) ? 0 : parameters.hashCode());
       result = prime * result + ((postfix == null) ? 0 : postfix.hashCode());
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
       if (parameters == null)
       {
         if (other.parameters != null) return false;
       }
       else if (!parameters.equals(other.parameters)) return false;
       if (postfix == null)
       {
         if (other.postfix != null) return false;
       }
       else if (!postfix.equals(other.postfix)) return false;
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
      @Override public WideToken copy()
      {
        throw new IllegalArgumentException("Not implemented");
      }
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
      public <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E
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
      public <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E
      {
        return visitor.visitWideToken(this);
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
     public <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E
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
     public <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E
     {
       return visitor.visitOneBlockToken(this);
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
      public <S, T, U, V, E extends Throwable> S visit(TokenVisitor3Err<S, T, U, V, E> visitor, T param1, U param2, V param3) throws E
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
      public <S, E extends Throwable> S visit(TokenVisitorErr<S, E> visitor) throws E
      {
        return visitor.visitOneExpressionOneBlockToken(this);
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

   public static interface TokenVisitorErr<S, E extends Throwable>
   {
      public S visitSimpleToken(SimpleToken token) throws E;
      public S visitParameterToken(ParameterToken token) throws E;
      public S visitWideToken(WideToken token) throws E;
      public S visitOneBlockToken(OneBlockToken token) throws E;
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token) throws E;
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
   public static interface TokenVisitor3Err<S, T, U, V, E extends Throwable>
   {
      public S visitSimpleToken(SimpleToken token, T param1, U param2, V param3) throws E;
      public S visitParameterToken(ParameterToken token, T param1, U param2, V param3) throws E;
      public S visitWideToken(WideToken token, T param1, U param2, V param3) throws E;
      public S visitOneBlockToken(OneBlockToken token, T param1, U param2, V param3) throws E;
      public S visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, T param1, U param2, V param3) throws E;
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
