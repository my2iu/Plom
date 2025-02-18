package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;
import elemental.svg.SVGDocument;

public class MethodPanel
{
  MethodPanel(DivElement mainDiv, CodeRepositoryClient repository, FunctionSignature sig, boolean isNew)
  {
    this.repository = repository;

    doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getMethodPanelHtml().getText());
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());
    
    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"),
        (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent"));
    simpleEntry.setVisible(false);
    
    showMethod(mainDiv, sig, isNew);
  }

  final Document doc;
  SignatureListener listener;
  CodeRepositoryClient repository;
  SimpleEntry simpleEntry;
//  MethodNameWidget methodWidget;
  MethodNameWidget2 methodWidget2;
  FunctionSignature originalSig;
  TypeEntryField returnTypeField = null;
  SubCodeArea returnTypeArea;
  SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
  
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
//    List<DivElement> nameEls = new ArrayList<>();
//    List<DivElement> argEls = new ArrayList<>();
//    List<TypeEntryField> argTypeFields = new ArrayList<>();
    originalSig = sig;
    
    // (Using the method_name div to get the maximum width for rendering types is a little
    // bit too wide, should find something that removes the padding of the argument box too)
    int maxTypeWidth = containerDiv.querySelector(".method_name").getClientWidth();
    
    CodeWidgetInputPanels inputPanels = new CodeWidgetInputPanels(
        (DivElement)containerDiv.querySelector("div.choices"),
        simpleEntry,
        new CodeWidgetCursorOverlay((Element)containerDiv.querySelector("svg.cursoroverlay")),
        true);
    methodWidget2 = new MethodNameWidget2(sig, inputPanels, 
        widthCalculator, 
        (Element)containerDiv.querySelector(".nameHeading"),
        maxTypeWidth, containerDiv.querySelector(".methoddetails"), containerDiv.querySelector(".methoddetails .scrollable-interior"));
    containerDiv.querySelector(".method_name2").setInnerHTML("");
    containerDiv.querySelector(".method_name2").appendChild(methodWidget2.getBaseElement());
    methodWidget2.setListener((nameSig) -> {
      notifyOfChanges();
    });

//    // Fill in the function name
//    nameEls.add((DivElement)containerDiv.querySelector("div.method_name"));
//    ((InputElement)nameEls.get(0).querySelector("input")).setValue(sig.nameParts.get(0));
//
//    for (int n = 0; n < sig.argNames.size(); n++)
//      addMethodPanelArg(containerDiv, sig.nameParts.get(n), sig.argNames.get(n), sig.argTypes.get(n), nameEls, argEls, argTypeFields);
//    
    // Add argument button
//    containerDiv.querySelector(".method_args_add a").addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//      methodWidget.addArgumentToEnd();
//    }, false);

    // Add argument button
    containerDiv.querySelector(".method_args_add2 a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      methodWidget2.addArgumentToEnd();
    }, false);

    // Render the return type
    if (!sig.isConstructor)
    {
//      int maxReturnTypeWidth = containerDiv.querySelector(".method_return").getClientWidth();
//      returnTypeField = new TypeEntryField(sig.getReturnType().copy(), (DivElement)containerDiv.querySelector(".method_return .typeEntry"), simpleEntry, true,
//          (scope, coreTypes) -> {
//            StandardLibrary.createGlobals(null, scope, coreTypes);
//            scope.setParent(new RepositoryScope(repository, coreTypes));
//          },
//          (context) -> {},
//          widthCalculator, maxReturnTypeWidth, containerDiv.querySelector(".methoddetails"), containerDiv.querySelector(".methoddetails .scrollable-interior"));
//      returnTypeField.setChangeListener((type, isFinal) -> {
//        notifyOfChanges();
//      });
//      returnTypeField.render();
      
      
      returnTypeArea = SubCodeArea.forTypeField(
          containerDiv.querySelector(".methodReturnCode"),
          inputPanels,
          (Element)containerDiv.querySelector(".scrollable-interior"),
          containerDiv.querySelector(".methoddetails"), 
          (Element)containerDiv.querySelector(".nameHeading"),
          widthCalculator);
      returnTypeArea.setVariableContextConfigurator(
          repository.makeCodeCompletionSuggesterNoContext());
      returnTypeArea.setListener((isCodeChanged) -> {
        if (isCodeChanged)
        {
//          // Updates the code in the repository (this is not actually
//          // necessary since the StatementContainer in the variable area
//          // is set to the same object as the one in the repository, but
//          // I'm doing an explicit update in case that changes)
//          repository.setVariableDeclarationCode(returnArea.codeList);
//          
          // Update error list
          returnTypeArea.codeErrors.clear();
          try {
            ParseToAst.parseStatementContainer(Symbol.ReturnTypeField, returnTypeArea.codeList, returnTypeArea.codeErrors);
          }
          catch (Exception e)
          {
            // No errors should be thrown
          }
          // Update line numbers
        }
      });
      returnTypeArea.setSingleLineCode(sig.getReturnTypeCode().copy());
    }
    else
    {
      // Constructors have no return type
      NodeList returnDivs = containerDiv.querySelectorAll(".method_return");
      for (int n = 0; n < returnDivs.length(); n++)
      {
        ((Element)returnDivs.item(n)).getStyle().setDisplay(Display.NONE);      
      }
      
    }

    // Ok Button
    AnchorElement okButton = (AnchorElement)containerDiv.querySelector("a.done");
    okButton.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      // TODO: Remove OK button and just have changes automatically be applied
      FunctionSignature newSig = getCurrentSignature();
      if (listener != null)
        listener.onSignatureChange(newSig, true);
    }, false);
    
    // When creating a new function/method, the name should be selected so that you can immediately give it a new name 
    if (isNew)
    {
      methodWidget2.focusAndSelectFirstName();
    }
  }

