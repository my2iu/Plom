package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter.GatheredUnboundTypeInfo;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter.TypeLookup;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter.VariableDeclarer;

/**
 * Executes the code for a method argument and reads out the argument
 * name and type
 */
public class MethodArgumentExtractor
{
  public static void fromParameterField(TokenContainer line,
      VariableDeclarer variableDeclarer,
      ErrorList errorGatherer)
  {
    // Parse the line
    try {
      AstNode ast = ParseToAst.parseExpression(Symbol.ParameterField, line.tokens, null);
      if (ast == null) return;
      // Execute the code to extract the variables
      ast.recursiveVisit(statementHandlers, variableDeclarer, null);
    }
    catch (Exception e)
    {
      // Ignore errors
//      e.printStackTrace();
    }
  }

  public static void fromFunctionTypeParameterField(TokenContainer line,
      VariableDeclarer variableDeclarer,
      ErrorList errorGatherer)
  {
    // Parse the line
    try {
      AstNode ast = ParseToAst.parseExpression(Symbol.ParameterFieldOptionalName, line.tokens, null);
      if (ast == null) return;
      // Execute the code to extract the variables
      ast.recursiveVisit(statementHandlers, variableDeclarer, null);
    }
    catch (Exception e)
    {
      // Ignore errors
//      e.printStackTrace();
    }
  }
  
  
  static AstNode.VisitorTriggers<VariableDeclarer, TypeLookup<Type>, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<>();
  static {
    statementHandlers
      .add(Rule.ParameterField_DotDeclareIdentifier_Type,
          (triggers, node, variableDeclarer, typeLookup) -> {
            // Now create the variable
            if (!node.children.get(0).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
              return false;
            String name = ((Token.ParameterToken)node.children.get(0).children.get(0).token).getLookupName();
            GatheredUnboundTypeInfo typeInfo = new GatheredUnboundTypeInfo();
            node.children.get(1).recursiveVisit(VariableDeclarationInterpreter.typeParsingHandlers, typeInfo, null);
            UnboundType type = typeInfo.type;
            variableDeclarer.handle(name, type);
            return false;
          })
      .add(Rule.ParameterFieldOptionalName_DotDeclareIdentifier_Type,
          (triggers, node, variableDeclarer, typeLookup) -> {
            // Now create the variable
            String name = ((Token.ParameterToken)node.children.get(0).children.get(0).token).getLookupName();
            GatheredUnboundTypeInfo typeInfo = new GatheredUnboundTypeInfo();
            node.children.get(1).recursiveVisit(VariableDeclarationInterpreter.typeParsingHandlers, typeInfo, null);
            UnboundType type = typeInfo.type;
            variableDeclarer.handle(name, type);
            return false;
          })
      .add(Rule.ParameterFieldOptionalName_Type,
          (triggers, node, variableDeclarer, typeLookup) -> {
            // Now create the variable
            GatheredUnboundTypeInfo typeInfo = new GatheredUnboundTypeInfo();
            node.children.get(0).recursiveVisit(VariableDeclarationInterpreter.typeParsingHandlers, typeInfo, null);
            UnboundType type = typeInfo.type;
            variableDeclarer.handle(null, type);
            return false;
          });
  }
  
//  // This set of parsers simply returns the raw tokens for the argument name and type
//  public static interface RawArgumenttokens
//  {
//    void handle(String name, Type t);
//  }
//
//  
//  static AstNode.VisitorTriggers<VariableDeclarer, TypeLookup, RuntimeException> statementHandlersForJustTokens = new AstNode.VisitorTriggers<>();
//  static {
//    statementHandlersForJustTokens
//      .add(Rule.ParameterField_DotDeclareIdentifier_AtType,
//          (triggers, node, variableDeclarer, typeLookup) -> {
//            // Now create the variable
//            if (!node.children.get(0).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
//              return false;
//            String name = ((Token.ParameterToken)node.children.get(0).children.get(0).token).getLookupName();
//            GatheredTypeInfo typeInfo = new GatheredTypeInfo();
//            node.children.get(1).recursiveVisit(VariableDeclarationInterpreter.typeParsingHandlers, typeInfo, typeLookup);
//            Type type = typeInfo.type;
//            variableDeclarer.handle(name, type);
//            return false;
//          });
//  }
//
}
