package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.FindToken;
import org.programmingbasics.plom.core.ast.Token;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class RunException extends Exception
{
  private static final long serialVersionUID = 1L;
  private Token errorTokenSource;
  private ProgramCodeLocation location;
  
  public RunException(String msg, Throwable t, ProgramCodeLocation loc) { super(msg, t); this.location = loc; }
  @JsIgnore public RunException() { this(null, null, null); }
  @JsIgnore public RunException(String msg) { this(msg, null, null); }
  @JsIgnore public RunException(String msg, ProgramCodeLocation loc) { this(msg, null, loc); }
  @JsIgnore public RunException(String msg, Throwable t) { this(msg, t, null); }
  @JsIgnore public RunException(Throwable t) { this(null, t, null); }

  @JsIgnore
  public static RunException withLocationFromNode(String msg, AstNode node, MachineContext machine)
  {
    return new RunException(msg).addProgramLocationFromNodeIfNeeded(node, machine);
  }
  
  /** Stores the location where an error occurred */
  public void setErrorLocation(ProgramCodeLocation loc)
  {
    location = loc;
  }
  
  public ProgramCodeLocation getErrorLocation()
  {
    return location;
  }
  
  /** Used for computing the line number that triggered the error */
  public void setErrorTokenSource(Token token)
  {
    errorTokenSource = token;
  }
  
  public Token getErrorTokenSource()
  {
    return errorTokenSource;
  }

  /** 
   * Sometimes, we can add additional context about an exception
   * outside of the location where we actually throw the exception.
   * So instead of wrapping an exception, we can use this method
   * to add a program location before rethrowing the same exception
   */
  public RunException addProgramLocationFromNodeIfNeeded(AstNode node, MachineContext machine)
  {
    if (getErrorLocation() == null)
    {
      if (node != null)
      {
        Token errorToken = node.scanForToken();
        if (errorToken != null)
        {
          CodePosition errorPos = null;
          if (machine.getTopStackFrame().sourceLookup.isPresent())
            errorPos = FindToken.tokenToPosition(errorToken, machine.getTopStackFrame().sourceLookup.get());
          setErrorLocation(ProgramCodeLocation.fromCodeUnit(machine.getTopStackFrame().codeUnit, errorPos));
        }
      }
      else
        setErrorLocation(ProgramCodeLocation.fromCodeUnit(machine.getTopStackFrame().codeUnit, null));
    }
    return this;
  }
  
  /**
   * This creates an error pointing to the start of the current function/method.
   * It creates a code position pointing to the start of the function,
   * even if the function is empty and has no nodes.
   */
  public RunException addProgramLocationToMethodStartIfNeeded(MachineContext machine)
  {
    if (getErrorLocation() == null)
    {
      setErrorLocation(ProgramCodeLocation.fromCodeUnit(machine.getTopStackFrame().codeUnit, CodePosition.fromOffsets(0)));
    }
    return this;
  }

}
