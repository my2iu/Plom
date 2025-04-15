package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.Token.ParameterToken;
import org.programmingbasics.plom.core.ast.gen.Rule;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.MachineContext.MachineNodeVisitor;

import elemental.util.ArrayOf;
import jsinterop.annotations.JsType;

/**
 * In order to work out how the language will work, I need a simple 
 * code interpreter to make things more concrete and to experiment
 * with different possibilities.
 * 
 * This just runs a simple script or code sequence.
 */
@JsType
public class SimpleInterpreter
{
  private static final ParameterToken AT_END_METHOD = Token.ParameterToken.fromContents(".at end", Symbol.DotVariable);
  private static final ParameterToken VALUE_METHOD = Token.ParameterToken.fromContents(".value", Symbol.DotVariable);
  private static final ParameterToken NEXT_METHOD = Token.ParameterToken.fromContents(".next", Symbol.DotVariable);
  private static final UnboundType NUMBER_ITERATOR_TYPE = UnboundType.forClassLookupName("number iterator");
  
  public SimpleInterpreter(StatementContainer code)
  {
    this.code = code;
  }
  
  StatementContainer code;
  AstNode parsedCode;
  MachineContext ctx;
  ErrorLogger errorLogger;
  
//  // When parsing type information, we need a structure for stashing
//  // that type info in order to return it
//  static class GatheredTypeInfo
//  {
//    Type type;
//  }
//  static AstNode.VisitorTriggers<GatheredTypeInfo, MachineContext, RunException> typeParsingHandlers = new AstNode.VisitorTriggers<GatheredTypeInfo, MachineContext, RunException>()
//      .add(Rule.AtType, (triggers, node, typesToReturn, machine) -> {
//        Type t = machine.currentScope().typeFromToken((Token.ParameterToken)node.token);
//        typesToReturn.type = t;
//        return true;
//      });

