package org.programmingbasics.plom.core.view;

import java.io.IOException;

import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class CodeFragmentExtractor
{
  public static String extractFromStatements(StatementContainer code, CodePosition start, CodePosition end)
  {
    return findCommonBaseFromStatements(code, start, end, 0);
//    for (TokenContainer line: code.statements)
//    {
//      calculateNestingForLine(line);
//    }
  }

  // Find the deepest level in hierarchy where the two code positions share a common base adn start diverging
  public static String findCommonBaseFromStatements(StatementContainer code, CodePosition start, CodePosition end, int level)
  {
    if (start.getOffset(level) == end.getOffset(level))
    {
      // Recurse deeper
      return findCommonBaseFromLine(code.statements.get(start.getOffset(level)), start, end, level + 1);
    }
    return null;
  }
  
  public static String findCommonBaseFromLine(TokenContainer line, CodePosition start, CodePosition end, int level)
  {
    if (start.getOffset(level) == end.getOffset(level))
    {
      // Recurse deeper
      return null;
    }
    
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    try {
      // Temporarily, just copy tokens between the two levels
      for (int idx = start.getOffset(level); idx < end.getOffset(level); idx++)
      {
        PlomTextWriter.writeToken(out, line.tokens.get(idx));
        ;
      }
    } catch (IOException e) {}

    return strBuilder.toString();
  }
}
