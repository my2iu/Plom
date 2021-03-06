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
  ReturnStatement(false),
  VarStatement(false),
  VarType(false),
  VarAssignment(false),
  ExpressionOnly(false),
  AssignmentExpression(false),
  AssignmentExpressionMore(false),
  Expression(false),
  OrExpression(false),
  OrExpressionMore(false),
  AndExpression(false),
  AndExpressionMore(false),
  RelationalExpression(false),
  RelationalExpressionMore(false),
  AdditiveExpression(false),
  AdditiveExpressionMore(false),
  MultiplicativeExpression(false),
  MultiplicativeExpressionMore(false),
  MemberExpression(false),
  MemberExpressionMore(false),
  ParenthesisExpression(false),
  ValueExpression(false),
  StaticMethodCallExpression(false),
  EndStatement(true),
  This(true),
  NullLiteral(true),
  TrueLiteral(true),
  FalseLiteral(true),
  String(true),
  Number(true),
  DotVariable(true),
  AtType(true),
  Var(true),
  Colon(true),
  Assignment(true),
  Retype(true),
  Lt(true),
  Gt(true),
  Le(true),
  Ge(true),
  Eq(true),
  Ne(true),
  Or(true),
  And(true),
  Plus(true),
  Minus(true),
  Multiply(true),
  Divide(true),
  OpenParenthesis(true),
  ClosedParenthesis(true),
  Return(true),
  PrimitivePassthrough(true),
  
  DotDeclareIdentifier(false),  // Expands to DotVariable
  DotMember(false),             // Expands to DotVariable
  
  DUMMY_COMMENT(true),
  COMPOUND_IF(true),
  COMPOUND_ELSE(true),
  COMPOUND_ELSEIF(true),
  COMPOUND_WHILE(true),
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