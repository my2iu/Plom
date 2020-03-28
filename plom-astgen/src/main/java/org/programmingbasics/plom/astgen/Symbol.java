package org.programmingbasics.plom.astgen;

enum Symbol
{
  EMPTY(true),
  Statement(false),
  Expression(false),
  AdditiveExpression(false),
  AdditiveExpressionMore(false),
  ValueExpression(false),
  EndStatement(true),
  Number(true),
  Plus(true),
  Minus(true),
  String(true),
  
  DUMMY_IF(true),
  DUMMY_BEGIN(true),
  DUMMY_END(true);
  Symbol(boolean isTerminal)
  {
    this.isTerminal = isTerminal;
  }
  boolean isTerminal;
  boolean isDummy()
  {
    return name().startsWith("DUMMY_");
  }
}