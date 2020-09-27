package org.programmingbasics.plom.core.ast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Reading and writing code as text
 */
public class PlomTextWriter
{
  // We use { } as special symbols, so we need to escape them if they
  // appear in tokens and variables (though they shouldn't)
  public static String escape(String str)
  {
    return str;
  }
  
  public static String unescape(String str)
  {
    return str;
  }
  
  static final Map<Symbol, String> symbolTokenMap = new HashMap<>();
  static {
    symbolTokenMap.put(Symbol.This, "this");
    symbolTokenMap.put(Symbol.TrueLiteral, "true");
    symbolTokenMap.put(Symbol.FalseLiteral, "false");
    symbolTokenMap.put(Symbol.DotVariable, ".");
    symbolTokenMap.put(Symbol.AtType, "@");
    symbolTokenMap.put(Symbol.Var, "var");
    symbolTokenMap.put(Symbol.Colon, ":");
    symbolTokenMap.put(Symbol.Assignment, ":=");
    symbolTokenMap.put(Symbol.Lt, "<");
    symbolTokenMap.put(Symbol.Gt, ">");
    symbolTokenMap.put(Symbol.Le, "<=");
    symbolTokenMap.put(Symbol.Ge, ">=");
    symbolTokenMap.put(Symbol.Eq, "=");
    symbolTokenMap.put(Symbol.Ne, "!=");
    symbolTokenMap.put(Symbol.Or, "or");
    symbolTokenMap.put(Symbol.And, "and");
    symbolTokenMap.put(Symbol.Plus, "+");
    symbolTokenMap.put(Symbol.Minus, "-");
    symbolTokenMap.put(Symbol.Multiply, "*");
    symbolTokenMap.put(Symbol.Divide, "/");
    symbolTokenMap.put(Symbol.OpenParenthesis, "(");
    symbolTokenMap.put(Symbol.ClosedParenthesis, ")");
    symbolTokenMap.put(Symbol.Return, "return");
    symbolTokenMap.put(Symbol.PrimitivePassthrough, "primitive");
    symbolTokenMap.put(Symbol.DUMMY_COMMENT, "//");
    symbolTokenMap.put(Symbol.COMPOUND_IF, "if");
    symbolTokenMap.put(Symbol.COMPOUND_ELSE, "else");
    symbolTokenMap.put(Symbol.COMPOUND_ELSEIF, "elseif");
    symbolTokenMap.put(Symbol.COMPOUND_WHILE, "while");

    // Check coverage of symbol token map
    for (Symbol sym: Symbol.values())
    {
      if (!sym.isTerminal()) continue;
      if (symbolTokenMap.containsKey(sym))
        continue;
      switch (sym)
      {
      case DUMMY_BEGIN:
      case DUMMY_END:
      case EMPTY:
      case ASSEMBLED_STATEMENTS_BLOCK:
      case EndStatement:
      case String:
      case Number:
        // Symbols that are not handled or handled specially
        continue;
      default:
        throw new IllegalArgumentException("Missing mapping for symbol " + sym.name());
      }
    }
  }
  
  public void writeToken(StringBuilder out, Token tok) throws IOException
  {
    tok.visit(new Token.TokenVisitorErr<Void, IOException>() {
      @Override public Void visitSimpleToken(SimpleToken token) throws IOException
      {
        switch (token.getType())
        {
        case String:
          break;
        case Number:
          break;
        default:
          out.append(" ");
          out.append(symbolTokenMap.get(token.getType()));
        }
        return null;
      }

      @Override public Void visitParameterToken(ParameterToken token) throws IOException
      {
        switch(token.getType())
        {
        case AtType:
          out.append("@");
          out.append("{");
          out.append("}");
          break;
        case DotVariable:
          out.append(".");
          out.append("{");
          out.append("}");
          break;
        default:
          throw new IllegalArgumentException("Unknown token type");
        }
        return null;
      }

      @Override public Void visitWideToken(WideToken token) throws IOException
      {
        // TODO Auto-generated method stub
        return null;
      }

      @Override public Void visitOneBlockToken(OneBlockToken token) throws IOException
      {
        // TODO Auto-generated method stub
        return null;
      }

      @Override public Void visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token) throws IOException
      {
        // TODO Auto-generated method stub
        return null;
      }});
  }
  
}
