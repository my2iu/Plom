package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

public class ErrorList
{
  List<Exception> codeErrors = new ArrayList<>();
  public void add(Exception e)
  {
    codeErrors.add(e);
  }
  public List<Exception> getCodeErrors() { return codeErrors; }
  public boolean containsToken(Token token)
  {
    for (Exception e: codeErrors)
    {
      if (e instanceof ParseToAst.ParseException)
      {
        ParseToAst.ParseException parseException = (ParseToAst.ParseException)e;
        if (parseException.token == token) return true;
      }
    }
    return false;
  }
  public void clear()
  {
    codeErrors.clear();
  }
}
