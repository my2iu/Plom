package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;

/**
 * Executes variable declaration statements and creates the
 * corresponding variables
 */
public class VariableDeclarationInterpreter
{
  public static void fromStatements(StatementContainer code,
      VariableDeclarer variableDeclarer,
//      TypeLookup<Type> typeLookup,
      ErrorList errorGatherer)
  {
    // Parse things one line at a time since we want to skip over
    // errors to extract as much variable information as possible 
    for (TokenContainer line: code.statements)
    {
      // Parse the line
      ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, errorGatherer);
      AstNode parsed = null;
      try {
        parsed = parser.parseToEnd(Symbol.VariableDeclarationOrEmpty);
      }
      catch (ParseException e)
      {
        if (errorGatherer != null)
          errorGatherer.add(e);
      }
      if (parsed == null) 
        continue;

      // Execute the code to extract the variables
      parsed.recursiveVisit(statementHandlers, variableDeclarer, null);
    }
  }

  // When parsing type information, we need a structure for stashing
  // that type info in order to return it
  static class GatheredUnboundTypeInfo
  {
    UnboundType type;
  }
  
  // Callbacks
  public static interface VariableDeclarer
  {
    void handle(String name, UnboundType t);
  }
  public static interface TypeLookup<T>
  {
    T lookupType(UnboundType unboundType);
  }

  // Various AST node handlers for executing each variable declaration 
  static AstNode.VisitorTriggers<GatheredUnboundTypeInfo, Void, RuntimeException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredUnboundTypeInfo, Void, RuntimeException>()
      .add(Rule.AtType, (triggers, node, typesToReturn, unused) -> {
        UnboundType t = UnboundType.fromToken(node.token);
        typesToReturn.type = t;
        return true;
      })
      .add(Rule.FunctionType, (triggers, node, typesToReturn, unused) -> {
        UnboundType t = UnboundType.fromToken(node.token);
        typesToReturn.type = t;
        return true;
      });

  public static UnboundType gatherUnboundTypeInfo(AstNode node)
  {
    GatheredUnboundTypeInfo typeInfo = new GatheredUnboundTypeInfo();
    node.recursiveVisit(typeParsingHandlers, typeInfo, null);
    return typeInfo.type;
  }

  static AstNode.VisitorTriggers<VariableDeclarer, TypeLookup<Type>, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<>();
  static {
    statementHandlers
      .add(Rule.VarDeclarationStatement_Var_DotDeclareIdentifier_VarType,
          (triggers, node, variableDeclarer, typeLookup) -> {
            // Now create the variable
            if (!node.children.get(1).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
              return false;
            String name = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
            UnboundType type = gatherUnboundTypeInfo(node.children.get(2));
            variableDeclarer.handle(name, type);
            return false;
          });
  }
}
