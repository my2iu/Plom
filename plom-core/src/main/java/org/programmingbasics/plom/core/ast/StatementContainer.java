package org.programmingbasics.plom.core.ast;

import java.util.ArrayList;
import java.util.List;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class StatementContainer
{
  public StatementContainer() {}
  @JsIgnore
  public StatementContainer(TokenContainer...lines)
  {
    this();
    for (TokenContainer line: lines)
      statements.add(line);
  }
  public List<TokenContainer> statements = new ArrayList<>();
   
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((statements == null) ? 0 : statements.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    StatementContainer other = (StatementContainer) obj;
    if (statements == null)
    {
      if (other.statements != null) return false;
    }
    else if (!statements.equals(other.statements)) return false;
    return true;
  }
  
}
