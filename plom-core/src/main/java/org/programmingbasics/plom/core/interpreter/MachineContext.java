package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

/**
 * We want to run the interpreter in JavaScript, but JavaScript
 * has no threads. Since we may want to emulate functions that block
 * or asynchronous execution or coroutines, we can't actually store
 * state or instruction pointers in the interpreter code itself. We
 * need to abstract everything so that the stack, execution state, and
 * instruction pointers are stored in its own object and we have a
 * generic "machine" that executes the code. This gets a little messy
 * because we don't compile down to a flatter bytecode but execute
 * out of the abstract syntax tree. 
 */
public class MachineContext
{
  /**
   * Holds the core types
   */
  CoreTypeLibrary coreTypes = new CoreTypeLibrary();
  
  /**
   * Provides easy access to the core types
   */
  CoreTypeLibrary coreTypes() { return coreTypes; }
  
  /**
   * Scope where bindings of global names and values are held
   */
  private VariableScope globalScope = new VariableScope();
  
  /**
   * Gets the global scope so that new global variables can be added
   */
  public VariableScope getGlobalScope()
  {
    return globalScope;
  }
  
  /**
   * Gets the current scope used for looking up variables and where
   * new variables will be added. A scope is where bindings of names 
   * to values for variables can be looked up
   */
  public VariableScope currentScope()
  {
    return topStackFrame.topScope;
  }
  
  /**
   * Pushes a new variable scope level (mainly used for testing)
   */
  void pushScope(VariableScope scope)
  {
    scope.setParent(topStackFrame.topScope);
    topStackFrame.topScope = scope;
  }
  public void pushNewScope()
  {
    VariableScope scope = new VariableScope();
    pushScope(scope);
  }
  public void popScope()
  {
    topStackFrame.topScope = topStackFrame.topScope.getParent();
  }
  
  /**
   * Stack of values where values can be stashed while expressions
   * are being evaluated.
   */
  private List<Value> valueStack = new ArrayList<>();
  
  // TODO: Change Value stuff to have a Value pool and to overwrite values
  // instead of passing around references
  public void pushValue(Value v)
  {
    valueStack.add(v);
  }
  public Value popValue()
  {
    return valueStack.remove(valueStack.size() - 1);
  }
  public void popValues(int count)
  {
    for (int n = 0; n < count; n++)
      valueStack.remove(valueStack.size() - 1);
  }
  public Value readValue(int offset)
  {
    return valueStack.get(valueStack.size() - 1 - offset);
  }
  public int valueStackSize()
  {
    return valueStack.size();
  }
  

  /**
   * For handling some correctness assertions for the assignment
   * stuff, we have extra variables for storing values we can later
   * assert against. This value should later be put into the stack
   * frame.
   */
  public void setLValueAssertCheck(int val) { topStackFrame.lValueStackAssertCheck = val; } 
  public int getLValueAssertCheck() { return topStackFrame.lValueStackAssertCheck; } 
  public int lValueStackSize() { return lvalueStack.size(); }

  /**
   * When doing an assignment into a variable, we don't want to track
   * the value of the variable, but its location, so we'll use a separate
   * data structure and stack for tracking and storing that information.
   */
  // TODO: Change LValue stuff to have a Value pool and to overwrite values
  // instead of passing around references
  private List<LValue> lvalueStack = new ArrayList<>();
  public void pushLValue(LValue v)
  {
    lvalueStack.add(v);
  }
  public LValue popLValue()
  {
    return lvalueStack.remove(lvalueStack.size() - 1);
  }
  
