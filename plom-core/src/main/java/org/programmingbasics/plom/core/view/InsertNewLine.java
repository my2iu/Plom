package org.programmingbasics.plom.core.view;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor2;
import org.programmingbasics.plom.core.ast.Token.WideToken;

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
      token.visit(new TokenVisitor2<Void, CodePosition, Integer>() {
        @Override
        public Void visitSimpleToken(SimpleToken token, CodePosition pos,
            Integer level)
        {
          throw new IllegalArgumentException();
        }
        void handleWideToken(StatementContainer blockContainer, CodePosition pos,
            int level)
        {
          if (blockContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
          {
            insertNewlineIntoStatementContainer(blockContainer, pos, level + 1);
          }
        }
        @Override
        public Void visitWideToken(WideToken token, CodePosition pos,
            Integer level)
        {
          throw new IllegalArgumentException();
        }
        @Override
        public Void visitOneBlockToken(OneBlockToken token, CodePosition pos,
            Integer level)
        {
          handleWideToken(token.block, pos, level);
          return null;
        }
        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, CodePosition pos,
            Integer level)
        {
          handleWideToken(token.block, pos, level);
          return null;
        }
        
      }, pos, level + 2);
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
