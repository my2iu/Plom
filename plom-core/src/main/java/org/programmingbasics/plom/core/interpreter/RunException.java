package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.Token;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class RunException extends Exception
{
  private static final long serialVersionUID = 1L;
  private Token errorTokenSource;
  
  public RunException(String msg, Throwable t) { super(msg, t); }
  @JsIgnore public RunException() { this(null, null); }
  @JsIgnore public RunException(String msg) { this(msg, null); }
  @JsIgnore public RunException(Throwable t) { this(null, t); }

  
  
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
