package org.programmingbasics.plom.core.ast.gen;

public enum Symbol
{
	EMPTY,
	Statement,
	Expression,
	AdditiveExpression,
	AdditiveExpressionMore,
	MultiplicativeExpression,
	MultiplicativeExpressionMore,
	ParenthesisExpression,
	ValueExpression,
	EndStatement,
	Number,
	Plus,
	Minus,
	Multiply,
	Divide,
	OpenParenthesis,
	ClosedParenthesis,
	String,
	DUMMY_COMMENT,
	DUMMY_IF,
	DUMMY_BEGIN,
	DUMMY_END;
	public boolean isTerminal()
	{
		switch(this) {
		case EMPTY:
		case EndStatement:
		case Number:
		case Plus:
		case Minus:
		case Multiply:
		case Divide:
		case OpenParenthesis:
		case ClosedParenthesis:
		case String:
		case DUMMY_COMMENT:
		case DUMMY_IF:
		case DUMMY_BEGIN:
		case DUMMY_END:
			return true;
		default:
			return false;
		}
	}
}
