package org.programmingbasics.plom.core.ast.gen;

import java.util.Map;
import java.util.HashMap;

public class Parser
{
	public Map<Symbol, Map<Symbol, Symbol[]>> parsingTable = new HashMap<>();
	{
		parsingTable.put(Symbol.Expression, new HashMap<>());
		parsingTable.get(Symbol.Expression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.Expression).put(Symbol.String, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.Expression).put(Symbol.Number, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.put(Symbol.AdditiveExpression, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.String, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.Number, new Symbol[] {Symbol.MultiplicativeExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.put(Symbol.ExpressionOnly, new HashMap<>());
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.Expression, });
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.String, new Symbol[] {Symbol.Expression, });
		parsingTable.get(Symbol.ExpressionOnly).put(Symbol.Number, new Symbol[] {Symbol.Expression, });
		parsingTable.put(Symbol.AfterIf, new HashMap<>());
		parsingTable.get(Symbol.AfterIf).put(Symbol.COMPOUND_IF, new Symbol[] {});
		parsingTable.get(Symbol.AfterIf).put(Symbol.OpenParenthesis, new Symbol[] {});
		parsingTable.get(Symbol.AfterIf).put(Symbol.COMPOUND_ELSEIF, new Symbol[] {Symbol.COMPOUND_ELSEIF, Symbol.AfterIf, });
		parsingTable.get(Symbol.AfterIf).put(Symbol.DUMMY_COMMENT, new Symbol[] {});
		parsingTable.get(Symbol.AfterIf).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.AfterIf).put(Symbol.COMPOUND_ELSE, new Symbol[] {Symbol.COMPOUND_ELSE, });
		parsingTable.get(Symbol.AfterIf).put(Symbol.String, new Symbol[] {});
		parsingTable.get(Symbol.AfterIf).put(Symbol.Number, new Symbol[] {});
		parsingTable.put(Symbol.MultiplicativeExpression, new HashMap<>());
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.String, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.get(Symbol.MultiplicativeExpression).put(Symbol.Number, new Symbol[] {Symbol.ParenthesisExpression, Symbol.MultiplicativeExpressionMore, });
		parsingTable.put(Symbol.WideStatement, new HashMap<>());
		parsingTable.get(Symbol.WideStatement).put(Symbol.COMPOUND_IF, new Symbol[] {Symbol.COMPOUND_IF, Symbol.AfterIf, });
		parsingTable.get(Symbol.WideStatement).put(Symbol.DUMMY_COMMENT, new Symbol[] {Symbol.DUMMY_COMMENT, });
		parsingTable.put(Symbol.ParenthesisExpression, new HashMap<>());
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.OpenParenthesis, Symbol.Expression, Symbol.ClosedParenthesis, });
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.String, new Symbol[] {Symbol.ValueExpression, });
		parsingTable.get(Symbol.ParenthesisExpression).put(Symbol.Number, new Symbol[] {Symbol.ValueExpression, });
		parsingTable.put(Symbol.Statement, new HashMap<>());
		parsingTable.get(Symbol.Statement).put(Symbol.COMPOUND_IF, new Symbol[] {Symbol.WideStatement, Symbol.StatementOrEmpty, });
		parsingTable.get(Symbol.Statement).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.Expression, });
		parsingTable.get(Symbol.Statement).put(Symbol.DUMMY_COMMENT, new Symbol[] {Symbol.WideStatement, Symbol.StatementOrEmpty, });
		parsingTable.get(Symbol.Statement).put(Symbol.String, new Symbol[] {Symbol.Expression, });
		parsingTable.get(Symbol.Statement).put(Symbol.Number, new Symbol[] {Symbol.Expression, });
		parsingTable.put(Symbol.ValueExpression, new HashMap<>());
		parsingTable.get(Symbol.ValueExpression).put(Symbol.String, new Symbol[] {Symbol.String, });
		parsingTable.get(Symbol.ValueExpression).put(Symbol.Number, new Symbol[] {Symbol.Number, });
		parsingTable.put(Symbol.MultiplicativeExpressionMore, new HashMap<>());
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Multiply, new Symbol[] {Symbol.Multiply, Symbol.MultiplicativeExpression, });
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Minus, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Plus, new Symbol[] {});
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.Divide, new Symbol[] {Symbol.Divide, Symbol.MultiplicativeExpression, });
		parsingTable.get(Symbol.MultiplicativeExpressionMore).put(Symbol.ClosedParenthesis, new Symbol[] {});
		parsingTable.put(Symbol.FullStatement, new HashMap<>());
		parsingTable.get(Symbol.FullStatement).put(Symbol.COMPOUND_IF, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.get(Symbol.FullStatement).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.get(Symbol.FullStatement).put(Symbol.DUMMY_COMMENT, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.get(Symbol.FullStatement).put(Symbol.EndStatement, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.get(Symbol.FullStatement).put(Symbol.String, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.get(Symbol.FullStatement).put(Symbol.Number, new Symbol[] {Symbol.StatementOrEmpty, Symbol.EndStatement, });
		parsingTable.put(Symbol.StatementOrEmpty, new HashMap<>());
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.COMPOUND_IF, new Symbol[] {Symbol.Statement, });
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.OpenParenthesis, new Symbol[] {Symbol.Statement, });
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.DUMMY_COMMENT, new Symbol[] {Symbol.Statement, });
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.String, new Symbol[] {Symbol.Statement, });
		parsingTable.get(Symbol.StatementOrEmpty).put(Symbol.Number, new Symbol[] {Symbol.Statement, });
		parsingTable.put(Symbol.AdditiveExpressionMore, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Minus, new Symbol[] {Symbol.Minus, Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Plus, new Symbol[] {Symbol.Plus, Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.ClosedParenthesis, new Symbol[] {});
	}
}
