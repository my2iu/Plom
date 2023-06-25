package org.programmingbasics.plom.core.ast;

import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;
import org.programmingbasics.plom.core.ast.Token.WideToken;

/**
 * Finds a token in some code and returns the code position of that token.
 * Walks through all the tokens in the given code to see if token is there
 */
public class FindToken
{
  public static CodePosition tokenToPosition(Token token, StatementContainer codeList)
  {
    CodePosition pos = new CodePosition();
    if (inStatements(token, codeList, pos, 0))
      return pos;
    return null;
  }
  
  static boolean inStatements(Token token,
      StatementContainer stmtContainer,
      CodePosition pos, 
      int level)
  {
    for (int n = 0; n < stmtContainer.statements.size(); n++)
    {
      TokenContainer line = stmtContainer.statements.get(n);
      pos.setOffset(level, n);
      if (inLine(token, line, pos, level + 1))
        return true;
      pos.setMaxOffset(level + 1);
    }
    return false;
  }

  static boolean inLine(Token token, TokenContainer line,
      CodePosition pos, int level)
  {
    for (int n = 0; n < line.tokens.size(); n++)
    {
      Token tok = line.tokens.get(n);
      pos.setOffset(level, n);
      if (tok.visit(inToken, token, pos, level + 1))
        return true;
      pos.setMaxOffset(level + 1);
    }
    return false;
    
  }
  
  static FindInToken inToken = new FindInToken();
  
  static class FindInToken implements TokenVisitor3<Boolean, Token, CodePosition, Integer>
  {

    @Override
    public Boolean visitSimpleToken(SimpleToken token, Token target, CodePosition pos, Integer level)
    {
      if (token == target) return true;
      return false;
    }

    @Override
    public Boolean visitParameterToken(ParameterToken token, Token target, CodePosition pos, Integer level)
    {
      if (token == target) return true;
      pos.setOffset(level, CodePosition.PARAMTOK_POS_EXPRS);
      for (int n = 0; n < token.parameters.size(); n++)
      {
        pos.setOffset(level + 1, n);
        if (inLine(target, token.parameters.get(n), pos, level + 2))
          return true;
        pos.setMaxOffset(level + 2);
      }
      return false;
    }

    @Override
    public Boolean visitWideToken(WideToken token, Token target, CodePosition pos, Integer level)
    {
      if (token == target) return true;
      return false;
    }

    @Override
    public Boolean visitOneBlockToken(OneBlockToken token, Token target, CodePosition pos, Integer level)
    {
      if (token == target) return true;
      pos.setOffset(level, CodePosition.EXPRBLOCK_POS_BLOCK);
      if (inStatements(target, token.block, pos, level + 1))
        return true;
      pos.setMaxOffset(level + 1);
      return false;
    }

    @Override
    public Boolean visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token, Token target, CodePosition pos,
        Integer level)
    {
      if (token == target) return true;
      pos.setOffset(level, CodePosition.EXPRBLOCK_POS_EXPR);
      if (inLine(target, token.expression, pos, level + 1))
        return true;
      pos.setMaxOffset(level + 1);

      pos.setOffset(level, CodePosition.EXPRBLOCK_POS_BLOCK);
      if (inStatements(target, token.block, pos, level + 1))
        return true;
      pos.setMaxOffset(level + 1);
      
      return false;
    }
  }
}
