package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3Err;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class RecurseIntoCompoundToken<T, U, E extends Throwable> implements TokenVisitor3Err<T, CodePosition, Integer, U, E>
{

  @Override
  public T visitSimpleToken(SimpleToken token, CodePosition pos, Integer level, 
      U param) throws E
  {
    throw new IllegalArgumentException();
  }

  @Override
  public T visitParameterToken(ParameterToken token, CodePosition pos, Integer level, 
      U param) throws E
  {
    if (pos.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
    {
      return handleExpression(token, token.parameters.get(pos.getOffset(level + 1)), pos, level + 2, param);
    }
    throw new IllegalArgumentException();
  }
  
  @Override
  public T visitWideToken(WideToken token, CodePosition pos, Integer level,
      U param) throws E
  {
    return handleWideToken(token, null, null, pos, level, param);
  }

  @Override
  public T visitOneBlockToken(OneBlockToken token, CodePosition pos,
      Integer level, U param) throws E
  {
    return handleWideToken(token, null, token.block, pos, level, param);
  }

  @Override
  public T visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token,
      CodePosition pos, Integer level, U param) throws E
  {
    return handleWideToken(token, token.expression, token.block, pos, level, param);
  }

  T handleWideToken(WideToken originalToken, TokenContainer exprContainer,
      StatementContainer blockContainer, CodePosition pos, int level, U param) throws E
  {
    if (exprContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
    {
      return handleExpression(originalToken, exprContainer, pos, level + 1, param);
    }
    else if (blockContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
    {
      return handleStatementContainer(originalToken, blockContainer, pos, level + 1, param);
    }
    throw new IllegalArgumentException();
  }
  
  T handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer, CodePosition pos, int level, U param) throws E
  {
    return null;
  }
  
  T handleStatementContainer(TokenWithSymbol originalToken, StatementContainer blockContainer, CodePosition pos, int level, U param) throws E
  {
    return null;
  }

}
