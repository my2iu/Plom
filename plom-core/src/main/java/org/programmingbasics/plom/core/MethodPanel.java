package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;

public class MethodPanel
{
  MethodPanel(DivElement mainDiv, ModuleCodeRepository repository, FunctionSignature sig, boolean isNew)
  {
    this.repository = repository;

    doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getMethodPanelHtml().getText());
    
    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"));
    simpleEntry.setVisible(false);
    
    showMethod(mainDiv, sig, isNew);
  }

  final Document doc;
  SignatureListener listener;
  ModuleCodeRepository repository;
  SimpleEntry simpleEntry;
  TypeEntryField returnTypeField = null;
  
  public static interface SignatureListener
  {
    public void onSignatureChange(FunctionSignature newSig, boolean isFinal);
  }

  public void setListener(SignatureListener listener)
  {
    this.listener = listener;
  }
  
  public void showMethod(DivElement containerDiv, FunctionSignature sig, boolean isNew)
  {
    List<DivElement> nameEls = new ArrayList<>();
    List<DivElement> argEls = new ArrayList<>();
    List<TypeEntryField> argTypeFields = new ArrayList<>();

    MethodNameWidget methodWidget = new MethodNameWidget(sig);
    containerDiv.querySelector(".methoddetails").appendChild(methodWidget.getBaseElement());
    
    // Fill in the function name
    nameEls.add((DivElement)containerDiv.querySelector("div.method_name"));
    ((InputElement)nameEls.get(0).querySelector("input")).setValue(sig.nameParts.get(0));

    for (int n = 0; n < sig.argNames.size(); n++)
      addMethodPanelArg(containerDiv, sig.nameParts.get(n), sig.argNames.get(n), sig.argTypes.get(n), nameEls, argEls, argTypeFields);
    
    // Add argument button
    containerDiv.querySelector(".method_args_add a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      addMethodPanelArg(containerDiv, "", "", null, nameEls, argEls, argTypeFields);
    }, false);

    // Render the return type
    if (!sig.isConstructor)
    {
      returnTypeField = new TypeEntryField(sig.returnType, (DivElement)containerDiv.querySelector(".method_return .typeEntry"), simpleEntry, true,
          (scope, coreTypes) -> {
            StandardLibrary.createGlobals(null, scope, coreTypes);
            scope.setParent(new RepositoryScope(repository, coreTypes));
          },
          (context) -> {});
      returnTypeField.render();
    }
    else
      // Constructors have no return type
      containerDiv.querySelector(".method_return").getStyle().setDisplay(Display.NONE);      

    // Ok Button
    AnchorElement okButton = (AnchorElement)containerDiv.querySelector("a.done");
    okButton.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      // TODO: Remove OK button and just have changes automatically be applied
      // TODO: Check validity of function name
      List<String> nameParts = new ArrayList<>();
      List<String> argNames = new ArrayList<>();
      List<Token.ParameterToken> argTypes = new ArrayList<>();
      Token.ParameterToken returnType = null;
      for (DivElement div: nameEls)
        nameParts.add(((InputElement)div.querySelector("input")).getValue());
      for (DivElement div: argEls)
        argNames.add(((InputElement)div.querySelector("plom-autoresizing-input")).getValue());
      for (TypeEntryField typeField: argTypeFields)
        argTypes.add(typeField.type);
      if (returnTypeField != null)
        returnType = returnTypeField.type;
      FunctionSignature newSig = FunctionSignature.from(returnType, nameParts, argNames, argTypes, sig);
      if (listener != null)
        listener.onSignatureChange(newSig, true);
    }, false);
    
    // When creating a new function/method, the name should be selected so that you can immediately give it a new name 
    if (isNew)
    {
      ((InputElement)nameEls.get(0).querySelector("input")).focus();  // Documentation is unclear as to whether select() also sets focus or not
      ((InputElement)nameEls.get(0).querySelector("input")).select();
    }
  }

  private void addMethodPanelArg(DivElement mainDiv,
      String nameVal, String argNameVal, Token.ParameterToken argTypeVal, 
      List<DivElement> nameEls, List<DivElement> argEls, 
      List<TypeEntryField> argTypeFields)
  {
    if (!argEls.isEmpty())
    {
      DivElement nameDiv = doc.createDivElement();
      nameDiv.setInnerHTML("<div style=\"padding-left: 1em;\" class=\"method_args_name\"><input size=\"15\" type=\"text\" autocapitalize=\"off\">:</div>");
      ((InputElement)nameDiv.querySelector("input")).setValue(nameVal);
      nameEls.add(nameDiv);
      mainDiv.querySelector(".method_args").appendChild(nameDiv);
    }
    DivElement varDiv = doc.createDivElement();
    varDiv.setInnerHTML("<div style=\"padding-left: 1em;\" class=\"method_args_var\"><div style=\"min-width: 1em; margin-right: 0.2em; display: inline-block;\"><a href=\"#\" aria-label=\"delete argument\" class=\"plomUiRemoveButton\"></a></div>.<plom-autoresizing-input></plom-autoresizing-input><div class=\"typeEntry\">&nbsp;</div></div>");
    ((InputElement)varDiv.querySelector("plom-autoresizing-input")).setValue(argNameVal);
    argEls.add(varDiv);
    mainDiv.querySelector(".method_args").appendChild(varDiv);

    // argument type
    TypeEntryField typeField = new TypeEntryField(argTypeVal, (DivElement)varDiv.querySelector(".typeEntry"), simpleEntry, false,
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes));
        },
        (context) -> {});
    argTypeFields.add(typeField);
    typeField.render();

    // remove arg button
    varDiv.querySelector(".method_args_var a").addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      int idx = argEls.indexOf(varDiv);
      if (idx != 0)
      {
        DivElement div = nameEls.remove(idx);
        div.getParentElement().removeChild(div);
      }
      else if (nameEls.size() > 1)
      {
        DivElement div = nameEls.get(1);
        nameEls.remove(div);
        div.getParentElement().removeChild(div);
      }
      argEls.remove(varDiv);
      varDiv.getParentElement().removeChild(varDiv);
      argTypeFields.remove(typeField);
    }, false);
  }
  
  static class MethodNameWidget
  {
    final Document doc;
    final DivElement baseDiv;
    FunctionSignature sig;
    List<InputElement> nameEls = new ArrayList<>();
    List<DivElement> argEls = new ArrayList<>();
    List<TypeEntryField> argTypeFields = new ArrayList<>();
    Element firstColonEl;
    
    public MethodNameWidget(FunctionSignature sig)
    {
      doc = Browser.getDocument();
      this.sig = FunctionSignature.copyOf(sig);
      
      // Create initial layout
      baseDiv = doc.createDivElement();
      baseDiv.setClassName("flexloosewrappinggroup");
      rebuildTree();
    }
    
    private void rebuildTree() 
    {
      baseDiv.setInnerHTML(UIResources.INSTANCE.getMethodNameBaseHtml().getText());
      firstColonEl = (Element)baseDiv.querySelectorAll(".method_name_colon").item(0);
      
      InputElement firstNamePartEl = (InputElement)baseDiv.querySelectorAll("plom-autoresizing-input").item(0); 
      nameEls.add(firstNamePartEl);
      firstNamePartEl.setValue(sig.nameParts.get(0));
      
      if (!sig.argNames.isEmpty())
      {
        DivElement dummyDiv = doc.createDivElement();
        dummyDiv.setInnerHTML(UIResources.INSTANCE.getMethodNamePartForArgumentHtml().getText());
        while (dummyDiv.getFirstChild() != null)
          baseDiv.appendChild(dummyDiv.getFirstChild());
      }
    }
    
    public Element getBaseElement() { return baseDiv; }
    
  }
}
