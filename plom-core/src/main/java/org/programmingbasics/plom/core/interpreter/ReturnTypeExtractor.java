package org.programmingbasics.plom.core.interpreter;

import java.util.function.Consumer;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.GatheredTypeInfo;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter.GatheredUnboundTypeInfo;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter.TypeLookup;

/**
 * Executes the code for a method argument and reads out the argument
 * name and type
 */
public class ReturnTypeExtractor
{
  public static UnboundType fromReturnFieldTokenContainer(TokenContainer line)
  {
    class ReturnTypeSaver implements Consumer<UnboundType>
    {
      UnboundType type;
      @Override public void accept(UnboundType t) { type = t; }
    }
    ReturnTypeSaver returnTypeSaver = new ReturnTypeSaver();
    ReturnTypeExtractor.fromReturnField(
        line,
        returnTypeSaver,
        null);
    if (returnTypeSaver.type != null)
      return returnTypeSaver.type;
    else
      return UnboundType.forClassLookupName("void");
  }
  
  public static void fromReturnField(TokenContainer line,
      Consumer<UnboundType> typeReturner,
//      Void typeLookup,
      ErrorList errorGatherer)
  {
    // Parse the line
    try {
      AstNode ast = ParseToAst.parseExpression(Symbol.ReturnTypeField, line.tokens, null);
      if (ast == null) return;
      // Execute the code to extract the variables
      ast.recursiveVisit(statementHandlers, typeReturner, null);
    }
    catch (Exception e)
    {
      // Ignore errors
//      e.printStackTrace();
    }

  }

  static AstNode.VisitorTriggers<Consumer<UnboundType>, Void, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<>();
  static {
    statementHandlers
      .add(Rule.ReturnTypeField_Type,
          (triggers, node, typeReturn, unsued) -> {
            GatheredUnboundTypeInfo typeInfo = new GatheredUnboundTypeInfo();
            node.children.get(0).recursiveVisit(VariableDeclarationInterpreter.typeParsingHandlers, typeInfo, null);
            UnboundType type = typeInfo.type;
            typeReturn.accept(type);
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
