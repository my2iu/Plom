package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ModuleCodeRepository.VariableDescription;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;
import jsinterop.annotations.JsFunction;

/**
 * UI code for listing global variables
 */
public class GlobalsPanel
{
  Document doc = Browser.getDocument();
  SimpleEntry simpleEntry;
  ModuleCodeRepository repository;
  DivElement mainDiv;
  LoadFunctionCodeViewCallback viewSwitchCallback;
  LoadFunctionSigViewCallback functionSigCallback;
  LoadClassViewCallback classViewCallback;
  
  GlobalsPanel(DivElement mainDiv, ModuleCodeRepository repository, LoadFunctionCodeViewCallback callback, LoadFunctionSigViewCallback functionSigCallback, LoadClassViewCallback classViewCallback)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
    this.viewSwitchCallback = callback;
    this.functionSigCallback = functionSigCallback;
    this.classViewCallback = classViewCallback;
    rebuildView();
  }
  
  void rebuildView()
  {
    Document doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
    
    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));
    simpleEntry.setVisible(false);

    // For adding classes
    Element newClassAnchor = mainDiv.querySelector(".classesHeading a");
    newClassAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      // Find a unique class name
      String newClassName = ModuleCodeRepository.findUniqueName("class", (name) -> !repository.hasClassWithName(name));
      ClassDescription c = repository.addClassAndResetIds(newClassName);
      // Switch to view the class
      classViewCallback.load(c, true);
    }, false);

    // List of classes
    Element classListEl = mainDiv.querySelector(".classesList");
    for (ClassDescription cls: repository.getAllClassesSorted())
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setTextContent(cls.name);
      a.setHref("#");
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        classViewCallback.load(cls, false);
      }, false);
      DivElement div = doc.createDivElement();
      if (cls.isImported || cls.isBuiltIn)
        div.getClassList().add("moduleImported");
      div.appendChild(a);
      classListEl.appendChild(div);
    }
    
    // For adding functions
    Element newFunctionAnchor = mainDiv.querySelector(".functionsHeading a");
    newFunctionAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      // Find a unique function name
      String newFunctionName = ModuleCodeRepository.findUniqueName("function", (name) -> repository.getFunctionWithName(name) == null);
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newFunctionName),
          new StatementContainer());
      repository.addFunctionAndResetIds(func);
      
      if (functionSigCallback != null)
        functionSigCallback.load(func, true);
    }, false);
    
    // List of functions
    Element functionListEl = mainDiv.querySelector(".functionList");
    for (FunctionDescription fnName: repository.getAllFunctionSorted())
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.setTextContent(fnName.sig.getLookupName());
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        viewSwitchCallback.load(fnName);
      }, false);
      DivElement div = doc.createDivElement();
      if (fnName.isImported)
        div.getClassList().add("moduleImported");
      div.appendChild(a);
      functionListEl.appendChild(div);
    }
    
    // For adding global variables
    Element newGlobalAnchor = mainDiv.querySelector(".globalVarsHeading a");
    newGlobalAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newVarName = "";
      int newId = repository.addGlobalVarAndResetIds(newVarName, Token.ParameterToken.fromContents("@object", Symbol.AtType));
      rebuildView();
      NodeList nodes = mainDiv.querySelectorAll("div.global_var");
      // Assume ids are linear
      if (newId < nodes.length())
      {
        Element el = (Element)nodes.item(newId);
        el.scrollIntoView();
        ((InputElement)el.querySelector("input")).focus();
        ((InputElement)el.querySelector("input")).select();
      }
    }, false);
   
    List<DivElement> globalVarDivs = new ArrayList<>();
    for (VariableDescription v: repository.getAllGlobalVarsSorted())
    {
      addGlobalVarEntry(mainDiv, v, globalVarDivs);
    }
  }

  private void addGlobalVarEntry(DivElement mainDiv,
      VariableDescription v,
      List<DivElement> varDivs)
  {
    String name = v.name;
    Token.ParameterToken type = v.type; 
    DivElement div = doc.createDivElement();
    div.setInnerHTML("<div class=\"global_var\">.<input size=\"15\" type=\"text\" autocapitalize=\"off\"> <div class=\"typeEntry\">&nbsp;</div></div>");
    if (v.isImported)
      div.getClassList().add("moduleImported");

    ((InputElement)div.querySelector("input")).setValue(name);
    varDivs.add(div);
    mainDiv.querySelector(".globalVarsList").appendChild(div);
    TypeEntryField typeField = new TypeEntryField(type, (DivElement)div.querySelector(".typeEntry"), simpleEntry, false,
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes));
        },
        (context) -> {});
    typeField.setChangeListener((newType, isFinal) -> {
      v.type = newType; 
      repository.updateGlobalVariable(v);
    });
    typeField.render();
    
    InputElement nameInput = (InputElement)div.querySelector("input"); 
    nameInput.addEventListener(Event.CHANGE, (evt) -> {
      v.name = nameInput.getValue(); 
      repository.updateGlobalVariable(v);
    }, false);
  }

  @JsFunction
  public static interface LoadFunctionSigViewCallback
  {
    void load(FunctionDescription sig, boolean isNew);
  }

  @JsFunction
  public static interface LoadClassViewCallback
  {
    void load(ClassDescription cls, boolean isNew);
    
  }
  @JsFunction
  public static interface LoadFunctionCodeViewCallback
  {
    void load(FunctionDescription fnName);
  }
}
