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
	ReturnStatement,
	VarStatement,
	VarType,
	VarAssignment,
	Type,
	TypeOnly,
	ExpressionOnly,
	AssignmentExpression,
	AssignmentExpressionMore,
	Expression,
	OrExpression,
	OrExpressionMore,
	AndExpression,
	AndExpressionMore,
	RelationalExpression,
	RelationalExpressionMore,
	AdditiveExpression,
	AdditiveExpressionMore,
	MultiplicativeExpression,
	MultiplicativeExpressionMore,
	IsAsExpression,
	IsAsExpressionMore,
	MemberExpression,
	MemberExpressionMore,
	ParenthesisExpression,
	ValueExpression,
	StaticMethodCallExpression,
	SuperCallExpression,
	ForExpression,
	ForExpressionOnly,
	FunctionLiteralExpression,
	FunctionLiteralExpressionOnly,
	EndStatement,
	This,
	Super,
	NullLiteral,
	TrueLiteral,
	FalseLiteral,
	String,
	Number,
	DotVariable,
	AtType,
	FunctionType,
	FunctionLiteral,
	Var,
	Assignment,
	Retype,
	As,
	Is,
	Lt,
	Gt,
	Le,
	Ge,
	Eq,
	Ne,
	Or,
	And,
	Plus,
	Minus,
	Multiply,
	Divide,
	OpenParenthesis,
	ClosedParenthesis,
	Return,
	PrimitivePassthrough,
	In,
	DotDeclareIdentifier,
	DotMember,
	DotSuperMember,
	DUMMY_COMMENT,
	COMPOUND_IF,
	COMPOUND_ELSE,
	COMPOUND_ELSEIF,
	COMPOUND_WHILE,
	COMPOUND_FOR,
	DUMMY_BEGIN,
	DUMMY_END,
	FullVariableDeclaration,
	VariableDeclaration,
	VariableDeclarationOrEmpty,
	VarDeclarationStatement,
	ReturnTypeField,
	ReturnTypeFieldOnly,
	ParameterField,
	ParameterFieldOnly,
	ParameterFieldOptionalName,
	ParameterFieldOptionalNameOnly,
	ASSEMBLED_STATEMENTS_BLOCK;
	public boolean isTerminal()
	{
		switch(this) {
		case EMPTY:
		case EndStatement:
		case This:
		case Super:
		case NullLiteral:
		case TrueLiteral:
		case FalseLiteral:
		case String:
		case Number:
		case DotVariable:
		case AtType:
		case FunctionType:
		case FunctionLiteral:
		case Var:
		case Assignment:
		case Retype:
		case As:
		case Is:
		case Lt:
		case Gt:
		case Le:
		case Ge:
		case Eq:
		case Ne:
		case Or:
		case And:
		case Plus:
		case Minus:
		case Multiply:
		case Divide:
		case OpenParenthesis:
		case ClosedParenthesis:
		case Return:
		case PrimitivePassthrough:
		case In:
		case DUMMY_COMMENT:
		case COMPOUND_IF:
		case COMPOUND_ELSE:
		case COMPOUND_ELSEIF:
		case COMPOUND_WHILE:
		case COMPOUND_FOR:
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
		case COMPOUND_FOR:
			return true;
		default:
			return false;
		}
	}
}