  static MachineContext.NodeHandlers statementHandlers = new MachineContext.NodeHandlers();
  static MachineContext.NodeHandlers popIpHandlersForBreakContinue = new MachineContext.NodeHandlers();
  static {
    statementHandlers
      .add(Rule.ASSEMBLED_STATEMENTS_BLOCK, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx < node.internalChildren.size())
              machine.ip.pushAndAdvanceIdx(node.internalChildren.get(idx), statementHandlers);
            else
              machine.ip.pop();
      })
      .add(Rule.VarStatement_Var_DotDeclareIdentifier_VarType_VarAssignment, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0:
              // If a value is assigned to the new var, then calculate the value
              if (node.children.get(3).matchesRule(Rule.VarAssignment_Assignment_Expression))
                machine.ip.pushAndAdvanceIdx(node.children.get(3), ExpressionEvaluator.expressionHandlers);
              else
                machine.ip.advanceIdx();
              break;
            case 1:
              {
              // Now create the variable
              if (!node.children.get(1).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
                throw new RunException();
              String name = ((Token.ParameterToken)node.children.get(1).children.get(0).token).getLookupName();
              Type type = machine.currentScope().typeFromUnboundTypeFromScope(VariableDeclarationInterpreter.gatherUnboundTypeInfo(node.children.get(2)));
              if (type == null) type = machine.coreTypes().getVoidType();
              Value val;
              if (node.children.get(3).matchesRule(Rule.VarAssignment_Assignment_Expression))
                val = machine.popValue();
              else
                val = machine.coreTypes.getNullValue();
              machine.currentScope().addVariable(name, type, val);
              machine.ip.pop();
              break;
              }
            }
          })
      .add(Rule.Statement_AssignmentExpression,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
              machine.ip.pushAndAdvanceIdx(node.children.get(0), ExpressionEvaluator.assignmentLValueHandlers);
            else
              machine.ip.pop();
          })
      .add(Rule.ReturnStatement_Return_ReturnExpression, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
            {
              if (node.children.get(1).matchesRule(Rule.ReturnExpression))
                machine.popStackFrameReturning(Value.createVoidValue(machine.coreTypes()));
              else
                machine.ip.pushAndAdvanceIdx(node.children.get(1), ExpressionEvaluator.expressionHandlers);
            }
            else
              machine.popStackFrameReturning(machine.popValue());
          })
      .add(Rule.Statement_Break, 
    	(MachineContext machine, AstNode node, int idx) -> {
    	  // Walk upwards until we reach a loop
    	  while (machine.ip.hasNext())
    	  {
    	    AstNode ipNode = machine.ip.peekHead().node;
    	    if (ipNode.matchesRule(Rule.WideStatement_COMPOUND_WHILE))
    	    {
    	      forcePopIpForBreakContinue(machine);
    	      return;
    	    }
    	    else if (ipNode.matchesRule(Rule.WideStatement_COMPOUND_FOR))
    	    {
              forcePopIpForBreakContinue(machine);
    	      return;
    	    }
            forcePopIpForBreakContinue(machine);
    	  }
    	  throw new RunException("break was encountered but not inside a loop");
        })
      .add(Rule.Statement_Continue, 
    	(MachineContext machine, AstNode node, int idx) -> {
          // Walk upwards until we reach a loop
          while (machine.ip.hasNext())
          {
            AstNode ipNode = machine.ip.peekHead().node;
            if (ipNode.matchesRule(Rule.WideStatement_COMPOUND_WHILE))
            {
              // Restart execution as if the while loop has returned
              return;
            }
            else if (ipNode.matchesRule(Rule.WideStatement_COMPOUND_FOR))
            {
              // Restart execution as if the for loop has returned
              return;
            }
            forcePopIpForBreakContinue(machine);
          }
      	  throw new RunException("continue not implemented");
        })
      .add(Rule.PrimitivePassthrough, 
          (MachineContext machine, AstNode node, int idx) -> {
            CodeUnitLocation codeUnit = machine.getTopStackFrame().codeUnit;
            PrimitivePassthrough primitive = machine.coreTypes().lookupPrimitive(codeUnit);
            if (primitive == null)
              throw new RunException();
            MachineContext.PrimitiveBlockingFunctionReturn blockWait = machine.getNewBlocker();
            primitive.call(blockWait, machine);
            if (blockWait.isBlocked)
              machine.waitOnBlockingPrimitive(blockWait);
            else
              machine.popStackFrameReturning(blockWait.returnValue);
            // This instruction doesn't end (the primitive should pop
            // the stack frame)
          })
      .add(Rule.WideStatement_COMPOUND_IF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (!machine.coreTypes().getBooleanType().equals(val.type))
                throw new RunException();
              if (val.getBooleanValue())
              {
                machine.pushNewScope();
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              }
              else
                machine.ip.setIdx(3);
              break;
            case 2: // if is taken
              machine.popScope();
              machine.ip.pop();
              break;
            case 3: // if is not taken
              machine.ip.pushAndAdvanceIdx(node.children.get(1), statementHandlers);
              break;
            case 4: // return from if not taken
              machine.ip.pop();
              break;
            }
          })
      .add(Rule.AfterIf_COMPOUND_ELSEIF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (!machine.coreTypes().getBooleanType().equals(val.type))
                throw new RunException();
              if (val.getBooleanValue())
              {
                machine.pushNewScope();
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              }
              else
                machine.ip.setIdx(3);
              break;
            case 2: // if is taken
              machine.popScope();
              machine.ip.pop();
              break;
            case 3: // if is not taken
              machine.ip.pushAndAdvanceIdx(node.children.get(1), statementHandlers);
              break;
            case 4: // return from if not taken
              machine.ip.pop();
              break;
            }
          })
      .add(Rule.AfterIf_COMPOUND_ELSE,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 0)
            {
              // Evaluate block
              machine.pushNewScope();
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), statementHandlers);
            }
            else
            {
              machine.popScope();
              machine.ip.pop();
            }
          })
      .add(Rule.WideStatement_COMPOUND_WHILE, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Evaluate expression
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // Decide whether to follow the if or not
              Value val = machine.popValue();
              if (!machine.coreTypes().getBooleanType().equals(val.type))
                throw new RunException("Expecting a boolean value from while expression");
              if (val.getBooleanValue())
              {
                machine.pushNewScope();
                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              }
              else
                machine.ip.pop();
              break;
            case 2: // go back to reevaluate the expression
              machine.popScope();
              machine.ip.setIdx(0);
              break;
            }
          })
      .add(Rule.WideStatement_COMPOUND_FOR, 
          (MachineContext machine, AstNode node, int idx) -> {
            switch (idx)
            {
            case 0: // Get the iterator or list
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(0).children.get(3), ExpressionEvaluator.expressionHandlers);
              break;
            case 1: // If it's a list, get an iterator from it
            {
              Value val = machine.popValue();
              if (!val.type.isInstanceOf(machine.currentScope().typeFromUnboundTypeFromScope(NUMBER_ITERATOR_TYPE)))
                throw new RunException("Expecting an iterator");
              // Leave the iterator on the stack and start the iteration
              machine.pushValue(val);
              machine.ip.advanceIdx();
              break;
            }
            case 2: // See if there are any values to iterate over
            {
              Value iterator = machine.readValue(0);
              Value self = iterator;
              machine.ip.advanceIdx();  // on return from the method call, we need to be in a different state to look at the result
              ExecutableFunction method = self.type.lookupMethod(AT_END_METHOD.getLookupName());
              machine.pushValue(self); 
              ExpressionEvaluator.callMethodOrFunction(machine, self, method, false, null, false, null);
              break;
            }
            case 3: // We have result of calling "at end", so decide if we should break out of the loop or not
            {
              Value atEnd = machine.popValue();
              if (!machine.coreTypes().getBooleanType().equals(atEnd.type))
                throw new RunException();
              if (atEnd.getBooleanValue())
              {
                // At end of the loop, so clean-up the iterator that we have on the stack
                machine.popValue();
                machine.ip.pop();
                break;
              }
              machine.ip.advanceIdx();
              break;
            }
            case 4: // Start a loop, get the value from the iterator
            {
              Value iterator = machine.readValue(0);
              Value self = iterator;
              
              machine.ip.advanceIdx();  // on return from the method call, we need to be in a different state to look at the result
              ExecutableFunction method = self.type.lookupMethod(VALUE_METHOD.getLookupName());
              machine.pushValue(self); 
              ExpressionEvaluator.callMethodOrFunction(machine, self, method, false, null, false, null);
              break;
            } 
            case 5: // Have the value for the loop, store it in a variable
            {
              machine.pushNewScope();  // For the loop variable and for the block
              AstNode forExpression = node.children.get(0).internalChildren.get(0);
              if (!forExpression.children.get(0).matchesRule(Rule.DotDeclareIdentifier_DotVariable))
                throw new RunException();
              String name = ((Token.ParameterToken)forExpression.children.get(0).children.get(0).token).getLookupName();
              Type type = machine.currentScope().typeFromUnboundTypeFromScope(VariableDeclarationInterpreter.gatherUnboundTypeInfo(forExpression.children.get(1)));
              if (type == null) type = machine.coreTypes().getVoidType();
              Value val = machine.popValue();
              machine.currentScope().addVariable(name, type, val);
              machine.ip.advanceIdx();
              break;
            } 
            case 6: // Run the loop block code
              machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
              break;
            case 7: // Clean up after running loop block code and advance the iterator
            {
              machine.popScope();
              Value iterator = machine.readValue(0);
              Value self = iterator;
              machine.ip.advanceIdx();  // on return from the method call, we need to be in a different state to look at the result
              ExecutableFunction method = self.type.lookupMethod(NEXT_METHOD.getLookupName());
              machine.pushValue(self); 
              ExpressionEvaluator.callMethodOrFunction(machine, self, method, false, null, false, null);
              break;
            } 
            case 8: // Go back and run the loop again
              machine.popValue();  // discard the return value
              machine.ip.setIdx(2);
              break;
//            case 1: // Decide whether to follow the if or not
//              Value val = machine.popValue();
//              if (!machine.coreTypes().getBooleanType().equals(val.type))
//                throw new RunException();
//              if (val.getBooleanValue())
//              {
//                machine.pushNewScope();
//                machine.ip.pushAndAdvanceIdx(node.children.get(0).internalChildren.get(1), statementHandlers);
//              }
//              else
//                machine.ip.pop();
//              break;
//            case 2: // go back to reevaluate the expression
//              machine.popScope();
//              machine.ip.setIdx(0);
//              break;
            }
          });
  }

  static {
    popIpHandlersForBreakContinue
      .add(Rule.WideStatement_COMPOUND_WHILE, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 2)
              machine.popScope();
            machine.ip.pop();
          })
      .add(Rule.WideStatement_COMPOUND_FOR, 
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 7)
            {
              machine.popValue();
              machine.popScope();
            }
            machine.ip.pop();
          })
      .add(Rule.AfterIf_COMPOUND_ELSEIF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 2)
              machine.popScope();
            machine.ip.pop();
          })
      .add(Rule.WideStatement_COMPOUND_IF_AfterIf,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx == 2)
              machine.popScope();
            machine.ip.pop();
          })
      .add(Rule.AfterIf_COMPOUND_ELSE,
          (MachineContext machine, AstNode node, int idx) -> {
            if (idx != 0)
              machine.popScope();
            machine.ip.pop();
          });
  }
  /** During a break or continue, we need to pop instructions off
   * the instruction pointer, but there might be variable scopes
   * need to be popped too. Calling this function with handle that
   * too. (But it's only safe when used specifically for handling
   * break and continue because those statements are only called
   * when the value stack is (mostly) empty and when the variable
   * scopes are known)
   * 
   * This is pretty inelegant and error-prone, so it might be
   * better to find a different way of handling break and continue
   */
  private static void forcePopIpForBreakContinue(MachineContext machine) throws RunException
  {
    AstNode node = machine.ip.peekHead().node;
    MachineNodeVisitor match = popIpHandlersForBreakContinue.get(node.symbols);
    if (match != null)
      match.handleNode(machine, node, machine.ip.peekHead().idx);
    else
      machine.ip.pop();
  }
  
  @JsType
  public static abstract class ErrorLogger
  {
    public abstract void error(Object errObj, ProgramCodeLocation location);
    public abstract void debugLog(Object value);
    public abstract void warn(Object errObj);
    public abstract void log(String msg, LogLevel logLevel, ProgramCodeLocation location);
    
    public static LogLevel DEBUG = LogLevel.DEBUG;
    public static LogLevel WARN = LogLevel.WARN;
    public static LogLevel ERROR = LogLevel.ERROR;
    
  }
  
  @JsType
  public static enum LogLevel
  {
    DEBUG(0), WARN(1), ERROR(2);
    private int level;
    LogLevel(int level) { this.level = level; }
    public int getLevel() { return level; }
    public static LogLevel from(int val)
    {
      for (LogLevel level: values())
      {
        if (level.level == val)
          return level;
      }
      return DEBUG;
    }
  }

  public static class NullErrorLogger extends ErrorLogger
  {
    @Override public void error(Object errObj, ProgramCodeLocation location) {}
    @Override public void warn(Object errObj) {}
    @Override public void debugLog(Object value) {}
    @Override public void log(String msg, LogLevel logLevel, ProgramCodeLocation location) {}
  }
  
  public SimpleInterpreter setErrorLogger(ErrorLogger errorLogger)
  {
    this.errorLogger = errorLogger;
    return this;
  }
  
  public ErrorLogger getErrorLogger()
  {
    return errorLogger;
  }
  
  public void continueRun()
  {
    try {
      ctx.runToCompletion();
    } 
    catch (Throwable e)
    {
      if (errorLogger != null)
      {
        ProgramCodeLocation location = null;
        if (!(e instanceof RunException))
        {
          int idx = 0;
          // Walk through the nodes from the top of the stack to the
          // bottom until we hit one with an associated token
          for (AstNode node = ctx.ip.peekNode(idx); node != null && location == null; idx++)
          {
            location = RunException.locationFromNode(node, ctx);
          }
        }
        else
        {
          location = ((RunException)e).getErrorLocation();
        }
          errorLogger.error(e, location);
      }
    }
  }

  public void runNoReturn(ConfigureGlobalScope globalConfigurator) throws ParseException, RunException
  {
    try {
      ctx = new MachineContext();
      ctx.setErrorLogger(getErrorLogger());
      if (globalConfigurator != null)
        globalConfigurator.configure(ctx.getGlobalScope(), ctx.coreTypes());
      if (parsedCode == null)
        parsedCode = ParseToAst.parseStatementContainer(code);
      
      ctx.pushStackFrame(parsedCode, CodeUnitLocation.forUnknown(), Optional.of(code), null, statementHandlers);
      ctx.runToCompletion();
    }
    catch (Throwable e)
    {
      if (errorLogger != null)
      {
        ProgramCodeLocation location = null;
        if (!(e instanceof RunException))
        {
          int idx = 0;
          // Walk through the nodes from the top of the stack to the
          // bottom until we hit one with an associated token
          for (AstNode node = ctx.ip.peekNode(idx); node != null && location == null; idx++)
          {
            location = RunException.locationFromNode(node, ctx);
          }
        }
        else
        {
          location = ((RunException)e).getErrorLocation();
        }
          errorLogger.error(e, location);
      }
      else
        throw e;
    }
  }
  
  /**
   * Mainly used for testing to check whether scopes are pushed and popped
   * properly within a stack frame.
   */
  void runFrameForTesting(MachineContext ctx, VariableScope scope) throws ParseException, RunException
  {
    if (parsedCode == null)
      parsedCode = ParseToAst.parseStatementContainer(code);
    
    ctx.pushStackFrame(parsedCode, CodeUnitLocation.forUnknown(), Optional.of(code), null, statementHandlers);
    if (scope != null)
      ctx.pushScope(scope);
    ctx.runToEndOfFrame();
  }

  /**
   * A Plom lambda can be passed to JavaScript code to run. When JS tries to
   * run that lambda, it ends up in this method to actually start up an 
   * interpreter to run it.
   */
  public static Value callPlomLambdaFromJs(MachineContext oldCtx, LambdaFunction lambda, ArrayOf<Value> arguments) throws RunException
  {
    try {
      // Create a new interpreter / MachineContext (we're too lazy to check if we can reuse an existing one for now)
      SimpleInterpreter terp = new SimpleInterpreter(null);
      // Reuse the global scope from the context where the lambda was created
      terp.ctx = MachineContext.fromOutsideContext(oldCtx.coreTypes(), oldCtx.getGlobalScope());
      terp.ctx.setErrorLogger(terp.getErrorLogger());
      
      // Set up arguments
      ExecutableFunction fn = lambda.toExecutableFunction();
      List<Value> paddedArgs = new ArrayList<>();
      for (int n = 0; n < fn.argPosToName.size(); n++)
      {
        if (n < arguments.length())
          paddedArgs.add(arguments.get(n));
        else 
          paddedArgs.add(terp.ctx.coreTypes().getNullValue());
      }
      
      // Trying calling into the lambda now
      ExpressionEvaluator.callMethodOrFunctionNoStack(terp.ctx, null, fn, false, null, true, lambda.closureScope, paddedArgs);
      
      // Run the lambda now that its stackframe is on the stack
      terp.ctx.runToCompletion();
      
      // See if there was a return value
      return terp.ctx.getExitReturnValue();
    }
    catch (Throwable e)
    {
//      if (errorLogger != null)
//        errorLogger.error(e);
//      else
        throw e;
    }
  }
  
//  public void run(ConfigureGlobalScope globalConfigurator) throws ParseException, RunException
//  {
//    runNoReturn(globalConfigurator);
//  }
}
