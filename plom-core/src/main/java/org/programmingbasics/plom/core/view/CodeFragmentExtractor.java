package org.programmingbasics.plom.core.view;

import java.io.IOException;

import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
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
    if (start.getOffset(level) == end.getOffset(level))
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
    if (start.getOffset(level) == end.getOffset(level))
    {
      // Recurse deeper
      Token tok = line.tokens.get(start.getOffset(level));
      tok.visit(new RecurseIntoCompoundToken<Void, CodePosition, IOException>() {
        @Override Void handleExpression(Token originalToken,
            TokenContainer exprContainer, CodePosition start, int level,
            CodePosition end) throws IOException
        {
          findCommonBaseFromLine(exprContainer, start, end, level, out);
          return null;
        }
        @Override Void handleStatementContainer(Token originalToken,
            StatementContainer blockContainer, CodePosition start, int level,
            CodePosition end) throws IOException
        {
          findCommonBaseFromStatements(blockContainer, start, end, level, out);
          return null;
        }
      }, start, level + 1, end);
      return;
    }
    
    // Temporarily, just copy tokens between the two levels
    for (int idx = start.getOffset(level); idx < end.getOffset(level); idx++)
    {
      PlomTextWriter.writeToken(out, line.tokens.get(idx));
    }

    return;
  }
  
  
  
  // Extracts code between a code position and anything afterwards
  public static void extractAfterFromStatements(StatementContainer code, CodePosition pos, int level, PlomCodeOutputFormatter out) throws IOException
  {
    return;
  }

  public static void extractAfterFromLine(TokenContainer line, CodePosition start, int level, PlomCodeOutputFormatter out) throws IOException
  {
    int wholeCopyStart = start.getOffset(level);
    // See if we only want to copy part of the first line
    if (start.hasOffset(level + 1))
    {
      // Recurse deeper
//      extractAfterFromLine(code.statements.get(start.getOffset(level)), start, level + 1);
      wholeCopyStart++;
    }
    // Extract in-between stuff
    for (int idx = wholeCopyStart; idx < line.tokens.size(); idx++)
    {
      PlomTextWriter.writeToken(out, line.tokens.get(idx));
    }
  }

  
  // Extracts code that comes before a code position
  public static void extractBeforeFromStatements(StatementContainer code, CodePosition pos, int level, PlomCodeOutputFormatter out) throws IOException
  {
    return;
  }

  public static void extractBeforeFromLine(TokenContainer line, CodePosition end, int level, PlomCodeOutputFormatter out) throws IOException
  {
    for (int n = 0; n < end.getOffset(level); n++)
      PlomTextWriter.writeToken(out, line.tokens.get(n));
    if (end.hasOffset(level + 1))
    {
      // Recurse deeper
    }
  }

}
