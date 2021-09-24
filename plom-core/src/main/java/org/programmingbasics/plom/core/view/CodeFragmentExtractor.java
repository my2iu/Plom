package org.programmingbasics.plom.core.view;

import java.io.IOException;

import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class CodeFragmentExtractor
{
  public static String extractFromStatements(StatementContainer code, CodePosition start, CodePosition end)
  {
    if (!start.isBefore(end) && !end.isBefore(start)) return "";
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    try {
      findCommonBaseFromStatements(code, start, end, 0, out);
    } catch (IOException e) {}
    return strBuilder.toString();
  }

  // Find the deepest level in hierarchy where the two code positions share a common base adn start diverging
  public static void findCommonBaseFromStatements(StatementContainer code, CodePosition start, CodePosition end, int level, PlomCodeOutputFormatter out) throws IOException
  {
    if (start.equalUpToLevel(end, level))  // Sometimes, we an skip checking levels, so we need to check all previous levels too
    {
      // Recurse deeper
      findCommonBaseFromLine(code.statements.get(start.getOffset(level)), start, end, level + 1, out);
      return;
    }
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      extractAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1, out);
      out.newline();
      wholeCopyStart++;
    }
    // Extract in-between stuff
    // Temporarily, just copy tokens between the two levels
    for (int n = wholeCopyStart; n < end.getOffset(level); n++)
    {
      PlomTextWriter.writeTokenContainer(out, code.statements.get(n));
      out.newline();
    }

    // Extract the last bit
    if (end.hasOffset(level + 1))
    {
      extractBeforeFromLine(code.statements.get(end.getOffset(level)), end, level + 1, out);
    }

    return;
  }
  
  public static void findCommonBaseFromLine(TokenContainer line, CodePosition start, CodePosition end, int level, PlomCodeOutputFormatter out) throws IOException
  {
    if (start.equalUpToLevel(end, level))  // Sometimes, we an skip checking levels, so we need to check all previous levels too
    {
      // Recurse deeper
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new RecurseIntoCompoundToken<Void, CodePosition, IOException>() {
        @Override public Void visitParameterToken(ParameterToken token, CodePosition start,
            Integer level, CodePosition end) throws IOException
        {
          if (start.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
          {
            if (start.getOffset(level + 1) == end.getOffset(level + 1))
            {
              handleExpression(token, token.parameters.get(start.getOffset(level + 1)), start, level + 2, end);
              return null;
            }
            extractAfterFromLine(token.parameters.get(start.getOffset(level + 1)), start, level + 2, out);
            out.newline();
            for (int n = start.getOffset(level + 1) + 1; n < end.getOffset(level + 1); n++)
            {
              PlomTextWriter.writeTokenContainer(out, token.parameters.get(n));
              out.newline();
            }
            extractBeforeFromLine(token.parameters.get(end.getOffset(level + 1)), end, level + 2, out);
            return null;
          }
          else if (start.getOffset(level) == 0)
          {
            // Start is before the current token
            token.visit(new ExtractBefore(), end, level, out);
            return null;
          }
          throw new IllegalArgumentException();
        }
        @Override Void handleWideToken(WideToken originalToken,
            TokenContainer exprContainer, StatementContainer blockContainer,
            CodePosition start, int level, CodePosition end) throws IOException
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
            extractAfterFromLine(exprContainer, start, level + 1, out);
            out.newline();
            extractBeforeFromStatements(blockContainer, end, level + 1, out);
            return null;
          }
          else if (start.getOffset(level) == 0)
          {
            // Start is before the current token
            originalToken.visit(new ExtractBefore(), end, level, out);
            return null;
          }
          throw new IllegalArgumentException();
        }
        @Override Void handleExpression(TokenWithSymbol originalToken,
            TokenContainer exprContainer, CodePosition start, int level,
            CodePosition end) throws IOException
        {
          findCommonBaseFromLine(exprContainer, start, end, level, out);
          return null;
        }
        @Override Void handleStatementContainer(TokenWithSymbol originalToken,
            StatementContainer blockContainer, CodePosition start, int level,
            CodePosition end) throws IOException
        {
          findCommonBaseFromStatements(blockContainer, start, end, level, out);
          return null;
        }
      }, start, level + 1, end);
      return;
    }
    
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first token
    if (start.hasOffset(level + 1))
    {
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new ExtractAfter(), start, level + 1, out);
      out.newline();
      wholeCopyStart++;
    }
    // Temporarily, just copy tokens between the two levels
    for (int idx = wholeCopyStart; idx < end.getOffset(level); idx++)
    {
      PlomTextWriter.writeToken(out, line.tokens.get(idx));
    }
    if (end.hasOffset(level + 1))
      line.tokens.get(end.getOffset(level)).visit(new ExtractBefore(), end, level + 1, out);
    return;
  }
  
  
  
  // Extracts code between a code position and anything afterwards
  public static void extractAfterFromStatements(StatementContainer code, CodePosition start, int level, PlomCodeOutputFormatter out) throws IOException
  {
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      extractAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1, out);
      out.newline();
      wholeCopyStart++;
    }
    // Extract in-between stuff
    // Temporarily, just copy tokens between the two levels
    for (int n = wholeCopyStart; n < code.statements.size(); n++)
    {
      PlomTextWriter.writeTokenContainer(out, code.statements.get(n));
      out.newline();
    }
  }

  public static void extractAfterFromLine(TokenContainer line, CodePosition start, int level, PlomCodeOutputFormatter out) throws IOException
  {
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      // Recurse deeper
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new ExtractAfter(), start, level + 1, out);
//      extractAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1);
      wholeCopyStart++;
    }
    // Extract in-between stuff
    for (int idx = wholeCopyStart; idx < line.tokens.size(); idx++)
    {
      PlomTextWriter.writeToken(out, line.tokens.get(idx));
    }
  }
  static class ExtractAfter extends RecurseIntoCompoundToken<Void, PlomCodeOutputFormatter, IOException>
  {
    @Override
    public Void visitParameterToken(ParameterToken token, CodePosition pos,
        Integer level, PlomCodeOutputFormatter out) throws IOException
    {
      if (pos.getOffset(level) == CodeRenderer.PARAMTOK_POS_EXPRS)
      {
        handleExpression(token, token.parameters.get(pos.getOffset(level + 1)), pos, level + 2, out);
        out.newline();
        for (int n = pos.getOffset(level + 1) + 1; n < token.parameters.size(); n++)
        {
          PlomTextWriter.writeTokenContainer(out, token.parameters.get(n));
          out.newline();
        }
          
        return null;
      }
      throw new IllegalArgumentException();
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition start, int level,
        PlomCodeOutputFormatter out) throws IOException
    {
      if (exprContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
      {
        handleExpression(originalToken, exprContainer, start, level + 1, out);
        out.newline();
        if (blockContainer != null)
          PlomTextWriter.writeStatementContainer(out, blockContainer);
      }
      else if (blockContainer != null && start.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
      {
        handleStatementContainer(originalToken, blockContainer, start, level + 1, out);
      }
      return null;
    }
    @Override Void handleExpression(TokenWithSymbol originalToken,
        TokenContainer exprContainer, CodePosition start, int level,
        PlomCodeOutputFormatter out) throws IOException
    {
      extractAfterFromLine(exprContainer, start, level, out);
      return null;
    }
    @Override Void handleStatementContainer(TokenWithSymbol originalToken,
        StatementContainer blockContainer, CodePosition start, int level,
        PlomCodeOutputFormatter out) throws IOException
    {
      extractAfterFromStatements(blockContainer, start, level, out);
      return null;
    }
  }

  
  // Extracts code that comes before a code position
  public static void extractBeforeFromStatements(StatementContainer code, CodePosition end, int level, PlomCodeOutputFormatter out) throws IOException
  {
    // Extract in-between stuff
    for (int n = 0; n < end.getOffset(level); n++)
    {
      PlomTextWriter.writeTokenContainer(out, code.statements.get(n));
      out.newline();
    }

    // Extract the last bit
    if (end.hasOffset(level + 1))
    {
      extractBeforeFromLine(code.statements.get(end.getOffset(level)), end, level + 1, out);
    }
  }

  public static void extractBeforeFromLine(TokenContainer line, CodePosition end, int level, PlomCodeOutputFormatter out) throws IOException
  {
    for (int n = 0; n < end.getOffset(level); n++)
      PlomTextWriter.writeToken(out, line.tokens.get(n));
    if (end.hasOffset(level + 1))
    {
      // Recurse deeper
      Token tok = line.tokens.get(end.getOffset(level));
      tok.visit(new ExtractBefore(), end, level + 1, out);
    }
  }

  static class ExtractBefore extends RecurseIntoCompoundToken<Void, PlomCodeOutputFormatter, IOException>
  {
    @Override
    public Void visitParameterToken(ParameterToken token, CodePosition end,
        Integer level, PlomCodeOutputFormatter out) throws IOException
    {
      // Start is before the current token
      PlomTextWriter.writeParameterTokenStart(out, token);
      for (int n = 0; n < token.contents.size(); n++)
      {
        PlomTextWriter.writeParameterTokenParamStart(out, token, n);
        if (n < end.getOffset(level + 1))
          PlomTextWriter.writeTokenContainer(out, token.parameters.get(n));
        else if (n == end.getOffset(level + 1) && end.hasOffset(level + 2))
          extractBeforeFromLine(token.parameters.get(n), end, level + 2, out);
        PlomTextWriter.writeParameterTokenParamEnd(out);
      }
      PlomTextWriter.writeParameterTokenEnd(out, token);
      return null;
    }
    @Override
    Void handleWideToken(WideToken originalToken, TokenContainer exprContainer,
        StatementContainer blockContainer, CodePosition end, int level,
        PlomCodeOutputFormatter out) throws IOException
    {
      if (end.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
      {
        PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, false);
        extractBeforeFromLine(exprContainer, end, level + 1, out);
        if (blockContainer != null)
          PlomTextWriter.writeBlockTokenExpressionToBlock(out);
        PlomTextWriter.writeBlockTokenEnd(out);
        return null;
      }
      else if (end.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
      {
        if (exprContainer != null)
        {
          PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, false);
          PlomTextWriter.writeTokenContainer(out, exprContainer);
          PlomTextWriter.writeBlockTokenExpressionToBlock(out);
        }
        else
          PlomTextWriter.writeBlockTokenFirstLine(out, originalToken, true);
        extractBeforeFromStatements(blockContainer, end, level + 1, out);
        out.newline();
        PlomTextWriter.writeBlockTokenEnd(out);
        return null;
      }
      return null;
    }
  }

}
