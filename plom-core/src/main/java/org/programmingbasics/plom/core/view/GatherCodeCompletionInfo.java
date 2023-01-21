package org.programmingbasics.plom.core.view;

import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.OneBlockToken;
import org.programmingbasics.plom.core.ast.Token.OneExpressionOneBlockToken;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.Token.SimpleToken;
import org.programmingbasics.plom.core.ast.Token.TokenWithSymbol;
import org.programmingbasics.plom.core.ast.Token.WideToken;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.CodeSuggestExpressionTyper;

public class GatherCodeCompletionInfo
{
  public static void fromStatements(StatementContainer statements, CodeCompletionContext context, CodePosition pos, int level)
  {
    int end = pos.getOffset(level);
    // Parse through each line up to the current position
    for (int n = 0; n < end; n++)
    {
      if (n >= statements.statements.size()) break;
      TokenContainer line = statements.statements.get(n);
      // Parse the whole line and extract type and variable info, but
      // do not recurse into sub-blocks
      parseWholeLine(line, Symbol.StatementOrEmpty, context);
    }
    if (end < statements.statements.size() && pos.hasOffset(level + 1))
    {
      TokenContainer line = statements.statements.get(end);
      fromLine(line, Symbol.StatementOrEmpty, context, pos, level + 1);
    }
    return;
  }
  
  static void parseWholeLine(TokenContainer line, Symbol baseContext, CodeCompletionContext context)
  {
    ParseToAst parser = new ParseToAst(line.tokens, Symbol.EndStatement, null);
    parser.setRecurseIntoTokens(false);
    try {
      AstNode parsed = parser.parseToEnd(baseContext);
      parsed.recursiveVisit(statementHandlers, context, null);
    } 
    catch (ParseException e)
    {
      // Ignore errors
    }
  }

