package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;
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
   * Scope where bindings of names to values for variables can be looked up
   */
  VariableScope scope;
  
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
  private int lValueStackAssertCheck;
  public void setLValueAssertCheck(int val) { lValueStackAssertCheck = val; } 
  public int getLValueAssertCheck() { return lValueStackAssertCheck; } 
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
  List<InstructionPointerEntry> ip = new ArrayList<>();
  int ipHead = -1;
  public void ipAdvanceIdx()
  {
    ip.get(ipHead).idx++;
  }
  public void ipSetIdx(int newIdx)
  {
    ip.get(ipHead).idx = newIdx;
  }
  public void ipPushAndAdvanceIdx(AstNode newNode, NodeHandlers instructionHandlers)
  {
    ip.get(ipHead).idx++;
    ipPush(newNode, instructionHandlers);
  }
  private void ipPush(AstNode newNode, NodeHandlers instructionHandlers)
  {
    if (ipHead + 1 >= ip.size())
      ip.add(new InstructionPointerEntry());
    ipHead++;
    ip.get(ipHead).node = newNode;
    ip.get(ipHead).instructionHandlers = instructionHandlers;
    ip.get(ipHead).idx = 0;
  }
  public void ipPop()
  {
    ipHead--;
  }
  public InstructionPointerEntry ipPeekHead()
  {
    return ip.get(ipHead);
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
        machine.ipPop();
        return;
      }
      // Visit the next child
      machine.ipPushAndAdvanceIdx(node.children.get(idx), triggers);
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
  public void setStart(AstNode node, NodeHandlers instructionHandlers)
  {
    ipPush(node, instructionHandlers);
  }
  
  /**
   * Runs the next instruction or part of an instruction
   */
  public void runNextStep() throws RunException
  {
    InstructionPointerEntry nextInstruction = ipPeekHead();
    visitNodeRecursively(nextInstruction.node, nextInstruction.idx, this, nextInstruction.instructionHandlers);
  }

  /**
   * Keeps running instructions until there are no more instructions to run
   */
  public void runToCompletion() throws RunException
  {
    while (ipHead >= 0)
      runNextStep();
  }
}
