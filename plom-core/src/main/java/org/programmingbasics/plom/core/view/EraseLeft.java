package org.programmingbasics.plom.core.view;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class EraseLeft
{
  enum AfterAction
  {
    NONE, ERASE_BEGINNING
  }
  
  public static AfterAction eraseLeftFromStatementContainer(
      StatementContainer code, CodePosition pos, int level)
  {
    if (code.statements.isEmpty()) 
    {
      code.statements.add(new TokenContainer(Collections.emptyList()));
    }

    if (pos.getOffset(level) < code.statements.size())
    {
      TokenContainer line = code.statements.get(pos.getOffset(level));
      AfterAction after = eraseLeftFromLine(line, pos, level + 1);
      if (after == AfterAction.ERASE_BEGINNING)
      {
        // See if we're deleting past the beginning of the container
        if (pos.getOffset(level) == 0)
        {
          return AfterAction.ERASE_BEGINNING;
        }
        else
        {
          // See if it's safe to merge the lines
          TokenContainer prevLine = code.statements.get(pos.getOffset(level) - 1);
          
          int newLinePos = prevLine.tokens.size();
          // We can't have a wide token appearing after a normal token on the same line
          if (line.tokens.size() > 0 && line.tokens.get(0).isWide()
              && prevLine.tokens.size() > 0 && !prevLine.tokens.get(prevLine.tokens.size() - 1).isWide())
          {
            // I could delete a token from the end of the previous line, but that
            // might be confusing. Instead, I'll just move the cursor to the end of the
            // previous line
            // (so do nothing since the cursor movement uses the same code as the normal code path)
          }
          else
          {
            // Merge tokens from the lines together
            prevLine.tokens.addAll(line.tokens);
            code.statements.remove(pos.getOffset(level));
          }
          pos.setOffset(level, pos.getOffset(level) - 1);
          pos.setOffset(level + 1, newLinePos);
          pos.setMaxOffset(level + 2);
        }
      }
    }
    return AfterAction.NONE;

  }
  
  
  public static AfterAction eraseLeftFromLine(
      TokenContainer line, CodePosition pos, int level)
  {
    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
    {
      AfterAction after = line.tokens.get(pos.getOffset(level)).visit(new RecurseIntoCompoundToken<AfterAction, Void, RuntimeException>() {
        @Override
        public AfterAction visitParameterToken(ParameterToken token,
            CodePosition pos, Integer level, Void param)
        {
          if (pos.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
          {
            int paramNum = pos.getOffset(level + 1);
            AfterAction action = eraseLeftFromLine(token.parameters.get(paramNum), pos, level + 2);
            if (action == AfterAction.ERASE_BEGINNING)
            {
              if (paramNum > 0)
              {
                pos.setOffset(level + 1, paramNum - 1);
                LastPosition.lastPositionOfLine(token.parameters.get(paramNum - 1), pos, level + 2);
                return AfterAction.NONE;
              }
              return AfterAction.ERASE_BEGINNING;
              
            }
            return AfterAction.NONE;
          }
          throw new IllegalArgumentException();
        }
        @Override
        AfterAction handleWideToken(WideToken originalToken,
            TokenContainer exprContainer, StatementContainer blockContainer,
            CodePosition pos, int level, Void param)
        {
          if (exprContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
          {
            AfterAction action = eraseLeftFromLine(exprContainer, pos, level + 1);
            if (action == AfterAction.ERASE_BEGINNING)
              return AfterAction.ERASE_BEGINNING;
            return AfterAction.NONE;
          }
          else if (blockContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
          {
            AfterAction action = eraseLeftFromStatementContainer(blockContainer, pos, level + 1);
            if (action == AfterAction.ERASE_BEGINNING)
            {
              if (exprContainer != null)
              {
                pos.setOffset(level, CodeRenderer.EXPRBLOCK_POS_EXPR);
                LastPosition.lastPositionOfLine(exprContainer, pos, level + 1);
              }
              else
                return AfterAction.ERASE_BEGINNING;
            }
            return AfterAction.NONE;
          }
          throw new IllegalArgumentException();
        }
      }, pos, level + 1, null);
      if (after == AfterAction.ERASE_BEGINNING)
      {
        // Were inside the token and deleted the top of the token, meaning the
        // entire token should be removed
        line.tokens.remove(pos.getOffset(level));
        pos.setMaxOffset(level + 1);
        return AfterAction.NONE;
      }
      else
        return AfterAction.NONE;
    }
    if (pos.getOffset(level) > 0)
    {
      // If we press backspace at the end of an "if" statement, instead of 
      // deleting the whole "if" statement, we'll just move the position
      // inside the if block
      boolean enterCompoundToken = LastPosition.lastPositionInsideToken(line.tokens.get(pos.getOffset(level) - 1),
          pos, level + 1);
      if (enterCompoundToken)
      {
        // It was a compound token before the cursor, and we've moved into it
        pos.setOffset(level, pos.getOffset(level) - 1);
      }
      else
      {
        // Just a normal token, so backspace should just delete the token before the cursor position
        line.tokens.remove(pos.getOffset(level) - 1);
        pos.setOffset(level, pos.getOffset(level) - 1);
      }
      return AfterAction.NONE;
    }
    else
      return AfterAction.ERASE_BEGINNING;
  }
}
