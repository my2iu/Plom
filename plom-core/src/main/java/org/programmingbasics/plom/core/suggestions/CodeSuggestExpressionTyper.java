package org.programmingbasics.plom.core.suggestions;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.AstNode.RecursiveWalkerVisitor;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.interpreter.Type;

/**
 * Holds code for typing expressions in order to make code suggestions.
 */
public class CodeSuggestExpressionTyper
{
  static RecursiveWalkerVisitor<CodeCompletionContext, Void, RuntimeException> clearLastUsedType = (triggers, node, context, param) -> {
    context.clearLastTypeUsed();
    return true;
  };
//  static interface BinaryTypeHandler
//  {
//    public Type apply(Type left, Type right);
//  }
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
  
  public static AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException> lastTypeHandlers = new AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException>();
  static {
    lastTypeHandlers
      .add(Rule.DotVariable, (triggers, node, context, param) -> {
        Type t = context.currentScope().lookupType(((Token.ParameterToken)node.token).getLookupName());
        context.pushType(t);
        context.setLastTypeUsed(t);
        return true;
      })
      .add(Rule.DotMember_DotVariable, (triggers, node, context, param) -> {
        if (context.getLastTypeUsed() != null)
        {
          Type self = context.getLastTypeUsed();
          Type.TypeSignature sig = self.lookupMethodSignature(((Token.ParameterToken)node.children.get(0).token).getLookupName());
          context.setLastTypeUsed(sig.returnType);
          context.popType();
          context.pushType(sig.returnType);
        }
        else
        {
          context.setLastTypeUsed(null);
        }
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
      .add(Rule.Return, clearLastUsedType)
      .add(Rule.Assignment, clearLastUsedType)
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
      ;
  }

}
