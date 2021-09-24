package org.programmingbasics.plom.core.view;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.TokenContainer;

public class InsertNewLine
{
  public static void insertNewlineIntoStatementContainer(
      StatementContainer codeList, CodePosition pos, int level)
  {
    if (codeList.statements.isEmpty()) 
    {
      codeList.statements.add(new TokenContainer(Collections.emptyList()));
    }

    TokenContainer line = codeList.statements.get(pos.getOffset(level));
    if (pos.hasOffset(level + 2))
    {
      Token token = line.tokens.get(pos.getOffset(level + 1));
      token.visit(new RecurseIntoCompoundToken<Void, Void, RuntimeException>() {
        @Override
        Void handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
            CodePosition pos, int level, Void param)
        {
          throw new IllegalArgumentException();
        }
        @Override
        Void handleStatementContainer(TokenWithSymbol originalToken,
            StatementContainer blockContainer, CodePosition pos, int level, Void param)
        {
          insertNewlineIntoStatementContainer(blockContainer, pos, level);
          return null;
        }
      }, pos, level + 2, null);
      return;
    }
    
    TokenContainer newline = new TokenContainer(line.tokens.subList(pos.getOffset(level + 1), line.tokens.size()));
    for (int n = line.tokens.size() - 1; n >= pos.getOffset(level + 1); n--)
       line.tokens.remove(n);
    pos.setOffset(level, pos.getOffset(level) + 1);
    codeList.statements.add(pos.getOffset(level), newline);
    pos.setOffset(level + 1, 0);
  }

}
