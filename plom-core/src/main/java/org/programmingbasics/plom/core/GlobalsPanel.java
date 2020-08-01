package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

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
import elemental.html.InputElement;

/**
 * UI code for listing global variables
 */
public class GlobalsPanel
{
  Document doc = Browser.getDocument();
  SimpleEntry simpleEntry;
  
  GlobalsPanel(DivElement mainDiv, ModuleCodeRepository repository, GlobalsPanelViewSwitcher callback)
  {
    Document doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
    
    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));
    simpleEntry.setVisible(false);
    
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
    
    // For adding global variables
    Element newGlobalAnchor = mainDiv.querySelector(".globalVarsHeading a");
    newGlobalAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newVarName = "variable";
      int newVarNumber = 0;
      while (repository.hasFunctionWithName(newVarName))
      {
        newVarNumber++;
        newVarName = "variable " + newVarNumber;
      }
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newVarName),
          new StatementContainer());
      repository.addFunction(func);
      
    }, false);
   
    List<DivElement> globalVarDivs = new ArrayList<>();
    for (String globalName: repository.getAllGlobalVars())
    {
      addGlobalVarEntry(mainDiv, globalName, repository.getGlobalVarType(globalName), globalVarDivs);
    }
    
  }

  private void addGlobalVarEntry(DivElement mainDiv,
      String name, Token.ParameterToken type, 
      List<DivElement> varDivs)
  {
    DivElement div = doc.createDivElement();
    div.setInnerHTML("<div class=\"global_var\">.<input size=\"15\" type=\"text\"> <div class=\"typeEntry\">&nbsp;</div></div>");
    ((InputElement)div.querySelector("input")).setValue(name);
    varDivs.add(div);
    mainDiv.querySelector(".globalVarsList").appendChild(div);
    TypeEntryField typeField = new TypeEntryField(type, (DivElement)div.querySelector(".typeEntry"), simpleEntry, false);
    typeField.render();
    
//    DivElement varDiv = doc.createDivElement();
//    varDiv.setInnerHTML("<div style=\"padding-left: 1em;\" class=\"method_args_var\"><div style=\"min-width: 1em; display: inline-block;\"><a href=\"#\">-</a></div>.<input size=\"15\" type=\"text\"><div class=\"typeEntry\">&nbsp;</div></div>");
//    ((InputElement)varDiv.querySelector("input")).setValue(argNameVal);
//    argEls.add(varDiv);
//    mainDiv.querySelector(".method_args").appendChild(varDiv);
//
//    // argument type
//    TypeEntryField typeField = new TypeEntryField(argTypeVal, (DivElement)varDiv.querySelector(".typeEntry"), simpleEntry, false);
//    argTypeFields.add(typeField);
//    typeField.render();
//
//    // remove arg button
//    varDiv.querySelector(".method_args_var a").addEventListener(Event.CLICK, (evt) -> {
//      evt.preventDefault();
//      int idx = argEls.indexOf(varDiv);
//      if (idx != 0)
//      {
//        DivElement div = nameEls.remove(idx);
//        div.getParentElement().removeChild(div);
//      }
//      else if (nameEls.size() > 1)
//      {
//        DivElement div = nameEls.get(1);
//        nameEls.remove(div);
//        div.getParentElement().removeChild(div);
//      }
//      argEls.remove(varDiv);
//      varDiv.getParentElement().removeChild(varDiv);
//      argTypeFields.remove(typeField);
//    }, false);
  }

  public static interface GlobalsPanelViewSwitcher
  {
    void loadFunctionCodeView(String fnName);
    void loadFunctionSignatureView(FunctionSignature sig);
  }
}
