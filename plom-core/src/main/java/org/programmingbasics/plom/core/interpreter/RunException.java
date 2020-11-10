package org.programmingbasics.plom.core.interpreter;

import org.programmingbasics.plom.core.ast.Token;

import jsinterop.annotations.JsType;

@JsType
public class RunException extends Exception
{
  private static final long serialVersionUID = 1L;
  private Token errorTokenSource;
  
  public RunException() {}
  public RunException(String msg) { super(msg); }
  public RunException(Throwable t) { super(t); }
  
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
