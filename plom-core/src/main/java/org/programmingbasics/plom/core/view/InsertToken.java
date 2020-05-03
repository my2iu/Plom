package org.programmingbasics.plom.core.view;

import java.util.Collections;

import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenVisitor3;
import org.programmingbasics.plom.core.ast.Token.WideToken;

public class InsertToken
{
  public static void insertTokenIntoStatementContainer(StatementContainer stmtContainer, Token newToken, CodePosition pos, int level)
  {
    if (stmtContainer.statements.isEmpty()) 
    {
      stmtContainer.statements.add(new TokenContainer(Collections.emptyList()));
    }
    TokenContainer line = stmtContainer.statements.get(pos.getOffset(level));
    if (!pos.hasOffset(level + 2) && !newToken.isWide() 
        && line.tokens.size() > pos.getOffset(level + 1)
        && line.tokens.get(pos.getOffset(level + 1)).isWide())
    {
      // Normal tokens cannot have a wide token following them on a line.
      // (It is ok if normal tokens have wide tokens in front of them though.)
      // So if we're inserting a normal token into a line with a wide token,
      // we'll also insert a new line to hold the normal tokens
      // (I've tried changing the grammar to allow wide tokens after
      // normal tokens, but it's too flexible in that it keeps asking
      // you if you want to add if statements in the middle of expressions)
      TokenContainer newline = new TokenContainer(line.tokens.subList(pos.getOffset(level + 1), line.tokens.size()));
      for (int n = line.tokens.size() - 1; n >= pos.getOffset(level + 1); n--)
         line.tokens.remove(n);
      insertTokenIntoLine(line, newToken, pos, level + 1);
      stmtContainer.statements.add(pos.getOffset(level) + 1, newline);
    }
    else
      insertTokenIntoLine(line, newToken, pos, level + 1);
  }
  
  static void insertTokenIntoLine(TokenContainer line, Token newToken, CodePosition pos, int level)
  {
    if (pos.hasOffset(level + 1))
    {
      Token token = line.tokens.get(pos.getOffset(level));
      token.visit(new TokenVisitor3<Void, Token, CodePosition, Integer>() {
        @Override
        public Void visitSimpleToken(SimpleToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          throw new IllegalArgumentException();
        }
        Void handleWideToken(TokenContainer exprContainer, StatementContainer blockContainer,
            Token newToken, CodePosition pos, int level)
        {
          if (exprContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_EXPR)
          {
            insertTokenIntoLine(exprContainer, newToken, pos, level + 1);
            return null;
          }
          else if (blockContainer != null && pos.getOffset(level) == CodeRenderer.EXPRBLOCK_POS_BLOCK)
          {
            insertTokenIntoStatementContainer(blockContainer, newToken, pos, level + 1);
            return null;
          }
          throw new IllegalArgumentException();
        }
        @Override
        public Void visitWideToken(WideToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(null, null, newToken, pos, level);
        }
        @Override
        public Void visitOneBlockToken(OneBlockToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(null, token.block, newToken, pos, level);
        }
        @Override
        public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, Token newToken,
            CodePosition pos, Integer level)
        {
          return handleWideToken(token.expression, token.block, newToken, pos, level);
        }
        
      }, newToken, pos, level + 1);
      return;
    }
    line.tokens.add(pos.getOffset(level), newToken);
    pos.setOffset(level, pos.getOffset(level) + 1);
  }

}
