package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.MachineContext.MachineNodeVisitor;
import org.programmingbasics.plom.core.interpreter.MachineContext.NodeHandlers;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.GatheredTypeInfo;

/**
 * Executes variable declaration statements and creates the
 * corresponding variables
 */
public class VariableDeclarationInterpreter
{
  public static void fromStatements(StatementContainer code,
      VariableDeclarer variableDeclarer,
      TypeLookup typeLookup,
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
      parsed.recursiveVisit(statementHandlers, variableDeclarer, typeLookup);
    }
  }

  // Callbacks
  public static interface VariableDeclarer
  {
    void handle(String name, Type t);
  }
  public static interface TypeLookup
  {
    Type lookupType(Token token);
  }
  
  // Various AST node handlers for executing each variable declaration 
  static AstNode.VisitorTriggers<GatheredTypeInfo, TypeLookup, RuntimeException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredTypeInfo, TypeLookup, RuntimeException>()
      .add(Rule.AtType, (triggers, node, typesToReturn, typeLookup) -> {
        Type t = typeLookup.lookupType(node.token);
        typesToReturn.type = t;
        return true;
      });

  static AstNode.VisitorTriggers<VariableDeclarer, TypeLookup, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<>();
  static {
    statementHandlers
      .add(Rule.VarDeclarationStatement_Var_DotDeclareIdentifier_VarType,
          (triggers, node, variableDeclarer, typeLookup) -> {
            // Now create the variable
            if (!node.children.get(1).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
              return false;
            String name = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
            GatheredTypeInfo typeInfo = new GatheredTypeInfo();
            node.children.get(2).recursiveVisit(typeParsingHandlers, typeInfo, typeLookup);
            Type type = typeInfo.type;
            variableDeclarer.handle(name, type);
            return false;
          });
  }
}
