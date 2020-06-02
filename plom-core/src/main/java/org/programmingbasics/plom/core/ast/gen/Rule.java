package org.programmingbasics.plom.core.ast.gen;

import java.util.Arrays;
import java.util.List;

public class Rule
{
	public final static List<Symbol> FullStatement_StatementOrEmpty_EndStatement = Arrays.asList(Symbol.FullStatement, Symbol.StatementOrEmpty, Symbol.EndStatement);
	public final static List<Symbol> StatementOrEmpty_Statement = Arrays.asList(Symbol.StatementOrEmpty, Symbol.Statement);
	public final static List<Symbol> StatementOrEmpty = Arrays.asList(Symbol.StatementOrEmpty);
	public final static List<Symbol> Statement_AssignmentExpression = Arrays.asList(Symbol.Statement, Symbol.AssignmentExpression);
	public final static List<Symbol> Statement_WideStatement_StatementOrEmpty = Arrays.asList(Symbol.Statement, Symbol.WideStatement, Symbol.StatementOrEmpty);
	public final static List<Symbol> WideStatement_COMPOUND_IF_AfterIf = Arrays.asList(Symbol.WideStatement, Symbol.COMPOUND_IF, Symbol.AfterIf);
	public final static List<Symbol> WideStatement_DUMMY_COMMENT = Arrays.asList(Symbol.WideStatement, Symbol.DUMMY_COMMENT);
	public final static List<Symbol> ExpressionOnly_Expression_EndStatement = Arrays.asList(Symbol.ExpressionOnly, Symbol.Expression, Symbol.EndStatement);
	public final static List<Symbol> AssignmentExpression_Expression_AssignmentExpressionMore = Arrays.asList(Symbol.AssignmentExpression, Symbol.Expression, Symbol.AssignmentExpressionMore);
	public final static List<Symbol> AssignmentExpressionMore_Assignment_Expression = Arrays.asList(Symbol.AssignmentExpressionMore, Symbol.Assignment, Symbol.Expression);
	public final static List<Symbol> AssignmentExpressionMore = Arrays.asList(Symbol.AssignmentExpressionMore);
	public final static List<Symbol> Expression_AdditiveExpression = Arrays.asList(Symbol.Expression, Symbol.AdditiveExpression);
	public final static List<Symbol> AdditiveExpression_MultiplicativeExpression_AdditiveExpressionMore = Arrays.asList(Symbol.AdditiveExpression, Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore);
	public final static List<Symbol> AdditiveExpressionMore_Plus_AdditiveExpression = Arrays.asList(Symbol.AdditiveExpressionMore, Symbol.Plus, Symbol.AdditiveExpression);
	public final static List<Symbol> AdditiveExpressionMore_Minus_AdditiveExpression = Arrays.asList(Symbol.AdditiveExpressionMore, Symbol.Minus, Symbol.AdditiveExpression);
	public final static List<Symbol> AdditiveExpressionMore = Arrays.asList(Symbol.AdditiveExpressionMore);
	public final static List<Symbol> MultiplicativeExpression_MemberExpression_MultiplicativeExpressionMore = Arrays.asList(Symbol.MultiplicativeExpression, Symbol.MemberExpression, Symbol.MultiplicativeExpressionMore);
	public final static List<Symbol> MultiplicativeExpressionMore_Multiply_MultiplicativeExpression = Arrays.asList(Symbol.MultiplicativeExpressionMore, Symbol.Multiply, Symbol.MultiplicativeExpression);
	public final static List<Symbol> MultiplicativeExpressionMore_Divide_MultiplicativeExpression = Arrays.asList(Symbol.MultiplicativeExpressionMore, Symbol.Divide, Symbol.MultiplicativeExpression);
	public final static List<Symbol> MultiplicativeExpressionMore = Arrays.asList(Symbol.MultiplicativeExpressionMore);
	public final static List<Symbol> MemberExpression_ParenthesisExpression_MemberExpressionMore = Arrays.asList(Symbol.MemberExpression, Symbol.ParenthesisExpression, Symbol.MemberExpressionMore);
	public final static List<Symbol> MemberExpressionMore_DotVariable_MemberExpressionMore = Arrays.asList(Symbol.MemberExpressionMore, Symbol.DotVariable, Symbol.MemberExpressionMore);
	public final static List<Symbol> MemberExpressionMore = Arrays.asList(Symbol.MemberExpressionMore);
	public final static List<Symbol> ParenthesisExpression_OpenParenthesis_Expression_ClosedParenthesis = Arrays.asList(Symbol.ParenthesisExpression, Symbol.OpenParenthesis, Symbol.Expression, Symbol.ClosedParenthesis);
	public final static List<Symbol> ParenthesisExpression_ValueExpression = Arrays.asList(Symbol.ParenthesisExpression, Symbol.ValueExpression);
	public final static List<Symbol> ValueExpression_Number = Arrays.asList(Symbol.ValueExpression, Symbol.Number);
	public final static List<Symbol> ValueExpression_DotVariable = Arrays.asList(Symbol.ValueExpression, Symbol.DotVariable);
	public final static List<Symbol> ValueExpression_String = Arrays.asList(Symbol.ValueExpression, Symbol.String);
	public final static List<Symbol> AfterIf_COMPOUND_ELSEIF_AfterIf = Arrays.asList(Symbol.AfterIf, Symbol.COMPOUND_ELSEIF, Symbol.AfterIf);
	public final static List<Symbol> AfterIf_COMPOUND_ELSE = Arrays.asList(Symbol.AfterIf, Symbol.COMPOUND_ELSE);
	public final static List<Symbol> AfterIf = Arrays.asList(Symbol.AfterIf);
	public final static List<Symbol> EMPTY = Arrays.asList(Symbol.EMPTY);
	public final static List<Symbol> EndStatement = Arrays.asList(Symbol.EndStatement);
	public final static List<Symbol> Number = Arrays.asList(Symbol.Number);
	public final static List<Symbol> DotVariable = Arrays.asList(Symbol.DotVariable);
	public final static List<Symbol> Assignment = Arrays.asList(Symbol.Assignment);
	public final static List<Symbol> Plus = Arrays.asList(Symbol.Plus);
	public final static List<Symbol> Minus = Arrays.asList(Symbol.Minus);
	public final static List<Symbol> Multiply = Arrays.asList(Symbol.Multiply);
	public final static List<Symbol> Divide = Arrays.asList(Symbol.Divide);
	public final static List<Symbol> OpenParenthesis = Arrays.asList(Symbol.OpenParenthesis);
	public final static List<Symbol> ClosedParenthesis = Arrays.asList(Symbol.ClosedParenthesis);
	public final static List<Symbol> String = Arrays.asList(Symbol.String);
	public final static List<Symbol> DUMMY_COMMENT = Arrays.asList(Symbol.DUMMY_COMMENT);
	public final static List<Symbol> COMPOUND_IF = Arrays.asList(Symbol.COMPOUND_IF);
	public final static List<Symbol> COMPOUND_ELSE = Arrays.asList(Symbol.COMPOUND_ELSE);
	public final static List<Symbol> COMPOUND_ELSEIF = Arrays.asList(Symbol.COMPOUND_ELSEIF);
	public final static List<Symbol> DUMMY_BEGIN = Arrays.asList(Symbol.DUMMY_BEGIN);
	public final static List<Symbol> DUMMY_END = Arrays.asList(Symbol.DUMMY_END);
}