  /**
   * We track our execution position with a reference to a node
   * in the AST, and an index for a possible subposition within that
   * node. 
   */
  static class InstructionPointerEntry
  {
    AstNode node;
    int idx;
    NodeHandlers instructionHandlers;
  }
  static class InstructionPointer
  {
    private List<InstructionPointerEntry> ip = new ArrayList<>();
    private int ipHead = -1;
    public void advanceIdx()
    {
      ip.get(ipHead).idx++;
    }
    public void setIdx(int newIdx)
    {
      ip.get(ipHead).idx = newIdx;
    }
    public void pushAndAdvanceIdx(AstNode newNode, NodeHandlers instructionHandlers)
    {
      ip.get(ipHead).idx++;
      push(newNode, instructionHandlers);
    }
    private void push(AstNode newNode, NodeHandlers instructionHandlers)
    {
      if (ipHead + 1 >= ip.size())
        ip.add(new InstructionPointerEntry());
      ipHead++;
      ip.get(ipHead).node = newNode;
      ip.get(ipHead).instructionHandlers = instructionHandlers;
      ip.get(ipHead).idx = 0;
    }
    public void pop()
    {
      ipHead--;
    }
    public InstructionPointerEntry peekHead()
    {
      return ip.get(ipHead);
    }
    public AstNode peekNode(int idx)
    {
      if (ipHead - idx < 0) return null;
      return ip.get(ipHead - idx).node;
    }
    public boolean hasNext()
    {
      return ipHead >= 0;
    }
  }
  public InstructionPointer ip = new InstructionPointer();
  
  /**
   * Handles calls into different functions/methods
   */
  static class StackFrame
  {
    public StackFrame()
    {
      ip = new InstructionPointer();
      valueStack = new ArrayList<>();
      lvalueStack = new ArrayList<>();
    }
    /**
     * Holds the instruction pointer of where execution is in the 
     * current function/method
     */
    InstructionPointer ip;
    
    /**
     * Local variables and call parameters available in the function/method
     */
    VariableScope topScope;
    
    /**
     * Stack of values where values can be stashed while expressions
     * are being evaluated.
     */
    private List<Value> valueStack;

    /**
     * For handling some correctness assertions for the assignment
     * stuff, we have extra variables for storing values we can later
     * assert against. This value should later be put into the stack
     * frame.
     */
    private int lValueStackAssertCheck;

    /**
     * When doing an assignment into a variable, we don't want to track
     * the value of the variable, but its location, so we'll use a separate
     * data structure and stack for tracking and storing that information.
     */
    // TODO: Change LValue stuff to have a Value pool and to overwrite values
    // instead of passing around references
    private List<LValue> lvalueStack;
  }
  private List<StackFrame> stackFrames = new ArrayList<>();
  private StackFrame topStackFrame;
  
  public void pushStackFrame(AstNode node, NodeHandlers instructionHandlers)
  {
    StackFrame frame = new StackFrame();
    frame.ip.push(node, instructionHandlers);
    frame.topScope = getGlobalScope();
    
    stackFrames.add(frame);
    topStackFrame = frame;
    
    // Move current machine registers and other state to point to the current
    // stack frame
    ip = frame.ip;
    valueStack = frame.valueStack;
    lvalueStack = frame.lvalueStack;
    pushNewScope();
  }
  
  public void popStackFrame()
  {
    stackFrames.remove(stackFrames.size() - 1);
    if (!stackFrames.isEmpty()) 
    {
      topStackFrame = stackFrames.get(stackFrames.size() - 1);
      
      // Move current machine registers and other state to point to the previous
      // stack frame
      ip = topStackFrame.ip;
      valueStack = topStackFrame.valueStack;
      lvalueStack = topStackFrame.lvalueStack;
    }
    else
      topStackFrame = null;
  }
  
  /**
   * We occasionally need to call blocking functions, but since JavaScript 
   * doesn't support that, we need to exit the interpreter loop and then
   * repeatedly poll to see when we can restart the interpreter
   */
  public static class PrimitiveBlockingFunctionReturn
  {
    boolean isBlocked = true;
    /** Called each time the interpreter is started up in case some code needs to run to check if blocking is finished. Can be null */
    Runnable checkDone = null;
    /** Function needs to set the returnValue to whatever the function should return */
    Value returnValue;
    
