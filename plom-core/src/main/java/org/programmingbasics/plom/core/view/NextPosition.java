package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

/**
 * Sets the code position to the next position
 */
public class NextPosition
{
  /** Returns true iff parent should move to next position */
  public static boolean nextPositionOfStatements(StatementContainer code, CodePosition pos, int level)
  {
    if (code.statements.isEmpty())
    {
      return true;
    }
    TokenContainer line = code.statements.get(pos.getOffset(level));
    if (nextPositionOfLine(line, pos, level + 1))
    {
      if (pos.getOffset(level) + 1 < code.statements.size())
      {
        pos.setOffset(level, pos.getOffset(level) + 1);
        pos.setOffset(level + 1, 0);
        pos.setMaxOffset(level + 2);
        return false;
      }
      return true;
    }
    return false;
  }
  
  /** Returns true iff parent should move to next position */
  static boolean nextPositionOfLine(TokenContainer line, CodePosition pos, int level)
  {
    if (pos.getOffset(level) >= line.tokens.size())
      return true;
    if (nextPositionInsideToken(line.tokens.get(pos.getOffset(level)), pos, level + 1))
    {
      if (pos.getOffset(level) + 1 <= line.tokens.size())
      {
        pos.setOffset(level, pos.getOffset(level) + 1);
        pos.setMaxOffset(level + 1);
        return false;
      }
      return true;
    }
    return false;
  }
  
  /** Returns true iff parent should move to next position */
  static boolean nextPositionInsideToken(Token token, CodePosition pos, int level)
  {
    return token.visit(new RecurseIntoCompoundToken<Boolean, Void, RuntimeException>() {
      @Override
      public Boolean visitSimpleToken(SimpleToken token, CodePosition pos, Integer level, 
          Void param)
      {
        return true;
      }

      @Override
      public Boolean visitParameterToken(ParameterToken token, CodePosition pos, Integer level, 
          Void param)
      {
        // Start from the front of the token
        if (!pos.hasOffset(level))
        {
          if (!token.parameters.isEmpty())
          {
            pos.setOffset(level, CodeRenderer.PARAMTOK_POS_EXPRS);
            pos.setOffset(level + 1, 0);
            pos.setOffset(level + 2, 0);
            pos.setMaxOffset(level + 3);
            return false;
          }
          return true;
        }
        // Advance inside the parameter
        pos.setOffset(level, CodeRenderer.PARAMTOK_POS_EXPRS);
        if (nextPositionOfLine(token.parameters.get(pos.getOffset(level + 1)), pos, level + 2))
        {
          if (pos.getOffset(level + 1) + 1 < token.parameters.size())
          {
            pos.setOffset(level + 1, pos.getOffset(level + 1) + 1);
            pos.setOffset(level + 2, 0);
            pos.setMaxOffset(level + 3);
            return false;
          }
          return true;
        }
        return false;
//        if (token.parameters.isEmpty())
//          return false;
//        pos.setOffset(level, CodeRenderer.PARAMTOK_POS_EXPRS);
//        pos.setOffset(level + 1, token.parameters.size() - 1);
//        nextPositionOfLine(token.parameters.get(token.parameters.size() - 1), pos, level + 2);
//        return true;
      }
      
      Boolean handleWideToken(WideToken originalToken, TokenContainer exprContainer,
          StatementContainer blockContainer, CodePosition pos, int level, Void param)
      {
        // Start from the front of the token
        if (!pos.hasOffset(level))
        {
          if (exprContainer != null)
          {
            pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_EXPR);
            pos.setOffset(level + 1, 0);
            pos.setMaxOffset(level + 2);
            return false;
          }
          if (blockContainer != null)
          {
            pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
            pos.setOffset(level + 1, 0);
            pos.setOffset(level + 2, 0);
            pos.setMaxOffset(level + 3);
            return false;
          }
          return true;
        }
        else if (pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
        {
          // Last position was in the expression
          if (nextPositionOfLine(exprContainer, pos, level + 1))
          {
            if (blockContainer != null)
            {
              pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
              pos.setOffset(level + 1, 0);
              pos.setOffset(level + 2, 0);
              pos.setMaxOffset(level + 3);
              return false;
            }
          }
          else
            return false;
        }
        else if (pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
        {
          // Last position was in the block
          if (nextPositionOfStatements(blockContainer, pos, level + 1))
            return true;
          else
            return false;
        }
        throw new IllegalArgumentException();
      }

      
//      @Override
//      public Boolean visitWideToken(WideToken token)
//      {
//        return false;
//      }
//
//      @Override
//      public Boolean visitOneBlockToken(OneBlockToken token)
//      {
//        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
//        nextPositionOfStatements(token.block, pos, level + 1);
//        return true;
//      }
//
//      @Override
//      public Boolean visitOneExpressionOneBlockToken(
//          OneExpressionOneBlockToken token)
//      {
//        pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_BLOCK);
//        nextPositionOfStatements(token.block, pos, level + 1);
//        return true;
//      }
    }, pos, level, null);
  }
}
