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
  public static String escapeParameterTokenPart(String str)
  {
    return str;
  }
  
  public static String escapeComment(String str)
  {
    return str.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
  }
  
  public static String escapeStringLiteral(String str)
  {
    return str;
  }

  public static String escapeTemplateLiteral(String str)
  {
    return str.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$");
  }

  static final Map<Symbol, String> symbolTokenMap = new HashMap<>();
  static {
    symbolTokenMap.put(Symbol.This, "this");
    symbolTokenMap.put(Symbol.Super, "super");
    symbolTokenMap.put(Symbol.NullLiteral, "null");
    symbolTokenMap.put(Symbol.TrueLiteral, "true");
    symbolTokenMap.put(Symbol.FalseLiteral, "false");
    symbolTokenMap.put(Symbol.DotVariable, ".");
    symbolTokenMap.put(Symbol.AtType, "@");
    symbolTokenMap.put(Symbol.FunctionTypeName, "f@");
    symbolTokenMap.put(Symbol.Returns, "returns");
    symbolTokenMap.put(Symbol.FunctionLiteral, "lambda");
    symbolTokenMap.put(Symbol.Var, "var");
//    symbolTokenMap.put(Symbol.Colon, ":");
    symbolTokenMap.put(Symbol.Assignment, ":=");
    symbolTokenMap.put(Symbol.Retype, "retype");
    symbolTokenMap.put(Symbol.As, "as");
    symbolTokenMap.put(Symbol.Is, "is");
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
    symbolTokenMap.put(Symbol.In, "in");
    symbolTokenMap.put(Symbol.Break, "break");
    symbolTokenMap.put(Symbol.Continue, "continue");
    symbolTokenMap.put(Symbol.DUMMY_COMMENT, "//");
    symbolTokenMap.put(Symbol.COMPOUND_COMMENT, "/*");
    symbolTokenMap.put(Symbol.COMPOUND_IF, "if");
    symbolTokenMap.put(Symbol.COMPOUND_FOR, "for");
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

  public static class PlomCodeOutputFormatter
  {
    public PlomCodeOutputFormatter(StringBuilder out)
    {
      this.out = out;
    }
    StringBuilder out;
    public PlomCodeOutputFormatter append(String str)
    {
      out.append(str);
      return this;
    }
    public PlomCodeOutputFormatter newline()
    {
      out.append("\n");
      return this;
    }
    public PlomCodeOutputFormatter token(String str)
    {
      out.append(" ");
      out.append(str);
      return this;
    }
  }
  
  public static void writeToken(PlomCodeOutputFormatter out, Token tok) throws IOException
  {
    tok.visit(new Token.TokenVisitorErr<Void, IOException>() {
      @Override public Void visitSimpleToken(SimpleToken token) throws IOException
      {
        switch (token.getType())
        {
        case String:
          out.append("\"");
          out.append(escapeStringLiteral(token.contents.substring(1, token.contents.length() - 1)));
          out.append("\"");
          break;
        case Number:
          out.token(token.contents);
          break;
        default:
          out.token(symbolTokenMap.get(token.getType()));
        }
        return null;
      }

      @Override public Void visitParameterToken(ParameterToken token) throws IOException
      {
        writeParameterTokenStart(out, token);
        for (int n = 0; n < token.contents.size(); n++)
        {
          writeParameterTokenParamStart(out, token, n);
          writeTokenContainer(out, token.parameters.get(n));
          writeParameterTokenParamEnd(out);
        }
        writeParameterTokenEnd(out, token);
        return null;
      }

      @Override public Void visitWideToken(WideToken token) throws IOException
      {
        out.token(symbolTokenMap.get(token.getType()));
        if (token.getType() != Symbol.DUMMY_COMMENT)
          throw new IllegalArgumentException("Comments are the only wide comments that can be written out right now");
        out.append(escapeComment(token.contents.substring(2)));
        out.newline();
        return null;
      }

      @Override public Void visitOneBlockToken(OneBlockToken token) throws IOException
      {
        writeBlockTokenFirstLine(out, token, true);
        writeStatementContainer(out, token.block);
        if (token.isInline())
          writeInlineBlockTokenEnd(out);
        else          
          writeBlockTokenEnd(out);
        return null;
      }

      @Override public Void visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token) throws IOException
      {
        writeBlockTokenFirstLine(out, token, false);
        writeTokenContainer(out, token.expression);
        writeBlockTokenExpressionToBlock(out);
        writeStatementContainer(out, token.block);
        if (token.isInline())
          writeInlineBlockTokenEnd(out);
        else          
          writeBlockTokenEnd(out);
        return null;
      }});
  }

  public static void writeParameterTokenStart(PlomCodeOutputFormatter out,
      ParameterToken token)
  {
    switch(token.getType())
    {
    case AtType:
      out.token("@");
      out.token("{");
      break;
    case FunctionTypeName:
      out.token("f@");
      out.token("{");
      break;
    case DotVariable:
      out.token(".");
      out.token("{");
      break;
    default:
      throw new IllegalArgumentException("Unknown token type");
    }
  }
  
  public static void writeParameterTokenEnd(PlomCodeOutputFormatter out,
      ParameterToken token)
  {
    if (token.contents.isEmpty())
    {
      if (token.getType() != Symbol.FunctionTypeName)
        out.append(escapeParameterTokenPart(token.postfix.substring(1)));
      else
        out.append(escapeParameterTokenPart(token.postfix.substring(2)));
    }
    else
      out.append(escapeParameterTokenPart(token.postfix));
    out.token("}");
  }

  public static void writeParameterTokenParamStart(PlomCodeOutputFormatter out,
      ParameterToken token, int n)
  {
    if (n == 0)
    {
      if (token.getType() != Symbol.FunctionTypeName)
        out.append(escapeParameterTokenPart(token.contents.get(n).substring(1)));
      else        
        out.append(escapeParameterTokenPart(token.contents.get(n).substring(2)));
    }
    else
      out.append(escapeParameterTokenPart(token.contents.get(n)));
    out.token("{");
  }

  public static void writeParameterTokenParamEnd(
      PlomCodeOutputFormatter out)
  {
    out.token("}");
  }

  public static void writeBlockTokenFirstLine(PlomCodeOutputFormatter out, Token.TokenWithSymbol token, boolean nextIsBlock) throws IOException
  {
    out.token(symbolTokenMap.get(token.getType()));
    out.token("{");
    if (nextIsBlock)
      out.newline();
  }

  public static void writeBlockTokenExpressionToBlock(PlomCodeOutputFormatter out)
  {
    out.token("}");
    out.token("{");
    out.newline();
  }
  
  public static void writeBlockTokenEnd(PlomCodeOutputFormatter out) throws IOException
  {
    out.token("}");
    out.newline();
  }

  public static void writeInlineBlockTokenEnd(PlomCodeOutputFormatter out) throws IOException
  {
    out.token("}");
  }

  public static void writeTokenContainer(PlomCodeOutputFormatter out, TokenContainer tokens) throws IOException
  {
    for (Token tok: tokens.tokens)
    {
      writeToken(out, tok);
    }
  }

  public static void writeStatementContainer(PlomCodeOutputFormatter out, StatementContainer container) throws IOException
  {
    for (TokenContainer tokens: container.statements)
    {
      writeTokenContainer(out, tokens);
      out.newline();
    }
  }
}
