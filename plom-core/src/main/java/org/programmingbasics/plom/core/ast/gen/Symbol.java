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
	FunctionTypeName,
	FunctionLiteral,
	Returns,
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
		case FunctionTypeName:
		case FunctionLiteral:
		case Returns:
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
		case FunctionLiteral:
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
	public boolean isInline()
	{
		switch(this) {
		case EMPTY:
		case FullStatement:
		case Statement:
		case StatementOrEmpty:
		case StatementNoComment:
		case StatementNoCommentOrEmpty:
		case WideStatement:
		case OptionalComment:
		case AfterIf:
		case IfMore:
		case ReturnStatement:
		case VarStatement:
		case VarType:
		case VarAssignment:
		case Type:
		case TypeOnly:
		case ExpressionOnly:
		case AssignmentExpression:
		case AssignmentExpressionMore:
		case Expression:
		case OrExpression:
		case OrExpressionMore:
		case AndExpression:
		case AndExpressionMore:
		case RelationalExpression:
		case RelationalExpressionMore:
		case AdditiveExpression:
		case AdditiveExpressionMore:
		case MultiplicativeExpression:
		case MultiplicativeExpressionMore:
		case IsAsExpression:
		case IsAsExpressionMore:
		case MemberExpression:
		case MemberExpressionMore:
		case ParenthesisExpression:
		case ValueExpression:
		case StaticMethodCallExpression:
		case SuperCallExpression:
		case ForExpression:
		case ForExpressionOnly:
		case FunctionLiteralExpression:
		case FunctionLiteralExpressionOnly:
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
		case FunctionTypeName:
		case FunctionLiteral:
		case Returns:
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
		case DotDeclareIdentifier:
		case DotMember:
		case DotSuperMember:
		case DUMMY_BEGIN:
		case DUMMY_END:
		case FullVariableDeclaration:
		case VariableDeclaration:
		case VariableDeclarationOrEmpty:
		case VarDeclarationStatement:
		case ReturnTypeField:
		case ReturnTypeFieldOnly:
		case ParameterField:
		case ParameterFieldOnly:
		case ParameterFieldOptionalName:
		case ParameterFieldOptionalNameOnly:
		case ASSEMBLED_STATEMENTS_BLOCK:
			return true;
		default:
			return false;
		}
	}
}
