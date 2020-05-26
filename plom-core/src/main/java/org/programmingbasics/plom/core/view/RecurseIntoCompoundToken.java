package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;
import org.programmingbasics.plom.core.ast.Token.WideToken;

public class RecurseIntoCompoundToken<T, U> implements TokenVisitor3<T, CodePosition, Integer, U>
{

  @Override
  public T visitSimpleToken(SimpleToken token, CodePosition pos, Integer level, 
      U param)
  {
    throw new IllegalArgumentException();
  }

  @Override
  public T visitParameterToken(ParameterToken token, CodePosition pos, Integer level, 
      U param)
  {
    throw new IllegalArgumentException();
  }
  
  @Override
  public T visitWideToken(WideToken token, CodePosition pos, Integer level,
      U param)
  {
    return handleWideToken(token, null, null, pos, level, param);
  }

  @Override
  public T visitOneBlockToken(OneBlockToken token, CodePosition pos,
      Integer level, U param)
  {
    return handleWideToken(token, null, token.block, pos, level, param);
  }

  @Override
  public T visitOneExpressionOneBlockToken(OneExpressionOneBlockToken token,
      CodePosition pos, Integer level, U param)
  {
    return handleWideToken(token, token.expression, token.block, pos, level, param);
  }

  T handleWideToken(WideToken originalToken, TokenContainer exprContainer,
      StatementContainer blockContainer, CodePosition pos, int level, U param)
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
  
  T handleExpression(WideToken originalToken, TokenContainer exprContainer, CodePosition pos, int level, U param)
  {
    return null;
  }
  
  T handleStatementContainer(WideToken originalToken, StatementContainer blockContainer, CodePosition pos, int level, U param)
  {
    return null;
  }

}
