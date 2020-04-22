package org.programmingbasics.plom.astgen;

enum Symbol
{
  EMPTY(true),
  Statement(false),
  StatementNoComment(false),
  OptionalComment(false),
  IfMore(false),
  ExpressionOnly(false),
  Expression(false),
  AdditiveExpression(false),
  AdditiveExpressionMore(false),
  MultiplicativeExpression(false),
  MultiplicativeExpressionMore(false),
  ParenthesisExpression(false),
  ValueExpression(false),
  EndStatement(true),
  Number(true),
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
  DUMMY_END(true);
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