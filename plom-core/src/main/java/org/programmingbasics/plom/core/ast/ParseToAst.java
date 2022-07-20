package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.gen.Parser;
import org.programmingbasics.plom.core.ast.gen.Symbol;

public class ParseToAst 
{
  Parser parser = new Parser();
  List<Token> tokens;
  Symbol endSymbol;
  int idx = 0;
  boolean recurseIntoTokens = true;
  boolean errorOnPrematureEnd = true;
  ErrorList errorGatherer;
  
  public ParseToAst(List<Token> tokens, Symbol endSymbol, ErrorList errorGatherer)
  {
    this.tokens = tokens;
    this.endSymbol = endSymbol;
    this.errorGatherer = errorGatherer;
  }

  public void setRecurseIntoTokens(boolean val)
  {
    recurseIntoTokens = val;
  }

  /**
   * Set to false to let the parse continue even if the line ends prematurely (parse
   * tree may end up containing nulls)
   */
  public void setErrorOnPrematureEnd(boolean val)
  {
    errorOnPrematureEnd = val;
  }

  public static class ParseException extends Exception
  {
    private static final long serialVersionUID = 1L;
    public Token token;
    public boolean afterToken;
    public static ParseException forToken(Token token)
    {
      ParseException e = new ParseException();
      e.token = token;
      e.afterToken = false;
      return e;
    }
    public static ParseException forEnd(Token lastToken)
    {
      ParseException e = new ParseException();
      e.token = lastToken;
      e.afterToken = true;
      return e;
    }
  }

  public void parseParameterToken(AstNode node, Token.ParameterToken paramToken) throws ParseException
  {
    if (!recurseIntoTokens) return;
    node.internalChildren = new ArrayList<>();
    switch (paramToken.getType())
    {
    case FunctionType:
    case AtType:
      for (TokenContainer param: paramToken.parameters)
      {
        node.internalChildren.add(parseExpression(Symbol.ParameterFieldOptionalName, param.tokens, errorGatherer));
      }
      break;
    default:
      for (TokenContainer param: paramToken.parameters)
      {
        node.internalChildren.add(parseExpression(Symbol.Expression, param.tokens, errorGatherer));
      }
      break;
    }
  }

  private void parseOneExpressionOneBlockToken(AstNode node,
      OneExpressionOneBlockToken token) throws ParseException
  {
    if (!recurseIntoTokens) return;
    node.internalChildren = new ArrayList<>();
    if (token.getType() == Symbol.COMPOUND_FOR)
      node.internalChildren.add(parseExpression(Symbol.ForExpression, token.expression.tokens, errorGatherer));
    else
      node.internalChildren.add(parseExpression(Symbol.Expression, token.expression.tokens, errorGatherer));
    if (errorGatherer != null)
      node.internalChildren.add(parseStatementContainer(token.block, errorGatherer));
    else
      node.internalChildren.add(parseStatementContainer(token.block, null));
  }

  private void parseOneBlockToken(AstNode node, Token.OneBlockToken token) throws ParseException
  {
    if (!recurseIntoTokens) return;
    node.internalChildren = new ArrayList<>();
    if (errorGatherer != null)
      node.internalChildren.add(parseStatementContainer(token.block, errorGatherer));
    else
      node.internalChildren.add(parseStatementContainer(token.block, null));
  }


  private AstNode parse(Symbol base) throws ParseException
  {
    Symbol sym = peekNextTokenType();
    if (base.isTerminal())
    {
      if (sym != base)
      {
        if (!errorOnPrematureEnd)
          return null;
        throwExceptionForNextToken();
      }
      AstNode node = AstNode.fromToken(readNextToken());
      if (node.token instanceof Token.ParameterToken)
        parseParameterToken(node, (Token.ParameterToken)node.token);
      else if (node.token instanceof Token.OneExpressionOneBlockToken)
        parseOneExpressionOneBlockToken(node, (Token.OneExpressionOneBlockToken)node.token);
      else if (node.token instanceof Token.OneBlockToken)
        parseOneBlockToken(node, (Token.OneBlockToken)node.token);
      return node;
    }

    if (parser.parsingTable.get(base) == null)
      throwExceptionForNextToken();
    Symbol[] expansion = parser.parsingTable.get(base).get(sym);
    if (expansion == null)
    {
      if (!errorOnPrematureEnd && sym == endSymbol)
        return null;
      throwExceptionForNextToken();
    }
    AstNode production = new AstNode(base);
    for (Symbol expanded: expansion)
      production.symbols.add(expanded);
    for (Symbol expanded: expansion)
      production.children.add(parse(expanded));
    return production;
  }

  public AstNode parseToEnd(Symbol base) throws ParseException
  {
    AstNode production = parse(base);
    
    // Make sure that we fully consumed all the data
    if (peekNextTokenType() != endSymbol)
      throwExceptionForNextToken();
    return production;
  }
  
  public static AstNode parseExpression(Symbol baseContext, List<Token> tokens, ErrorList errorGatherer) throws ParseException
  {
    try {
      ParseToAst exprParser = new ParseToAst(tokens, Symbol.EndStatement, errorGatherer);
      return exprParser.parseToEnd(baseContext);
    }
    catch (ParseException e)
    {
      if (errorGatherer != null)
        errorGatherer.add(e);
      else
        throw e;
    }
    return null;
  }

  public static AstNode parseStatementContainer(StatementContainer code) throws ParseException
  {
    return parseStatementContainer(code, null);
  }

  public static AstNode parseStatementContainer(StatementContainer code, ErrorList errorGatherer) throws ParseException 
  {
    return parseStatementContainer(Symbol.StatementOrEmpty, code, errorGatherer);
  }

  /**
   * The grammar for each line/statement should end with a EndStatement.
   * The Symbol to match for each line/statement should be specified using the parameter statementContent 
   */
  public static AstNode parseStatementContainer(Symbol statementContent, StatementContainer code, ErrorList errorGatherer) throws ParseException 
  {
    AstNode node = new AstNode(Symbol.ASSEMBLED_STATEMENTS_BLOCK);
    node.internalChildren = new ArrayList<>();
    for (TokenContainer line: code.statements)
    {
      ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, errorGatherer);
      try {
        AstNode parsed = parser.parseToEnd(statementContent);
        node.internalChildren.add(parsed);
      }
      catch (ParseException e)
      {
        if (errorGatherer != null)
          errorGatherer.add(e);
        else
          throw e;
      }
    }

    return node;
  }

  void throwExceptionForNextToken() throws ParseException
  {
    ParseException e;
    if (idx < tokens.size())
    {
      e = ParseException.forToken((Token)tokens.get(idx));
      if (errorGatherer != null) errorGatherer.add(e);
      throw e;
    }
    if (!tokens.isEmpty())
    {
      e = ParseException.forEnd(tokens.get(tokens.size() - 1)); 
      if (errorGatherer != null) errorGatherer.add(e);
      throw e;
    }
    e = ParseException.forEnd(null);
    if (errorGatherer != null) errorGatherer.add(e);
    throw e;
  }
  
  Symbol peekNextTokenType()
  {
    if (idx < tokens.size())
      return ((Token.TokenWithSymbol)tokens.get(idx)).getType();
    return endSymbol; 
  }
  
  Token readNextToken()
  {
    idx++;
    return tokens.get(idx - 1);
  }
}
