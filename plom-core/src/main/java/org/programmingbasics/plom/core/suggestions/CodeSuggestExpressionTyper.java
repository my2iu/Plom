package org.programmingbasics.plom.core.suggestions;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.AstNode.RecursiveWalkerVisitor;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter;
/**
 * Holds code for typing expressions in order to make code suggestions.
 */
public class CodeSuggestExpressionTyper
{
  // When parsing type information, we need a structure for stashing
  // that type info in order to return it
//  public static class GatheredTypeInfo
//  {
//    public Type type;
//  }
//  public static AstNode.VisitorTriggers<GatheredTypeInfo, CodeCompletionContext, RuntimeException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredTypeInfo, CodeCompletionContext, RuntimeException>()
//      .add(Rule.AtType, (triggers, node, typesToReturn, context) -> {
//          try
//          {
//            Type t = context.currentScope().typeFromToken(node.token);
//            typesToReturn.type = t;
//          }
//          catch (RunException e)
//          {
//            typesToReturn.type = null;
//          }
//        return true;
//      });

  
  // Helpers for quickly clearing the last type used
  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> clearLastUsedType = (triggers, node, context, param) -> {
    context.clearLastTypeUsed();
    return true;
  };
  
  // Helper for handling operators 
  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> createBinaryTypeToMethodHandler(String methodName) {
    return (triggers, node, context, param) -> {
      if (node.children.get(0) == null)
        return true;
      node.children.get(0).recursiveVisit(triggers, context, param);
      if (node.children.get(1) == null)
        return true;
      node.children.get(1).recursiveVisit(triggers, context, param);
      Type left = context.popType();
      Type right = context.popType();
      Type.TypeSignature sig = left.lookupMethodSignature(methodName);
      if (sig != null)
        context.pushType(sig.returnType);
      else
        context.pushType(context.coreTypes().getVoidType());
      if (node.children.get(2) != null)
        node.children.get(2).recursiveVisit(triggers, context, param);
      return true;
    }; 
  }
//  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> createBinaryTypeHandler(BinaryTypeHandler handler) {
//    return (triggers, node, context, param) -> {
//      if (node.children.get(0) == null)
//        return true;
//      context.clearLastTypeUsed();
//      if (node.children.get(1) == null)
//        return true;
//      node.children.get(1).recursiveVisit(triggers, context, param);
//      Type left = context.popType();
//      Type right = context.popType();
//      if (right == null)
//        context.pushType(null);
//      context.pushType(handler.apply(left, right));
//      context.setLastTypeUsed(right);
//      if (node.children.get(2) != null)
//        node.children.get(2).recursiveVisit(triggers, context, param);
//      return true;
//    }; 
//  }
 
