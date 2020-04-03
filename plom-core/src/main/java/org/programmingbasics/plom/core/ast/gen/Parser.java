package org.programmingbasics.plom.core.ast.gen;

import java.util.Map;
import java.util.HashMap;

public class Parser
{
	public Map<Symbol, Map<Symbol, Symbol[]>> parsingTable = new HashMap<>();
	{
		parsingTable.put(Symbol.Expression, new HashMap<>());
		parsingTable.get(Symbol.Expression).put(Symbol.Number, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.Expression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.Expression).put(Symbol.String, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.put(Symbol.MultiplicativeExpression, new HashMap<>());
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.Number, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.String, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.put(Symbol.MultiplicativeExpressionMore, new HashMap<>());
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Minus, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Plus, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.ClosedParenthesis, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Multiply, new Symbol[] {Symbol.Multiply, Symbol.MultiplicativeExpression, });
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Divide, new Symbol[] {Symbol.Divide, Symbol.MultiplicativeExpression, });
		parsingTable.put(Symbol.AdditiveExpressionMore, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Minus, new Symbol[] {Symbol.Minus, Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Plus, new Symbol[] {Symbol.Plus, Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.ClosedParenthesis, new Symbol[] {});
		parsingTable.put(Symbol.IfMore, new HashMap<>());
		parsingTable.get(Symbol.IfMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.IfMore).put(Symbol.COMPOUND_ELSE, new Symbol[] {Symbol.COMPOUND_ELSE, });
		parsingTable.get(Symbol.IfMore).put(Symbol.COMPOUND_ELSEIF, new Symbol[] {Symbol.COMPOUND_ELSEIF, Symbol.IfMore, });
		parsingTable.put(Symbol.ValueExpression, new HashMap<>());
		parsingTable.get(Symbol.ValueExpression).put(Symbol.Number, new Symbol[] {Symbol.Number, });
		parsingTable.get(Symbol.ValueExpression).put(Symbol.String, new Symbol[] {Symbol.String, });
		parsingTable.put(Symbol.Statement, new HashMap<>());
		parsingTable.get(Symbol.Statement).put(Symbol.Number, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.DUMMY_COMMENT, new Symbol[] {Symbol.DUMMY_COMMENT, Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.EndStatement, new Symbol[] {Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.COMPOUND_IF, new Symbol[] {Symbol.COMPOUND_IF, Symbol.IfMore, Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.String, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.put(Symbol.ExpressionOnly, new HashMap<>());
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.Number, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.String, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.put(Symbol.AdditiveExpression, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.Number, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.String, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.put(Symbol.ParenthesisExpression, new HashMap<>());
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.Number, new Symbol[] {Symbol.ValueExpression, });
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.OpenParenthesis, Symbol.Expression, Symbol.ClosedParenthesis, });
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.String, new Symbol[] {Symbol.ValueExpression, });
	}
}