//  private void addMethodPanelArg(DivElement mainDiv,
//      String nameVal, String argNameVal, Token.ParameterToken argTypeVal, 
//      List<DivElement> nameEls, List<DivElement> argEls, 
//      List<TypeEntryField> argTypeFields)
//  {
//    if (!argEls.isEmpty())
//    {
//      DivElement nameDiv = doc.createDivElement();
//      nameDiv.setInnerHTML("<div style=\"padding-left: 1em;\" class=\"method_args_name\"><input size=\"15\" type=\"text\" autocapitalize=\"off\">:</div>");
//      ((InputElement)nameDiv.querySelector("input")).setValue(nameVal);
//      nameEls.add(nameDiv);
//      mainDiv.querySelector(".method_args").appendChild(nameDiv);
//    }
//    DivElement varDiv = doc.createDivElement();
//    varDiv.setInnerHTML("<div style=\"padding-left: 1em;\" class=\"method_args_var\"><div style=\"min-width: 1em; margin-right: 0.2em; display: inline-block;\"><a href=\"#\" aria-label=\"delete argument\" class=\"plomUiRemoveButton\"></a></div>.<plom-autoresizing-input></plom-autoresizing-input><div class=\"typeEntry\">&nbsp;</div></div>");
//    ((InputElement)varDiv.querySelector("plom-autoresizing-input")).setValue(argNameVal);
//    argEls.add(varDiv);
//    mainDiv.querySelector(".method_args").appendChild(varDiv);
//
//    // argument type
//    TypeEntryField typeField = new TypeEntryField(argTypeVal, (DivElement)varDiv.querySelector(".typeEntry"), simpleEntry, false,
//        (scope, coreTypes) -> {
//          StandardLibrary.createGlobals(null, scope, coreTypes);
//          scope.setParent(new RepositoryScope(repository, coreTypes));
//        },
//        (context) -> {});
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
//  }

  private void notifyOfChanges()
  {
    FunctionSignature newSig = getCurrentSignature();
    if (listener != null)
      listener.onSignatureChange(newSig, false);
  }
  
  private FunctionSignature getCurrentSignature()
  {
    FunctionSignature nameSig = methodWidget2.getNameSig();
    List<TokenContainer> argCodes = new ArrayList<>();
    for (int n = 0; n < nameSig.getNumArgs(); n++)
      argCodes.add(nameSig.getArgCode(n));
//    Token.ParameterToken returnType = null;
//    if (returnTypeField != null)
//      returnType = returnTypeField.type;
//    if (returnTypeField != null)
//      returnType = returnTypeField.type;

  // TODO: Check validity of function name
//  List<String> nameParts = new ArrayList<>();
//  List<String> argNames = new ArrayList<>();
//  List<Token.ParameterToken> argTypes = new ArrayList<>();
//  FunctionSignature newSig = FunctionSignature.from(returnType, nameSig.nameParts, nameSig.argNames, nameSig.argTypes, originalSig);
    FunctionSignature newSig;
    if (returnTypeArea != null)
      newSig = FunctionSignature.from(returnTypeArea.getSingleLineCode(), nameSig.nameParts, argCodes, originalSig);
    else
      newSig = FunctionSignature.from(new TokenContainer(), nameSig.nameParts, argCodes, originalSig);
    return newSig;
  }
  
  static class MethodNameWidget2
  {
    final Document doc;
    CodeRepositoryClient repository;
    final DivElement baseDiv;
    FunctionSignature sig;
    List<InputElement> nameEls;
    List<SubCodeArea> argCodeAreas;
//    List<InputElement> argEls;
//    List<TypeEntryField> argTypeFields;
    Element firstColonEl;
    Consumer<FunctionSignature> changeListener;
    SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
    Element divForDeterminingWindowWidth;
    int maxTypeWidth;
    Element scrollableDiv;
    Element scrollableInterior;
    CodeWidgetInputPanels inputPanels;
    
    public MethodNameWidget2(FunctionSignature sig, CodeWidgetInputPanels inputPanels, SvgCodeRenderer.SvgTextWidthCalculator widthCalculator, Element divForDeterminingWindowWidth, int maxTypeWidth, Element scrollableDiv, Element scrollableInterior)
    {
      doc = Browser.getDocument();
      this.sig = FunctionSignature.copyOf(sig);
      this.widthCalculator = widthCalculator;
      this.divForDeterminingWindowWidth = divForDeterminingWindowWidth;
      this.maxTypeWidth = maxTypeWidth;
      this.scrollableDiv = scrollableDiv;
      this.scrollableInterior = scrollableInterior;
      this.inputPanels = inputPanels;
      
      // Create initial layout
      baseDiv = doc.createDivElement();
//      baseDiv.setClassName("flexloosewrappinggroup");
//      baseDiv.getStyle().setProperty("row-gap", "0.25em");
      rebuild();
    }
    
    public void rebuild() 
    {
      nameEls = new ArrayList<>();
      argCodeAreas = new ArrayList<>();
      
      baseDiv.setInnerHTML(UIResources.INSTANCE.getMethodNameBaseHtml().getText());
      firstColonEl = (Element)baseDiv.querySelectorAll(".method_name_colon").item(0);

      InputElement firstNamePartEl = (InputElement)baseDiv.querySelectorAll("plom-autoresizing-input").item(0);
      firstNamePartEl.addEventListener(Event.CHANGE, (evt) -> {
        onCommittedChangeInUi();
      }, false);
      nameEls.add(firstNamePartEl);
      firstNamePartEl.setValue(sig.nameParts.get(0));

      // Don't show a colon after the method name if there is no argument coming after it
      if (sig.getNumArgs() == 0)
        baseDiv.querySelector(".method_name_colon").getStyle().setDisplay("none");
      
      // Show the first argument if there is one
      if (sig.getNumArgs() != 0)
      {
        DivElement dummyDiv = doc.createDivElement();
        dummyDiv.setInnerHTML(UIResources.INSTANCE.getMethodNameFirstArgument2Html().getText());

        SubCodeArea codeArea = createCodeArea(dummyDiv.querySelector(".methodParameterCode"), 0);
        argCodeAreas.add(codeArea);

//        InputElement argNameEl = (InputElement)dummyDiv.querySelector("plom-autoresizing-input");
//        argNameEl.setValue(sig.argNames.get(0));
//        argNameEl.addEventListener(Event.CHANGE, (evt) -> {
//          onCommittedChangeInUi();
//        }, false);
//        argEls.add(argNameEl);
//        
//        // argument type
//        TypeEntryField typeField = new TypeEntryField(sig.argTypes.get(0), (DivElement)dummyDiv.querySelector(".typeEntry"), simpleEntry, false,
//            globalScopeForTypeLookup, (context) -> {}, widthCalculator, maxTypeWidth, scrollableDiv, scrollableInterior);
//        argTypeFields.add(typeField);
//        typeField.setChangeListener((token, isFinal) -> {
//          onCommittedChangeInUi();
//        });
//        typeField.render();

        // Remove button
        dummyDiv.querySelector(".plomUiRemoveButton").addEventListener(Event.CLICK, (evt) -> {
          evt.preventDefault();
          deleteArg(0);
        });
        
        // Copy markup for the arguments into the UI
        while (dummyDiv.getFirstChild() != null)
          baseDiv.appendChild(dummyDiv.getFirstChild());
      }
      
      // Handle the rest of the arguments
      for (int n = 1; n < sig.getNumArgs(); n++)
      {
        DivElement dummyDiv = doc.createDivElement();
        dummyDiv.setInnerHTML(UIResources.INSTANCE.getMethodNamePart2Html().getText());
        InputElement namePartEl = (InputElement)dummyDiv.querySelectorAll("plom-autoresizing-input").item(0); 
        namePartEl.setValue(sig.nameParts.get(n));
        namePartEl.addEventListener(Event.CHANGE, (evt) -> {
          onCommittedChangeInUi();
        }, false);
        nameEls.add(namePartEl);

        SubCodeArea codeArea = createCodeArea(dummyDiv.querySelector(".methodParameterCode"), n);
        argCodeAreas.add(codeArea);
        
//        InputElement argNameEl = (InputElement)dummyDiv.querySelectorAll("plom-autoresizing-input").item(1); 
//        argNameEl.setValue(sig.argNames.get(n));
//        argNameEl.addEventListener(Event.CHANGE, (evt) -> {
//          onCommittedChangeInUi();
//        }, false);
//        argEls.add(argNameEl);
//        
//        // argument type
//        TypeEntryField typeField = new TypeEntryField(sig.argTypes.get(n), (DivElement)dummyDiv.querySelector(".typeEntry"), simpleEntry, false,
//            globalScopeForTypeLookup, (context) -> {}, widthCalculator, maxTypeWidth, scrollableDiv, scrollableInterior);
//        argTypeFields.add(typeField);
//        typeField.setChangeListener((token, isFinal) -> {
//          onCommittedChangeInUi();
//        });
//        typeField.render();

        // Remove button
        final int argIdx = n;
        dummyDiv.querySelector(".plomUiRemoveButton").addEventListener(Event.CLICK, (evt) -> {
          evt.preventDefault();
          deleteArg(argIdx);
        });

        // Copy markup for the arguments into the UI
        while (dummyDiv.getFirstChild() != null)
          baseDiv.appendChild(dummyDiv.getFirstChild());
      }
    }

    private SubCodeArea createCodeArea(Element div, int argIdx)
    {
      SubCodeArea returnArea = SubCodeArea.forMethodParameterField(
          div,
          inputPanels,
          scrollableInterior,
          scrollableDiv, 
          divForDeterminingWindowWidth,
          widthCalculator);
      returnArea.setVariableContextConfigurator(
          repository.makeCodeCompletionSuggesterNoContext());
        returnArea.setListener((isCodeChanged) -> {
          if (isCodeChanged)
          {
            // Updates the code in the repository (this is not actually
            // necessary since the StatementContainer in the variable area
            // is set to the same object as the one in the repository, but
            // I'm doing an explicit update in case that changes)
            sig.setArgCode(argIdx, returnArea.getSingleLineCode(), null);
            onCommittedChangeInUi();
            
            // Update error list
            returnArea.codeErrors.clear();
            try {
              ParseToAst.parseStatementContainer(Symbol.ParameterField, returnArea.codeList, returnArea.codeErrors);
            }
            catch (Exception e)
            {
              // No errors should be thrown
            }
            // Update line numbers
          }
        });
        returnArea.setSingleLineCode(sig.getArgCode(argIdx));
      return returnArea;
    }
    
    /** User changed values in one of the input fields (this is a final, committed change) */
    private void onCommittedChangeInUi()
    {
      syncFromUi();
      if (changeListener != null)
        changeListener.accept(sig);
    }
    
    public Element getBaseElement() { return baseDiv; }
    
    public void addArgumentToEnd()
    {
      syncFromUi();
      sig.addNewArgCode();
      if (sig.getNumArgs() > 1)
        sig.nameParts.add("with");
      rebuild();
      if (nameEls.size() == 1)
      {
        argCodeAreas.get(0).showPredictedTokenInput();
      }
      else
      {
        nameEls.get(nameEls.size() - 1).focus();
        nameEls.get(nameEls.size() - 1).select();
      }
      onCommittedChangeInUi();
    }

    public void deleteArg(int index)
    {
      syncFromUi();
      if (index == 0)
      {
        sig.removeArgCode(index);
        if (sig.nameParts.size() > 1)
          sig.nameParts.remove(index + 1);
      }
      else
      {
        sig.removeArgCode(index);
        sig.nameParts.remove(index);
      }
      rebuild();
      onCommittedChangeInUi();
    }

    public void syncFromUi()
    {
      for (int n = 0; n < nameEls.size(); n++)
        sig.nameParts.set(n, nameEls.get(n).getValue());
      for (int n = 0; n < argCodeAreas.size(); n++)
      {
        sig.setArgCode(n, argCodeAreas.get(n).getSingleLineCode(), null);
//        sig.argNames.set(n, argEls.get(n).getValue());
//        sig.argTypes.set(n, argTypeFields.get(n).type);
      }
    }
    
    public void focusAndSelectFirstName()
    {
      nameEls.get(0).focus();  // Documentation is unclear as to whether select() also sets focus or not
      nameEls.get(0).select();
    }
    
    public FunctionSignature getNameSig()
    {
      syncFromUi();
      return sig;
    }

    public void setListener(Consumer<FunctionSignature> listener)
    {
      this.changeListener = listener;
    }
  }

}
