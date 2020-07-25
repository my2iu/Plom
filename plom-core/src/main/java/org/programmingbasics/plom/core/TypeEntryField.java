package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.view.CodeRenderer;

import elemental.html.DivElement;

/**
 * Handles UI logic for a field where a type can be entered
 */
public class TypeEntryField
{
  public TypeEntryField(DivElement div, Token.ParameterToken type)
  {
    this.type = type;
    fieldDiv = div;
  }
  DivElement fieldDiv;
  Token.ParameterToken type;
  
  public void render()
  {
    CodeRenderer.render(fieldDiv, 
        type == null ? new StatementContainer() : new StatementContainer(new TokenContainer(type)), 
            null, null, new ErrorList());
    if (type == null)
      fieldDiv.setTextContent("\u00A0");

  }
}
