package org.programmingbasics.plom.core.interpreter;

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

}
