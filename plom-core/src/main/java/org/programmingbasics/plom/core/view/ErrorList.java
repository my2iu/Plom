package org.programmingbasics.plom.core.view;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.Token;

public class ErrorList
{
  List<Exception> codeErrors = new ArrayList<>();
  public void add(Exception e)
  {
    codeErrors.add(e);
  }
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
}