  // Performs suggestion analysis for a line of code where the code position is somewhere
  // within the line (or at a deeper level inside a token on the line)
  public static void fromLine(TokenContainer line, Symbol baseContext, CodeCompletionContext context, CodePosition pos, int level)
  {
    if (!pos.hasOffset(level + 1))
    {
      // We're at the last level--for expressions and stuff, so we can
      // dig deeper to get the last type used in the expression. 
      executeTokensForSuggestions(line.tokens.subList(0, pos.getOffset(level)), baseContext, context, pos, level);
    }
    else
    {
      // We're inside another token that might contain statements, so we'll
      // need to recurse into there.
      Token token = line.tokens.get(pos.getOffset(level));
      // If we're entering a block of a for loop, we need to find
      // any variables defined in the expression part first
      token.visit(new Token.TokenVisitor1<Void, CodeCompletionContext>() {
        @Override public Void visitSimpleToken(SimpleToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitParameterToken(ParameterToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitWideToken(WideToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitOneBlockToken(OneBlockToken token,
            CodeCompletionContext param1) { return null; }
        @Override public Void visitOneExpressionOneBlockToken(
            OneExpressionOneBlockToken token, CodeCompletionContext param1) 
        {
          switch (token.getType())
          {
            case COMPOUND_FOR:
              if (pos.getOffset(level + 1) != CodeRenderer.EXPRBLOCK_POS_BLOCK) return null;
              context.setExpectedExpressionType(null);
              parseWholeLine(token.expression, Symbol.ForExpression, context);
              return null;
            case FunctionLiteral:
              // We're only interested in executing the expression part if we're recursing into the block part later on
              if (pos.getOffset(level + 1) != CodeRenderer.EXPRBLOCK_POS_BLOCK) return null;
              // Parse out the function type of the literal and extract any arguments defined in function literal type
              if (token.expression == null) return null;
              parseWholeLine(token.expression, Symbol.FunctionLiteralExpression, context);
              return null;
            default:
              return null;
          }
        }
      }, context); 
      // Now enter into the token for the current code position
      token.visit(new RecurseIntoCompoundToken<Void, CodeCompletionContext, RuntimeException>() {
        @Override Void handleExpression(TokenWithSymbol originalToken, TokenContainer exprContainer,
            CodePosition pos, int level, CodeCompletionContext context)
        {
          context.setExpectedExpressionType(null);
          if (originalToken.getType() == Symbol.COMPOUND_FOR)
            fromLine(exprContainer, Symbol.ForExpression, context, pos, level);
          else if (originalToken.getType() == Symbol.FunctionLiteral)
          {
            // Execute any previous tokens to extract possible lambda completion info
            executeTokensForSuggestions(line.tokens.subList(0, pos.getOffset(level - 2)), baseContext, context, pos, level);
            Type expectedTypeForLambda = context.getExpectedExpressionType();
            context.resetState();
            context.setExpectedExpressionType(expectedTypeForLambda);
            fromLine(exprContainer, Symbol.FunctionLiteralExpression, context, pos, level);
            
          }
          else if (originalToken instanceof Token.ParameterToken)
          {
            // For parameter tokens, we need to extract the type of the parameter
            // token to determine the expected type before recursing into it
            executeTokensForSuggestions(line.tokens.subList(0, pos.getOffset(level - 3) + 1), baseContext, context, pos, level);
            Type.TypeSignature lastSignatureCalled = context.lastSignatureCalled; 
            context.resetState();
            if (lastSignatureCalled != null)
              context.setExpectedExpressionType(lastSignatureCalled.args.get(pos.getOffset(level - 1)));
            fromLine(exprContainer, Symbol.Expression, context, pos, level);
          }
          else
            fromLine(exprContainer, Symbol.Expression, context, pos, level);
          return null;
        }
        @Override Void handleStatementContainer(TokenWithSymbol originalToken,
            StatementContainer blockContainer, CodePosition pos, int level,
            CodeCompletionContext context)
        {
          context.setExpectedExpressionType(null);
          fromStatements(blockContainer, context, pos, level);
          return null;
        }
      }, pos, level + 1, context);
    }
    
//    if (pos.getOffset(level) < line.tokens.size() && pos.hasOffset(level + 1))
//    {
//      return line.tokens.get(pos.getOffset(level)).visit(new TokenAtCursor(), pos, level + 1, null);
//    }
//    if (pos.getOffset(level) < line.tokens.size())
//      return line.tokens.get(pos.getOffset(level));
//    return null;
  }

  static void executeTokensForSuggestions(List<Token> tokens, Symbol baseContext,
      CodeCompletionContext context, CodePosition pos, int level)
  {
    ParseToAst parser = new ParseToAst(tokens, Symbol.EndStatement, null);
    parser.setErrorOnPrematureEnd(false);
    parser.setRecurseIntoTokens(false);
    try {
      AstNode parsed = parser.parseToEnd(baseContext);
      if (parsed != null)
        parsed.recursiveVisit(CodeSuggestExpressionTyper.lastTypeHandlers, context, null);
    } 
    catch (ParseException e)
    {
      // Ignore errors
      e.printStackTrace();
    }
  }
  
  static AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException> statementHandlers = new AstNode.VisitorTriggers<CodeCompletionContext, Void, RuntimeException>();
  static {
    statementHandlers
      .add(Rule.VarStatement_Var_DotDeclareIdentifier_VarType_VarAssignment, (triggers, node, context, param) -> {
        if (node.children.size() < 4)
          return true;
        AstNode declareIdentifier = node.children.get(1);
        AstNode varType = node.children.get(2);
        handleVariableDeclaration(context, declareIdentifier, varType);
        return true;
      })
    .add(Rule.ForExpression_DotDeclareIdentifier_VarType_In_Expression, (triggers, node, context, param) -> {
      if (node.children.size() < 4)
        return true;
      AstNode declareIdentifier = node.children.get(0);
      AstNode varType = node.children.get(1);
      handleVariableDeclaration(context, declareIdentifier, varType);
      return true;
      })
    .add(Rule.FunctionLiteralExpression_FunctionType, (triggers, node, context, param) -> {
      Type type = CodeSuggestExpressionTyper.gatherTypeInfoNoFail(node.children.get(0), context);
      // Add in all arguments from the function type
      if (!(type instanceof Type.LambdaFunctionType)) return true;
      Type.LambdaFunctionType funType = (Type.LambdaFunctionType)type;
      for (int n = 0; n < funType.args.size(); n++)
      {
        if (funType.optionalArgNames.get(n) == null) continue;
        if (funType.args.get(n) == null) continue;
        context.currentScope().addVariable(funType.optionalArgNames.get(n), funType.args.get(n), context.coreTypes().getNullValue());
      }
      return true;
      });
  }
  static void handleVariableDeclaration(CodeCompletionContext context,
      AstNode declareIdentifier, AstNode varType)
  {
    if (declareIdentifier == null || !declareIdentifier.matchesRule(Rule.DotDeclareIdentifier_DotVariable))
      return;
    String name = ((Token.ParameterToken)declareIdentifier.children.get(0).token).getLookupName();
    if (varType == null)
      return;
    Type type = CodeSuggestExpressionTyper.gatherTypeInfoNoFail(varType, context);

    if (type == null) type = context.coreTypes().getVoidType();
//        Value val = context.coreTypes().getNullValue();
//        if (node.children.get(3) == null || !node.children.get(3).matchesRule(Rule.VarAssignment))
//          return true;
    context.currentScope().addVariable(name, type, context.coreTypes().getNullValue());
    return;
  }
}
