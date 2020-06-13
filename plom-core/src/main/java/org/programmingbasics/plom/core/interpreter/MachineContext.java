package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.AstNode.RecursiveWalkerVisitor;
import org.programmingbasics.plom.core.ast.AstNode.VisitorTriggers;
import org.programmingbasics.plom.core.ast.gen.Symbol;

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
  List<Value> valueStack = new ArrayList<>();
  
  public void pushValue(Value v)
  {
    valueStack.add(v);
  }
  public Value popValue()
  {
    return valueStack.remove(valueStack.size() - 1);
  }
  public int valueStackSize()
  {
    return valueStack.size();
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
    if (ipHead + 1 >= ip.size())
    {
      ip.add(new InstructionPointerEntry());
    }
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
  private static void visitNodeRecursively(AstNode node, int idx, MachineContext machine, NodeHandlers triggers)
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
    public void handleNode(MachineContext machine, AstNode node, int idx);
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
    ip.add(new InstructionPointerEntry());
    ipHead++;
    ip.get(ipHead).node = node;
    ip.get(ipHead).instructionHandlers = instructionHandlers;
    ip.get(ipHead).idx = 0;
  }
  
  /**
   * Runs the next instruction or part of an instruction
   */
  public void runNextStep()
  {
    InstructionPointerEntry nextInstruction = ipPeekHead();
    visitNodeRecursively(nextInstruction.node, nextInstruction.idx, this, nextInstruction.instructionHandlers);
  }

  /**
   * Keeps running instructions until there are no more instructions to run
   */
  public void runToCompletion()
  {
    while (ipHead >= 0)
      runNextStep();
  }
}
