package org.programmingbasics.plom.core.interpreter;

public class RunException extends Exception
{
  private static final long serialVersionUID = 1L;
  public RunException() {}
  public RunException(String msg) { super(msg); }
  public RunException(Throwable t) { super(t); }
}
