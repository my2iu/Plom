package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;

/**
 * UI panel for modifying classes
 */
public class ClassPanel
{
  ClassPanel(DivElement mainDiv, ModuleCodeRepository repository, ClassPanelViewSwitcher callback)
  {
    Document doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getClassPanelHtml().getText());
    
    // For adding functions
    Element newFunctionAnchor = mainDiv.querySelector(".functionsHeading a");
    newFunctionAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newFunctionName = "function";
      int newFunctionNumber = 0;
      while (repository.hasFunctionWithName(newFunctionName))
      {
        newFunctionNumber++;
        newFunctionName = "function " + newFunctionNumber;
      }
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newFunctionName),
          new StatementContainer());
      repository.addFunction(func);
      
      callback.loadFunctionSignatureView(func.sig);
    }, false);
    
    // List of functions
    Element functionListEl = mainDiv.querySelector(".functionList");
    
    for (String fnName: repository.getAllFunctions())
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.setTextContent(fnName);
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        callback.loadFunctionCodeView(fnName);
      }, false);
      DivElement div = doc.createDivElement();
      div.appendChild(a);
      functionListEl.appendChild(div);
    }
  }
  
  public static interface ClassPanelViewSwitcher
  {
    void loadFunctionCodeView(String fnName);
    void loadFunctionSignatureView(FunctionSignature sig);
  }
}
