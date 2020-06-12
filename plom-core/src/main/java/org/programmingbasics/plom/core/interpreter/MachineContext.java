package org.programmingbasics.plom.core.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.AstNode;

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
  public Value popValue(Value v)
  {
    return valueStack.remove(valueStack.size() - 1);
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
  }
  List<InstructionPointerEntry> ip = new ArrayList<>();
  int ipHead = 0;
  public void ipAdvanceIdx()
  {
    ip.get(ipHead).idx++;
  }
  public void ipSetIdx(int newIdx)
  {
    ip.get(ipHead).idx = newIdx;
  }
  public void ipPushAndAdvanceIdx(AstNode newNode)
  {
    ip.get(ipHead).idx++;
    if (ipHead + 1 >= ip.size())
    {
      ip.add(new InstructionPointerEntry());
    }
    ipHead++;
    ip.get(ipHead).node = newNode;
    ip.get(ipHead).idx = 0;
  }
  public void ipPop()
  {
    ipHead--;
  }
}