  // Code for tracking types when executing partial code
  public static AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException> lastTypeHandlers = new AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException>();
  static {
    lastTypeHandlers
      .add(Rule.DotVariable, (triggers, node, context, param) -> {
        Type t = context.currentScope().lookupType(((Token.ParameterToken)node.token).getLookupName());
        if (t == null)
          t = context.coreTypes().getVoidType();
        else if (t instanceof Type.TypeSignature)
        {
          context.lastSignatureCalled = (Type.TypeSignature)t; 
          t = ((Type.TypeSignature)t).returnType;
        }
        context.pushType(t);
        context.setLastTypeUsed(t);
        return true;
      })
      .add(Rule.DotMember_DotVariable, (triggers, node, context, param) -> {
        if (context.getLastTypeUsed() != null)
        {
          Type self = context.getLastTypeUsed();
          Type.TypeSignature sig = self.lookupMethodSignature(((Token.ParameterToken)node.children.get(0).token).getLookupName());
          Type returnType;
          if (sig != null)
            returnType = sig.returnType;
          else
            returnType = context.coreTypes().getVoidType();
          context.lastSignatureCalled = sig; 
          context.setLastTypeUsed(returnType);
          context.popType();
          context.pushType(returnType);
        }
        else
        {
          context.lastSignatureCalled = null; 
          context.setLastTypeUsed(null);
        }
        return true;
      })
      .add(Rule.StaticMethodCallExpression_AtType_DotMember, (triggers, node, context, param) -> {
        // This rule wouldn't be matched unless we have at least the first child 
        // (i.e. the Type that the static call is being made on)
        Type type = gatherTypeInfoNoFail(node.children.get(0), context);
        
        // See setLastTypeForStaticCall() for more info about what's going on here
        if (node.children.get(1) == null)
        {
          // We have a type defined, but no static member accessed, so we're probably being
          // used to suggest a type for a static call, so store the type that the static call
          // is being made on
          context.setLastTypeForStaticCall(type);
          context.lastSignatureCalled = null; 
        }
        else
        {
          // We have a static call, and all the parts are there, so we probably aren't being
          // used to suggest completions for the static call, but for something further on 
          // in the code, so just return the type of the static call
          if (type != null)
          {
            String staticMethodName = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
            Type.TypeSignature sig = type.lookupStaticMethodSignature(staticMethodName);
            if (sig != null)
            {
              ExecutableFunction fn = type.lookupStaticMethod(staticMethodName);
              boolean isConstructor = false;
              if (fn.codeUnit != null)
                isConstructor = fn.codeUnit.isConstructor;
              if (isConstructor)
              {
                // Constructors have a @void return type, but they
                // actually return an instance of the class
                context.lastSignatureCalled = sig; 
                context.setLastTypeUsed(type);
                context.pushType(type);
              }
              else
              {
                context.lastSignatureCalled = sig; 
                context.setLastTypeUsed(sig.returnType);
                context.pushType(sig.returnType);
              }
            }
            else
            {
              context.lastSignatureCalled = null; 
              context.setLastTypeUsed(context.coreTypes().getVoidType());
              context.pushType(context.coreTypes().getVoidType());
            }
          }
          else
          {
            context.lastSignatureCalled = null; 
            context.setLastTypeUsed(context.coreTypes().getVoidType());
            context.pushType(context.coreTypes().getVoidType());
          }
        }
        return true;
      })
      .add(Rule.SuperCallExpression_Super_DotSuperMember, (triggers, node, context, param) -> {
//        // This rule wouldn't be matched unless we have at least the first child 
//        // (i.e. the Type that the static call is being made on)
//        GatheredTypeInfo typeInfo = new GatheredTypeInfo();
//        node.children.get(0).recursiveVisit(typeParsingHandlers, typeInfo, context);
//        Type type = typeInfo.type;
//        
        // See setLastTypeForStaticCall() for more info about what's going on here
        if (node.children.get(1) == null)
        {
          context.lastSignatureCalled = null; 
          // No member is defined, so we should provide a type that can be used for suggestions
          if (context.getIsConstructorMethod())
          {
            // Constructor chaining
            context.setLastTypeForStaticCall(context.getDefinedClassOfMethod().parent);
          }
          else if (!context.getIsStaticMethod())
          {
            // Instance method calling super instance method
            context.setLastTypeUsed(context.getDefinedClassOfMethod().parent);
          }
          else
          {
            // Static methods can't use super
          }
        }
        else
        {
          // Figure out the return type of the method being called 
          if (context.getIsConstructorMethod())
          {
            // Constructor chaining
            context.setLastTypeUsed(context.coreTypes().getVoidType());
            context.pushType(context.coreTypes().getVoidType());
            context.lastSignatureCalled = null;  // Not implemented yet 
          }
          else if (!context.getIsStaticMethod())
          {
            // Instance method calling super instance method
            Type self = context.getDefinedClassOfMethod().parent;
            Type.TypeSignature sig = self.lookupMethodSignature(((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName());
            Type returnType;
            if (sig != null)
              returnType = sig.returnType;
            else
              returnType = context.coreTypes().getVoidType();
            context.lastSignatureCalled = sig; 
            context.setLastTypeUsed(returnType);
            context.pushType(returnType);
          }
          else
          {
            // Static methods can't use super
            context.lastSignatureCalled = null;  // Not implemented yet 
          }
//          // We have a static call, and all the parts are there, so we probably aren't being
//          // used to suggest completions for the static call, but for something further on 
//          // in the code, so just return the type of the static call
//          if (type != null)
//          {
//            Type.TypeSignature sig = type.lookupStaticMethodSignature(((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName());
//            if (sig != null)
//            {
//              context.setLastTypeUsed(sig.returnType);
//              context.pushType(sig.returnType);
//            }
//            else
//            {
//              context.setLastTypeUsed(context.coreTypes().getVoidType());
//              context.pushType(context.coreTypes().getVoidType());
//            }
//          }
//          else
//          {
//            context.setLastTypeUsed(context.coreTypes().getVoidType());
//            context.pushType(context.coreTypes().getVoidType());
//          }
        }
        return true;
      })
      .add(Rule.FunctionLiteralExpression_FunctionType, (triggers, node, context, param) -> {
        // We don't currently do anything with function literal expressions,
        // but we do need to cancel out the "lastSignatureCalled" because it
        // may have been set earlier on and will confuse things
        context.lastSignatureCalled = null;  
        return true;
      })
      .add(Rule.Number, (triggers, node, context, param) -> {
        context.pushType(context.coreTypes().getNumberType());
        context.setLastTypeUsed(context.coreTypes().getNumberType());
        return true;
      })
      .add(Rule.String, (triggers, node, context, param) -> {
        context.pushType(context.coreTypes().getStringType());
        context.setLastTypeUsed(context.coreTypes().getStringType());
        return true;
      })
      .add(Rule.TrueLiteral, (triggers, node, context, param) -> {
        context.pushType(context.coreTypes().getBooleanType());
        context.setLastTypeUsed(context.coreTypes().getBooleanType());
        return true;
      })
      .add(Rule.FalseLiteral, (triggers, node, context, param) -> {
        context.pushType(context.coreTypes().getBooleanType());
        context.setLastTypeUsed(context.coreTypes().getBooleanType());
        return true;
      })
      .add(Rule.NullLiteral, (triggers, node, context, param) -> {
        context.pushType(context.coreTypes().getNullType());
        context.setLastTypeUsed(context.coreTypes().getNullType());
        return true;
      })
      .add(Rule.This, (triggers, node, context, param) -> {
        Type thisType;
        try {
          thisType = context.currentScope().lookupThis().type;
        }
        catch (RunException e)
        {
          thisType = context.coreTypes().getVoidType();
        }
        context.pushType(thisType);
        context.setLastTypeUsed(thisType);
        return true;
      })
      .add(Rule.Return, clearLastUsedType)
      .add(Rule.Assignment, clearLastUsedType)
      .add(Rule.Retype, clearLastUsedType)
      .add(Rule.Plus, clearLastUsedType)
      .add(Rule.Minus, clearLastUsedType)
      .add(Rule.Multiply, clearLastUsedType)
      .add(Rule.Divide, clearLastUsedType)
      .add(Rule.Eq, clearLastUsedType)
      .add(Rule.Ne, clearLastUsedType)
      .add(Rule.Gt, clearLastUsedType)
      .add(Rule.Ge, clearLastUsedType)
      .add(Rule.Lt, clearLastUsedType)
      .add(Rule.Le, clearLastUsedType)
      .add(Rule.And, clearLastUsedType)
      .add(Rule.Or, clearLastUsedType)
      .add(Rule.Is, clearLastUsedType)
      .add(Rule.As, clearLastUsedType)
      
      .add(Rule.AdditiveExpressionMore_Plus_MultiplicativeExpression_AdditiveExpressionMore,
          createBinaryTypeToMethodHandler("+:")
      )
      .add(Rule.AdditiveExpressionMore_Minus_MultiplicativeExpression_AdditiveExpressionMore,
          createBinaryTypeToMethodHandler("-:")
      )
      .add(Rule.MultiplicativeExpressionMore_Multiply_MemberExpression_MultiplicativeExpressionMore,
          createBinaryTypeToMethodHandler("*:")
      )
      .add(Rule.MultiplicativeExpressionMore_Divide_MemberExpression_MultiplicativeExpressionMore,
          createBinaryTypeToMethodHandler("/:")
      )
      .add(Rule.RelationalExpressionMore_Eq_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler("=:")
      )
      .add(Rule.RelationalExpressionMore_Ne_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler("!=:")
      )
      .add(Rule.RelationalExpressionMore_Gt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler(">:")
      )
      .add(Rule.RelationalExpressionMore_Ge_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler(">=:")
      )
      .add(Rule.RelationalExpressionMore_Lt_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler("<:")
      )
      .add(Rule.RelationalExpressionMore_Le_AdditiveExpression_RelationalExpressionMore, 
          createBinaryTypeToMethodHandler("<=:")
      )
      .add(Rule.RelationalExpressionMore_Is_Type_RelationalExpressionMore, 
          (triggers, node, context, param) -> {
//            if (node.children.get(0) == null)
//              return true;
//            node.children.get(0).recursiveVisit(triggers, context, param);
            if (node.children.get(1) == null)
              return true;
            Type type = gatherTypeInfoNoFail(node.children.get(1), context);
            context.popType();
            context.pushType(context.coreTypes.getBooleanType());
            context.setLastTypeUsed(context.coreTypes.getBooleanType());
            if (node.children.get(2) != null)
            {
              node.children.get(2).recursiveVisit(triggers, context, param);
            }              
            return true;
          })
      .add(Rule.MemberExpressionMore_As_Type_MemberExpressionMore, 
          (triggers, node, context, param) -> {
            if (node.children.get(1) == null)
              return true;
            Type castedType = gatherTypeInfoNoFail(node.children.get(1), context);
            context.popType();
            context.pushType(castedType);
            context.setLastTypeUsed(castedType);
            if (node.children.get(2) != null)
            {
              node.children.get(2).recursiveVisit(triggers, context, param);
            }              
            return true;
          })
      .add(Rule.RelationalExpressionMore_Retype_Type_RelationalExpressionMore, 
          (triggers, node, context, param) -> {
            if (node.children.get(0) == null)
              return true;
            node.children.get(0).recursiveVisit(triggers, context, param);
            if (node.children.get(1) == null)
              return true;
            Type retypeType = gatherTypeInfoNoFail(node.children.get(1), context);
            context.popType();
            context.pushType(retypeType);
            context.setLastTypeUsed(retypeType);
            if (node.children.get(2) != null)
            {
              node.children.get(2).recursiveVisit(triggers, context, param);
            }              
            return true;
          }
      )
      .add(Rule.OrExpressionMore_Or_AndExpression_OrExpressionMore,
          (triggers, node, context, param) -> {
            if (node.children.get(0) != null)
            {
              node.children.get(0).recursiveVisit(triggers, context, param);
              if (node.children.get(1) != null)
              {
                node.children.get(1).recursiveVisit(triggers, context, param);
                Type left = context.popType();
                Type right = context.popType();
                if (context.coreTypes().getBooleanType().equals(left)
                    && context.coreTypes().getBooleanType().equals(right))
                  context.pushType(context.coreTypes().getBooleanType());
                else
                  context.pushType(context.coreTypes().getVoidType());
                if (node.children.get(2) != null)
                {
                  node.children.get(2).recursiveVisit(triggers, context, param);
                }              
              }              
            }
            return true;
          }
      )
      .add(Rule.AndExpressionMore_And_RelationalExpression_AndExpressionMore,
          (triggers, node, context, param) -> {
            if (node.children.get(0) != null)
            {
              node.children.get(0).recursiveVisit(triggers, context, param);
              if (node.children.get(1) != null)
              {
                node.children.get(1).recursiveVisit(triggers, context, param);
                Type left = context.popType();
                Type right = context.popType();
                if (context.coreTypes().getBooleanType().equals(left)
                    && context.coreTypes().getBooleanType().equals(right))
                  context.pushType(context.coreTypes().getBooleanType());
                else
                  context.pushType(context.coreTypes().getVoidType());
                if (node.children.get(2) != null)
                {
                  node.children.get(2).recursiveVisit(triggers, context, param);
                }              
              }              
            }
            return true;
          }
      )
      .add(Rule.ParenthesisExpression_OpenParenthesis_Expression_ClosedParenthesis, 
          (triggers, node, context, param) -> {
            if (node.children.get(0) != null)
            {
              node.children.get(0).recursiveVisit(triggers, context, param);
              if (node.children.get(1) != null)
              {
                node.children.get(1).recursiveVisit(triggers, context, param);
                if (node.children.get(2) != null)
                {
                  node.children.get(2).recursiveVisit(triggers, context, param);
                  Type t = context.popType();
                  context.setLastTypeUsed(t);
                  context.pushType(t);
                }              
              }              
            }
            return true;
          }
      )
      .add(Rule.ForExpression_DotDeclareIdentifier_VarType_In_Expression, 
          (triggers, node, context, param) -> {
            if (node.children.get(3) != null)
            {
              node.children.get(3).recursiveVisit(triggers, context, param);
              Type t = context.popType();
              context.setLastTypeUsed(t);
              context.pushType(t);
            }
            return true;
          }
      )
      .add(Rule.AssignmentExpression_Expression_AssignmentExpressionMore, (triggers, node, context, param) -> {
        Type varType = null;
        if (node.children.get(0) != null)
        {
          node.children.get(0).recursiveVisit(triggers, context, param);
          varType = context.getLastTypeUsed();
        }
        if (node.children.get(1) != null)
        {
          if (node.children.get(1).matchesRule(Rule.AssignmentExpressionMore_Assignment_Expression))
            context.setExpectedExpressionType(varType);
          node.children.get(1).recursiveVisit(triggers, context, param);
        }
        return true;
      })
      .add(Rule.VarStatement_Var_DotDeclareIdentifier_VarType_VarAssignment,  (triggers, node, context, param) -> {
        if (node.children.get(0) != null)
          node.children.get(0).recursiveVisit(triggers, context, param);
        if (node.children.get(1) != null)
          node.children.get(1).recursiveVisit(triggers, context, param);
        if (node.children.get(2) != null)
          node.children.get(2).recursiveVisit(triggers, context, param);
        if (node.children.get(3) != null)
        {
          if (node.children.get(3).matchesRule(Rule.VarAssignment_Assignment_Expression))
          {
            Type type = gatherTypeInfoNoFail(node.children.get(2), context);
            context.setExpectedExpressionType(type);
          }
          node.children.get(3).recursiveVisit(triggers, context, param);
        }
        return true;
      })
      ;
  }
  
  public static Type gatherTypeInfoNoFail(AstNode node, CodeCompletionContext context)
  {
    UnboundType unboundType = VariableDeclarationInterpreter.gatherUnboundTypeInfo(node);
    try {
      return context.currentScope().typeFromUnboundTypeFromScope(unboundType);
    } 
    catch (RunException e) 
    {
      return null;
    }
  }
}
