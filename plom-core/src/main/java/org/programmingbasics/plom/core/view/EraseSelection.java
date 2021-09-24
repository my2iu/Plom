package org.programmingbasics.plom.core.view;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class EraseSelection
{
  public static void fromStatements(StatementContainer code, CodePosition start, CodePosition end)
  {
    if (!start.isBefore(end) && !end.isBefore(start)) return;
    findCommonBaseFromStatements(code, start, end, 0);
  }

  // Find the deepest level in hierarchy where the two code positions share a common base adn start diverging
  public static void findCommonBaseFromStatements(StatementContainer code, CodePosition start, CodePosition end, int level)
  {
    if (start.equalUpToLevel(end, level))  // Sometimes, we an skip checking levels, so we need to check all previous levels too
    {
      // Recurse deeper
      StatementContainer toAppend = findCommonBaseFromLine(code.statements.get(start.getOffset(level)), start, end, level + 1);
      if (toAppend != null)
      {
        if ((code.statements.get(start.getOffset(level)).tokens.isEmpty()
            || code.statements.get(start.getOffset(level)).endsWithWideToken())
            && !toAppend.statements.isEmpty())
        {
          code.statements.get(start.getOffset(level)).tokens.addAll(toAppend.statements.get(0).tokens);
          code.statements.addAll(start.getOffset(level) + 1, toAppend.statements.subList(1, toAppend.statements.size()));
        }
        else
          code.statements.addAll(start.getOffset(level) + 1, toAppend.statements);
      }
      return;
    }
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      eraseAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1);
//      out.newline();
      wholeCopyStart++;
    }
    // Erase in-between stuff
    // Temporarily, just copy tokens between the two levels
    code.statements.subList(wholeCopyStart, end.getOffset(level)).clear();

    // Erase the last bit
    if (end.hasOffset(level + 1))
    {
      StatementContainer choppedOut = new StatementContainer();
      eraseBeforeFromLine(code.statements.get(wholeCopyStart), end, level + 1, choppedOut);
      code.statements.remove(wholeCopyStart);
      // Append the leftover bits of the next line to the current line
      if (start.hasOffset(level + 1) && !choppedOut.statements.isEmpty())
      {
        TokenContainer prev = code.statements.get(start.getOffset(level));
        TokenContainer toMerge = choppedOut.statements.get(0);
        if (!prev.tokens.isEmpty() && prev.endWithNonWideToken()
            && !toMerge.tokens.isEmpty() && toMerge.tokens.get(0).isWide())
        {
          // Can't add wide tokens to a line with non-wide tokens
        }
        else
        {
          prev.tokens.addAll(toMerge.tokens);
          choppedOut.statements.remove(0);
        }
      }
      code.statements.addAll(wholeCopyStart, choppedOut.statements);
    }

    return;
  }
  
  public static StatementContainer findCommonBaseFromLine(TokenContainer line, CodePosition start, CodePosition end, int level)
  {
    if (start.equalUpToLevel(end, level))  // Sometimes, we an skip checking levels, so we need to check all previous levels too
    {
      // Recurse deeper
      Token tok = line.tokens.get(start.getOffset(level));
      final int parentLevel = level;
      final TokenContainer parentLine = line;
      return tok.visit(new RecurseIntoCompoundToken<StatementContainer, CodePosition, RuntimeException>() {
        @Override public StatementContainer visitParameterToken(ParameterToken token, CodePosition start,
            Integer level, CodePosition end)
        {
          if (start.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
          {
            if (start.getOffset(level + 1) == end.getOffset(level + 1))
            {
              handleExpression(token, token.parameters.get(start.getOffset(level + 1)), start, level + 2, end);
              return null;
            }
            eraseAfterFromLine(token.parameters.get(start.getOffset(level + 1)), start, level + 2);
            for (int n = start.getOffset(level + 1) + 1; n < end.getOffset(level + 1); n++)
            {
              token.parameters.get(n).tokens.clear();
            }
            StatementContainer choppedOut = new StatementContainer();
            eraseBeforeFromLine(token.parameters.get(end.getOffset(level + 1)), end, level + 2, choppedOut);
            if (!choppedOut.statements.isEmpty())
            {
              if (choppedOut.statements.size() == 1)
              {
                token.parameters.get(end.getOffset(level + 1)).tokens.clear();
                token.parameters.get(end.getOffset(level + 1)).tokens.addAll(choppedOut.statements.get(0).tokens);
              }
              else
                throw new IllegalArgumentException();
            }
            return null;
          }
          else if (start.getOffset(level) == 0)
          {
            // Start is before the current token
            StatementContainer choppedOut = new StatementContainer();
            token.visit(new EraseBefore(), end, level, choppedOut);
            // Parameter token is not a wide token, so we can add in other non-wide tokens to replace it
            int insertionPoint = start.getOffset(parentLevel);
            parentLine.tokens.remove(insertionPoint);
            for (TokenContainer line: choppedOut.statements)
            {
              boolean earlyOut = false;
              // Flatten all the lines together into a single line
              for (Token tok: line.tokens)
              {
                if (tok.isWide())
                {
                  earlyOut = true;
                  break;
                }
                parentLine.tokens.add(insertionPoint, tok);
                insertionPoint++;
              }
              if (earlyOut) break;
            }

//            throw new IllegalArgumentException();
            return null;
          }
          throw new IllegalArgumentException();
        }
        @Override StatementContainer handleWideToken(WideToken originalToken,
            TokenContainer exprContainer, StatementContainer blockContainer,
            CodePosition start, int level, CodePosition end) 
        {
          if (start.getOffset(level) == end.getOffset(level))
          {
            if (exprContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
            {
              return handleExpression(originalToken, exprContainer, start, level + 1, end);
            }
            else if (blockContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
            {
              return handleStatementContainer(originalToken, blockContainer, start, level + 1, end);
            }
            throw new IllegalArgumentException();
          }
          if (start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
          {
            eraseAfterFromLine(exprContainer, start, level + 1);
//            out.newline();
            StatementContainer choppedOut = new StatementContainer();
            eraseBeforeFromStatements(blockContainer, end, level + 1, choppedOut);
            blockContainer.statements.clear();
            blockContainer.statements.addAll(choppedOut.statements);
            return null;
          }
          else if (start.getOffset(level) == 0)
          {
            // Start is before the current token
            StatementContainer choppedOut = new StatementContainer();
            originalToken.visit(new EraseBefore(), end, level, choppedOut);
            // Insert all the leftover things from the if block into a newline
            TokenContainer newline = new TokenContainer(parentLine.tokens.subList(start.getOffset(parentLevel) + 1, parentLine.tokens.size()));
            parentLine.tokens.subList(start.getOffset(parentLevel), parentLine.tokens.size()).clear();
            choppedOut.statements.add(newline);
            return choppedOut;
          }
          throw new IllegalArgumentException();
        }
        @Override StatementContainer handleExpression(TokenWithSymbol originalToken,
            TokenContainer exprContainer, CodePosition start, int level,
            CodePosition end) 
        {
          StatementContainer toAppend = findCommonBaseFromLine(exprContainer, start, end, level);
          if (toAppend != null)
            throw new IllegalArgumentException();
          return null;
        }
        @Override StatementContainer handleStatementContainer(TokenWithSymbol originalToken,
            StatementContainer blockContainer, CodePosition start, int level,
            CodePosition end) 
        {
          findCommonBaseFromStatements(blockContainer, start, end, level);
          return null;
        }
      }, start, level + 1, end);
    }
    
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first token
    if (start.hasOffset(level + 1))
    {
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new EraseAfter(), start, level + 1, null);
      wholeCopyStart++;
    }
    // Temporarily, just copy tokens between the two levels
    line.tokens.subList(wholeCopyStart, end.getOffset(level)).clear();
    if (end.hasOffset(level + 1))
    {
      StatementContainer choppedOut = new StatementContainer();
      line.tokens.get(wholeCopyStart).visit(new EraseBefore(), end, level + 1, choppedOut);
      // Split out the rest of the line and other contents to be added to the line
      TokenContainer newline = new TokenContainer(line.tokens.subList(wholeCopyStart + 1, line.tokens.size()));
      line.tokens.subList(wholeCopyStart, line.tokens.size()).clear();
      choppedOut.statements.add(newline);
      return choppedOut;
    }
    return null;
  }
  
  
  
  // Erases code between a code position and anything afterwards
  public static void eraseAfterFromStatements(StatementContainer code, CodePosition start, int level) 
  {
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      eraseAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1);
//      out.newline();
      wholeCopyStart++;
    }
    // Erase in-between stuff
    code.statements.subList(wholeCopyStart, code.statements.size()).clear();
  }

  public static void eraseAfterFromLine(TokenContainer line, CodePosition start, int level)
  {
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      // Recurse deeper
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new EraseAfter(), start, level + 1, null);
//      extractAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1);
      wholeCopyStart++;
    }
    // Erase in-between stuff
    line.tokens.subList(wholeCopyStart, line.tokens.size()).clear();
  }
  static class EraseAfter extends RecurseIntoCompoundToken<Void, Void, RuntimeException>
  {
    @Override
    public Void visitParameterToken(ParameterToken token, CodePosition pos,
        Integer level, Void param)
    {
      if (pos.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
      {
        handleExpression(token, token.parameters.get(pos.getOffset(level + 1)), pos, level + 2, null);
//        out.newline();
        for (int n = pos.getOffset(level + 1) + 1; n < token.parameters.size(); n++)
        {
          token.parameters.get(n).tokens.clear();
//          PlomTextWriter.writeTokenContainer(out, token.parameters.get(n));
//          out.newline();
        }
          
        return null;
      }
      throw new IllegalArgumentException();
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition start, int level,
        Void param)
    {
      if (exprContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
      {
        handleExpression(originalToken, exprContainer, start, level + 1, param);
//        out.newline();
        if (blockContainer != null)
          blockContainer.statements.clear();
//          PlomTextWriter.writeStatementContainer(out, blockContainer);
      }
      else if (blockContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
      {
        handleStatementContainer(originalToken, blockContainer, start, level + 1, param);
      }
      return null;
    }
    @Override Void handleExpression(TokenWithSymbol originalToken,
        TokenContainer exprContainer, CodePosition start, int level,
        Void param)
    {
      eraseAfterFromLine(exprContainer, start, level);
      return null;
    }
    @Override Void handleStatementContainer(TokenWithSymbol originalToken,
        StatementContainer blockContainer, CodePosition start, int level,
        Void param)
    {
      eraseAfterFromStatements(blockContainer, start, level);
      return null;
    }
  }

  
  // Erase code that comes before a code position
  public static void eraseBeforeFromStatements(StatementContainer code, CodePosition end, int level, StatementContainer out)
  {
    // Erase in-between stuff
//    for (int n = 0; n < end.getOffset(level); n++)
//    {
//      PlomTextWriter.writeTokenContainer(out, code.statements.get(n));
//      out.newline();
//    }

    // Erase the last bit
    int wholeCopyStart = end.getOffset(level);
    if (end.hasOffset(level + 1))
    {
      eraseBeforeFromLine(code.statements.get(end.getOffset(level)), end, level + 1, out);
      wholeCopyStart++;
    }
    out.statements.addAll(code.statements.subList(wholeCopyStart, code.statements.size()));
//    for (int n = end.getOffset(level) + 1; n < code.statements.size(); n++)
//    {
//      out.statements.add(code.statements.get(n));
//    }
  }

  public static void eraseBeforeFromLine(TokenContainer line, CodePosition end, int level, StatementContainer out)
  {
//    for (int n = 0; n < end.getOffset(level); n++)
//      line.tokens.remove(n);
//      PlomTextWriter.writeToken(out, line.tokens.get(n));
    int wholeCopyStart = end.getOffset(level);
    if (end.hasOffset(level + 1))
    {
      // Recurse deeper
      Token tok = line.tokens.get(end.getOffset(level));
      tok.visit(new EraseBefore(), end, level + 1, out);
      wholeCopyStart++;
    }
    if (wholeCopyStart < line.tokens.size())
    {
      out.statements.add(new TokenContainer(line.tokens.subList(wholeCopyStart, line.tokens.size())));
    }
  }

  static class EraseBefore extends RecurseIntoCompoundToken<Void, StatementContainer, RuntimeException>
  {
    @Override
    public Void visitParameterToken(ParameterToken token, CodePosition end,
        Integer level, StatementContainer out)
    {
      // Start is before the current token
//      PlomTextWriter.writeParameterTokenStart(out, token);
      for (int n = 0; n < token.contents.size(); n++)
      {
//        PlomTextWriter.writeParameterTokenParamStart(out, token, n);
        if (n > end.getOffset(level + 1))
          out.statements.add(token.parameters.get(n));
        else if (n == end.getOffset(level + 1) && end.hasOffset(level + 2))
          eraseBeforeFromLine(token.parameters.get(n), end, level + 2, out);
//        PlomTextWriter.writeParameterTokenParamEnd(out);
      }
//      PlomTextWriter.writeParameterTokenEnd(out, token);
      return null;
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition end, int level,
        StatementContainer out)
    {
      if (end.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
      {
//        PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, false);
        eraseBeforeFromLine(exprContainer, end, level + 1, out);
        if (blockContainer != null)
          out.statements.addAll(blockContainer.statements);
//          PlomTextWriter.writeBlockTokenExpressionToBlock(out);
//        PlomTextWriter.writeBlockTokenEnd(out);
        return null;
      }
      else if (end.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
      {
//        if (exprContainer != null)
//        {
//          PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, false);
//          PlomTextWriter.writeTokenContainer(out, exprContainer);
//          PlomTextWriter.writeBlockTokenExpressionToBlock(out);
//        }
//        else
//          PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, true);
        eraseBeforeFromStatements(blockContainer, end, level + 1, out);
//        out.newline();
//        PlomTextWriter.writeBlockTokenEnd(out);
        return null;
      }
      return null;
    }
  }

}
