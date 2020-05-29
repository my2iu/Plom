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
	ExpressionOnly,
	AssignmentExpression,
	AssignmentExpressionMore,
	Expression,
	AdditiveExpression,
	AdditiveExpressionMore,
	MultiplicativeExpression,
	MultiplicativeExpressionMore,
	MemberExpression,
	MemberExpressionMore,
	ParenthesisExpression,
	ValueExpression,
	EndStatement,
	Number,
	DotVariable,
	Assignment,
	Plus,
	Minus,
	Multiply,
	Divide,
	OpenParenthesis,
	ClosedParenthesis,
	String,
	DUMMY_COMMENT,
	COMPOUND_IF,
	COMPOUND_ELSE,
	COMPOUND_ELSEIF,
	DUMMY_BEGIN,
	DUMMY_END;
	public boolean isTerminal()
	{
		switch(this) {
		case EMPTY:
		case EndStatement:
		case Number:
		case DotVariable:
		case Assignment:
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
		case DUMMY_BEGIN:
		case DUMMY_END:
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
			return true;
		default:
			return false;
		}
	}
}
