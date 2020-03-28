package org.programmingbasics.plom.core.ast.gen;

import java.util.Map;
import java.util.HashMap;

public class Parser
{
	public Map<Symbol, Map<Symbol, Symbol[]>> parsingTable = new HashMap<>();
	{
		parsingTable.put(Symbol.AdditiveExpression, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.Number, new Symbol[] {Symbol.ValueExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.get(Symbol.AdditiveExpression).put(Symbol.String, new Symbol[] {Symbol.ValueExpression, Symbol.AdditiveExpressionMore, });
		parsingTable.put(Symbol.ValueExpression, new HashMap<>());
		parsingTable.get(Symbol.ValueExpression).put(Symbol.Number, new Symbol[] {Symbol.Number, });
		parsingTable.get(Symbol.ValueExpression).put(Symbol.String, new Symbol[] {Symbol.String, });
		parsingTable.put(Symbol.Statement, new HashMap<>());
		parsingTable.get(Symbol.Statement).put(Symbol.EndStatement, new Symbol[] {Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.Number, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.get(Symbol.Statement).put(Symbol.String, new Symbol[] {Symbol.Expression, Symbol.EndStatement, });
		parsingTable.put(Symbol.Expression, new HashMap<>());
		parsingTable.get(Symbol.Expression).put(Symbol.Number, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.Expression).put(Symbol.String, new Symbol[] {Symbol.AdditiveExpression, });
		parsingTable.put(Symbol.AdditiveExpressionMore, new HashMap<>());
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.EndStatement, new Symbol[] {});
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Minus, new Symbol[] {Symbol.Minus, Symbol.AdditiveExpression, });
		parsingTable.get(Symbol.AdditiveExpressionMore).put(Symbol.Plus, new Symbol[] {Symbol.Plus, Symbol.AdditiveExpression, });
	}
}
