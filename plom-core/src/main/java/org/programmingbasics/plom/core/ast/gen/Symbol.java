package org.programmingbasics.plom.core.ast.gen;

public enum Symbol
{
	EMPTY,
	FullStatement,
	Statement,
	StatementOrEmpty,
	StatementNoComment,
	StatementNoCommentOrEmpty,
	WideStatement,
	OptionalComment,
	AfterIf,
	IfMore,
	VarStatement,
	VarType,
	VarAssignment,
	ExpressionOnly,
	AssignmentExpression,
	AssignmentExpressionMore,
	Expression,
	RelationalExpression,
	RelationalExpressionMore,
	AdditiveExpression,
	AdditiveExpressionMore,
	MultiplicativeExpression,
	MultiplicativeExpressionMore,
	MemberExpression,
	MemberExpressionMore,
	ParenthesisExpression,
	ValueExpression,
	EndStatement,
	TrueLiteral,
	FalseLiteral,
	Number,
	DotVariable,
	Var,
	Colon,
	Assignment,
	Lt,
	Gt,
	Le,
	Ge,
	Eq,
	Ne,
	Plus,
	Minus,
	Multiply,
	Divide,
	OpenParenthesis,
	ClosedParenthesis,
	String,
	DotDeclareIdentifier,
	DotType,
	DUMMY_COMMENT,
	COMPOUND_IF,
	COMPOUND_ELSE,
	COMPOUND_ELSEIF,
	COMPOUND_WHILE,
	DUMMY_BEGIN,
	DUMMY_END,
	ASSEMBLED_STATEMENTS_BLOCK;
	public boolean isTerminal()
	{
		switch(this) {
		case EMPTY:
		case EndStatement:
		case TrueLiteral:
		case FalseLiteral:
		case Number:
		case DotVariable:
		case Var:
		case Colon:
		case Assignment:
		case Lt:
		case Gt:
		case Le:
		case Ge:
		case Eq:
		case Ne:
		case Plus:
		case Minus:
		case Multiply:
		case Divide:
		case OpenParenthesis:
		case ClosedParenthesis:
		case String:
		case DUMMY_COMMENT:
		case COMPOUND_IF:
		case COMPOUND_ELSE:
		case COMPOUND_ELSEIF:
		case COMPOUND_WHILE:
		case DUMMY_BEGIN:
		case DUMMY_END:
		case ASSEMBLED_STATEMENTS_BLOCK:
			return true;
		default:
			return false;
		}
	}
	public boolean isWide()
	{
		switch(this) {
		case DUMMY_COMMENT:
		case COMPOUND_IF:
		case COMPOUND_ELSE:
		case COMPOUND_ELSEIF:
		case COMPOUND_WHILE:
			return true;
		default:
			return false;
		}
	}
}