    public void unblockAndReturn(Value returnValue)
    {
      this.returnValue = returnValue;
      isBlocked = false;
    }
    // I'm not sure if it's a good idea to let the blocked function restart the interpreter through here
    // or whether it's better to have that all be external to the blocking framework. 
//    MachineContext machine;
//    void restartMachine()
//    {
//      machine.runToCompletion();
//    }
  }
  PrimitiveBlockingFunctionReturn blocked;
  void waitOnBlockingFunction(PrimitiveBlockingFunctionReturn blockWait)
  {
    blocked = blockWait;
  }
  void testBlockFinished()
  {
    // Check if the thing we're blocking on is still blocked
    if (blocked.isBlocked)
      return;
    // If we're unblocked then save the return value on the stack and clear the blocking state
    pushValue(blocked.returnValue);
    blocked = null;
  }
  
  /**
   * Visits a node with a bunch of triggers for handling certain special cases
   */
  private static void visitNodeRecursively(AstNode node, int idx, MachineContext machine, NodeHandlers triggers) throws RunException
  {
    // See if we have a visitor registered for this production rule of symbols
    MachineNodeVisitor match = triggers.get(node.symbols);
    if (match != null)
    {
      match.handleNode(machine, node, idx);
    }
    else
    {
      // If we've visited all the children, then exit while leaving the
      // state of the stack in whatever way it was before (which may or may not
      // be safe depending on whether we've encoded the traversals correctly)
      if (idx == node.children.size())
      {
        machine.ip.pop();
        return;
      }
      // Visit the next child
      machine.ip.pushAndAdvanceIdx(node.children.get(idx), triggers);
    }
  }
  
  /**
   * A callback for running the code for an AstNode
   */
  @FunctionalInterface public static interface MachineNodeVisitor
  {
    public void handleNode(MachineContext machine, AstNode node, int idx) throws RunException;
  }
  /**
   * Groups a bunch of callbacks for different types of AstNodes together
   */
  public static class NodeHandlers extends HashMap<List<Symbol>, MachineNodeVisitor>
  {
    private static final long serialVersionUID = 1L;
    public NodeHandlers add(List<Symbol> match, MachineNodeVisitor callback)
    {
      put(match, callback);
      return this;
    }
  }
  
  /**
   * Sets the code that should be run in the machine
   */
//  public void setStart(AstNode node, NodeHandlers instructionHandlers)
//  {
//    ip.push(node, instructionHandlers);
//  }
  
  /**
   * Runs the next instruction or part of an instruction
   */
  public void runNextStep() throws RunException
  {
    InstructionPointerEntry nextInstruction = ip.peekHead();
    visitNodeRecursively(nextInstruction.node, nextInstruction.idx, this, nextInstruction.instructionHandlers);
  }

  /**
   * This is used mainly for testing. It runs until there's no more code
   * in the current stack frame to execute. If a function explicitly 
   * returns prematurely, the code will continue execution though
   * Returns true iff execution has finished (may return false if execution
   * is blocked and needs to be retried later)
   */
  boolean runToEndOfFrame() throws RunException
  {
    while (ip.hasNext())
    {
      if (blocked != null)
      {
        if (blocked.checkDone != null)
          blocked.checkDone.run();
        if (blocked.isBlocked)
          return false;
        testBlockFinished();
      }
      runNextStep();
    }
    return true;
  }
  
  /**
   * Keeps running instructions until there are no more instructions to run.
   * Returns true iff execution has finished (may return false if execution
   * is blocked and needs to be retried later)
   */
  public boolean runToCompletion() throws RunException
  {
    try {
      while (topStackFrame != null)
      {
        if (!runToEndOfFrame())
          return false;
        
        // Finished executing the current stack frame, so exit it
        popStackFrame();
      }
      return true;
    } 
    catch (RunException e)
    {
      // Augment the exception information with info about where in
      // the code the error was triggered.
      int idx = 0;
      // Walk through the nodes from the top of the stack to the
      // bottom until we hit one with an associated token
      for (AstNode node = ip.peekNode(idx); node != null; idx++)
      {
        Token errorToken = node.scanForToken();
        if (errorToken != null)
        {
          e.setErrorTokenSource(errorToken);
          break;
        }
      }
      throw e;
    }
  }
}
