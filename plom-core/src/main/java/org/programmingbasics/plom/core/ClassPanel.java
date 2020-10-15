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
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;

/**
 * UI panel for modifying classes
 */
public class ClassPanel
{
  Document doc = Browser.getDocument();
  SimpleEntry simpleEntry;
  DivElement mainDiv;
  ModuleCodeRepository repository; 
  ClassDescription cls;
  ClassPanelViewSwitcher viewSwitchCallback;
  
  ClassPanel(DivElement mainDiv, ModuleCodeRepository repository, ClassDescription cls, ClassPanelViewSwitcher callback)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
    this.cls = cls;
    this.viewSwitchCallback = callback;
    rebuildView();
  }
  
  public void rebuildView()
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getClassPanelHtml().getText());

    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));
    simpleEntry.setVisible(false);

    // For setting class name
    InputElement nameAnchor = (InputElement)mainDiv.querySelector(".className input");
    nameAnchor.setValue(cls.name);
    nameAnchor.addEventListener(Event.CHANGE, (e) -> {
      e.preventDefault();
      cls.setName(nameAnchor.getValue());
    }, false);
    
    
    // For adding methods
    Element newFunctionAnchor = mainDiv.querySelector(".methodsHeading a");
    newFunctionAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newMethodName = ModuleCodeRepository.findUniqueName("method", (name) -> !cls.hasMethodWithName(name));
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newMethodName),
          new StatementContainer());
      cls.addMethod(func);

      viewSwitchCallback.loadMethodSignatureView(cls, func);
    }, false);
    
    // List of instance methods
    createMethodList(mainDiv.querySelector(".methodsList"), cls.getInstanceMethods());

    // For adding static methods
    mainDiv.querySelector(".staticMethodsHeading a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newMethodName = ModuleCodeRepository.findUniqueName("method", (name) -> !cls.hasMethodWithName(name));
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newMethodName)
              .setIsStatic(true),
          new StatementContainer());
      cls.addMethod(func);

      viewSwitchCallback.loadMethodSignatureView(cls, func);
    }, false);
    
    // For adding constructor methods
    mainDiv.querySelector(".constructorMethodsHeading a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newMethodName = ModuleCodeRepository.findUniqueName("method", (name) -> !cls.hasMethodWithName(name));
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), newMethodName)
              .setIsConstructor(true),
          new StatementContainer());
      cls.addMethod(func);

      viewSwitchCallback.loadMethodSignatureView(cls, func);
    }, false);
    
    // List of static methods
    createMethodList(mainDiv.querySelector(".staticMethodsList"), cls.getStaticMethods());

    // List of constructor methods
    createMethodList(mainDiv.querySelector(".constructorMethodsList"), cls.getConstructorMethods());
    
    // Variables
    Element newVarAnchor = mainDiv.querySelector(".varsHeading a");
    newVarAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      String newVarName = "";
      cls.addVarAndResetIds(newVarName, Token.ParameterToken.fromContents("@object", Symbol.AtType));
      rebuildView();
    }, false);
   
    List<DivElement> varDivs = new ArrayList<>();
    for (VariableDescription v: cls.getAllVars())
    {
      addVarEntry(mainDiv, v, varDivs);
    }

  }

  private void createMethodList(Element methodListEl, List<FunctionDescription> fnList)
  {
    for (FunctionDescription fn: fnList)
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.setTextContent(fn.sig.getLookupName());
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        viewSwitchCallback.loadMethodCodeView(cls, fn);
      }, false);
      DivElement div = doc.createDivElement();
      div.appendChild(a);
      methodListEl.appendChild(div);
    }
  }
  
  private void addVarEntry(DivElement mainDiv,
      VariableDescription v,
      List<DivElement> varDivs)
  {
    String name = v.name;
    Token.ParameterToken type = v.type; 
    DivElement div = doc.createDivElement();
    div.setInnerHTML("<div class=\"class_var\">.<input size=\"15\" type=\"text\"> <div class=\"typeEntry\">&nbsp;</div></div>");
    ((InputElement)div.querySelector("input")).setValue(name);
    varDivs.add(div);
    mainDiv.querySelector(".varsList").appendChild(div);
    TypeEntryField typeField = new TypeEntryField(type, (DivElement)div.querySelector(".typeEntry"), simpleEntry, false,
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes));
        },
        (context) -> {});
    typeField.setChangeListener((newType, isFinal) -> {
      v.type = newType;
      cls.updateVariable(v);
    });
    typeField.render();
    
    InputElement nameInput = (InputElement)div.querySelector("input"); 
    nameInput.addEventListener(Event.CHANGE, (evt) -> {
      v.name = nameInput.getValue(); 
      cls.updateVariable(v);
    }, false);
  }

  
  public static interface ClassPanelViewSwitcher
  {
    void loadMethodCodeView(ClassDescription cls, FunctionDescription method);
    void loadMethodSignatureView(ClassDescription cls, FunctionDescription method);
  }
}
