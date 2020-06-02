package org.programmingbasics.plom.astgen;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Production
{
  public Symbol from;
  public List<Symbol> to;
//  public List<Symbol> toFull;
  public String name;

  public Production(String name, Symbol from, Symbol[] to)
  {
    this.name = name;
    this.from = from;
    this.to = Arrays.asList(to);
  }

  @Override
  public String toString()
  {
    return from.name() + " -> " + String.join(" ", to.stream().map((sym) -> sym.name()).collect(Collectors.toList()));
  }
}
