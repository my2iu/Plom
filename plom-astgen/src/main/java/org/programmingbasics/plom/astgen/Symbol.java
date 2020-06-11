package org.programmingbasics.plom.astgen;

enum Symbol
{
  EMPTY(true),
  FullStatement(false),
  Statement(false),
  StatementOrEmpty(false),
  StatementNoComment(false),
  StatementNoCommentOrEmpty(false),
  WideStatement(false),
  OptionalComment(false),
  AfterIf(false),
  IfMore(false),
  ExpressionOnly(false),
  AssignmentExpression(false),
  AssignmentExpressionMore(false),
  Expression(false),
  AdditiveExpression(false),
  AdditiveExpressionMore(false),
  MultiplicativeExpression(false),
  MultiplicativeExpressionMore(false),
  MemberExpression(false),
  MemberExpressionMore(false),
  ParenthesisExpression(false),
  ValueExpression(false),
  EndStatement(true),
  TrueLiteral(true),
  FalseLiteral(true),
  Number(true),
  DotVariable(true),
  Assignment(true),
  Plus(true),
  Minus(true),
  Multiply(true),
  Divide(true),
  OpenParenthesis(true),
  ClosedParenthesis(true),
  String(true),
  
  DUMMY_COMMENT(true),
  COMPOUND_IF(true),
  COMPOUND_ELSE(true),
  COMPOUND_ELSEIF(true),
  DUMMY_BEGIN(true),
  DUMMY_END(true),
  
  ASSEMBLED_STATEMENTS_BLOCK(true);  // Not a true symbol and not used by parsing, but used by the AST to repesent a bunch of statements put together in a block 
  Symbol(boolean isTerminal)
  {
    this.isTerminal = isTerminal;
  }
  boolean isTerminal;
  boolean isDummy()
  {
    return name().startsWith("DUMMY_") || name().startsWith("COMPOUND_");
  }
  boolean isWide()
  {
    return this == DUMMY_COMMENT || name().startsWith("COMPOUND_");
  }
}