package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.ConfigureGlobalScope;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
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
  MethodPanel(DivElement mainDiv, ModuleCodeRepository repository, FunctionSignature sig, boolean isNew)
  {
    this.repository = repository;

    doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getMethodPanelHtml().getText());
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());
    
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
  MethodNameWidget methodWidget;
  MethodNameWidget2 methodWidget2;
  FunctionSignature originalSig;
  TypeEntryField returnTypeField = null;
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
    
    methodWidget = new MethodNameWidget(sig, simpleEntry, 
        (scope, coreTypes) -> {
      StandardLibrary.createGlobals(null, scope, coreTypes);
      scope.setParent(new RepositoryScope(repository, coreTypes));
    }, widthCalculator, maxTypeWidth, containerDiv.querySelector(".methoddetails"), containerDiv.querySelector(".methoddetails .scrollable-interior"));
    containerDiv.querySelector(".method_name").setInnerHTML("");
    containerDiv.querySelector(".method_name").appendChild(methodWidget.getBaseElement());
    methodWidget.setListener((nameSig) -> {
      notifyOfChanges();
    });

    methodWidget2 = new MethodNameWidget2(sig, simpleEntry, 
        (DivElement)containerDiv.querySelector("div.choices"),
        (Element)containerDiv.querySelector("div.sidechoices"),
        (Element)containerDiv.querySelector("svg.cursoroverlay"),
        (scope, coreTypes) -> {
      StandardLibrary.createGlobals(null, scope, coreTypes);
      scope.setParent(new RepositoryScope(repository, coreTypes));
    }, widthCalculator, 
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
    containerDiv.querySelector(".method_args_add a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      methodWidget.addArgumentToEnd();
    }, false);

    // Add argument button
    containerDiv.querySelector(".method_args_add2 a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      methodWidget2.addArgumentToEnd();
    }, false);

    // Render the return type
    if (!sig.isConstructor)
    {
      int maxReturnTypeWidth = containerDiv.querySelector(".method_return").getClientWidth();
      returnTypeField = new TypeEntryField(sig.returnType.copy(), (DivElement)containerDiv.querySelector(".method_return .typeEntry"), simpleEntry, true,
          (scope, coreTypes) -> {
            StandardLibrary.createGlobals(null, scope, coreTypes);
            scope.setParent(new RepositoryScope(repository, coreTypes));
          },
          (context) -> {},
          widthCalculator, maxReturnTypeWidth, containerDiv.querySelector(".methoddetails"), containerDiv.querySelector(".methoddetails .scrollable-interior"));
      returnTypeField.setChangeListener((type, isFinal) -> {
        notifyOfChanges();
      });
      returnTypeField.render();
      
      
      SubCodeArea returnArea = SubCodeArea.forTypeField(
          containerDiv.querySelector(".methodReturnCode"), 
          (DivElement)containerDiv.querySelector("div.choices"),
          (Element)containerDiv.querySelector("svg.cursoroverlay"),
          simpleEntry,
          (Element)containerDiv.querySelector("div.sidechoices"),
          (Element)containerDiv.querySelector(".scrollable-interior"),
          containerDiv.querySelector(".methoddetails"), 
          (Element)containerDiv.querySelector(".nameHeading"),
          widthCalculator);
      returnArea.setVariableContextConfigurator(
          (scope, coreTypes) -> {
            StandardLibrary.createGlobals(null, scope, coreTypes);
            scope.setParent(new RepositoryScope(repository, coreTypes));
          },
          (context) -> {
            return;
          });
//      returnArea.setListener((isCodeChanged) -> {
//        if (isCodeChanged)
//        {
//          // Updates the code in the repository (this is not actually
//          // necessary since the StatementContainer in the variable area
//          // is set to the same object as the one in the repository, but
//          // I'm doing an explicit update in case that changes)
//          repository.setVariableDeclarationCode(returnArea.codeList);
//          
//          // Update error list
//          returnArea.codeErrors.clear();
//          try {
//            ParseToAst.parseStatementContainer(Symbol.VariableDeclarationOrEmpty, returnArea.codeList, returnArea.codeErrors);
//          }
//          catch (Exception e)
//          {
//            // No errors should be thrown
//          }
//          // Update line numbers
//        }
//      });
//      returnArea.setCode(repository.getVariableDeclarationCode());

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
//      // TODO: Remove OK button and just have changes automatically be applied
//      // TODO: Check validity of function name
//      List<String> nameParts = new ArrayList<>();
//      List<String> argNames = new ArrayList<>();
//      List<Token.ParameterToken> argTypes = new ArrayList<>();
      Token.ParameterToken returnType = null;
//      for (DivElement div: nameEls)
//        nameParts.add(((InputElement)div.querySelector("input")).getValue());
//      for (DivElement div: argEls)
//        argNames.add(((InputElement)div.querySelector("plom-autoresizing-input")).getValue());
//      for (TypeEntryField typeField: argTypeFields)
//        argTypes.add(typeField.type);
      if (returnTypeField != null)
        returnType = returnTypeField.type;
      FunctionSignature nameSig = methodWidget.getNameSig();
      FunctionSignature newSig = FunctionSignature.from(returnType, nameSig.nameParts, nameSig.argNames, nameSig.argTypes, sig);
      if (listener != null)
        listener.onSignatureChange(newSig, true);
    }, false);
    
    // When creating a new function/method, the name should be selected so that you can immediately give it a new name 
    if (isNew)
    {
      methodWidget.focusAndSelectFirstName();
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
    FunctionSignature nameSig = methodWidget.getNameSig();
    Token.ParameterToken returnType = null;
    if (returnTypeField != null)
      returnType = returnTypeField.type;
    FunctionSignature newSig = FunctionSignature.from(returnType, nameSig.nameParts, nameSig.argNames, nameSig.argTypes, originalSig);
    if (listener != null)
      listener.onSignatureChange(newSig, false);
  }
  
  static class MethodNameWidget
  {
    final Document doc;
    final SimpleEntry simpleEntry;
    final ConfigureGlobalScope globalScopeForTypeLookup;
    final DivElement baseDiv;
    FunctionSignature sig;
    List<InputElement> nameEls;
    List<InputElement> argEls;
    List<TypeEntryField> argTypeFields;
    Element firstColonEl;
    Consumer<FunctionSignature> changeListener;
    SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
    int maxTypeWidth;
    Element scrollableDiv;
    Element scrollableInterior;
    
    public MethodNameWidget(FunctionSignature sig, SimpleEntry simpleEntry, ConfigureGlobalScope globalScopeForTypeLookup,   SvgCodeRenderer.SvgTextWidthCalculator widthCalculator, int maxTypeWidth, Element scrollableDiv, Element scrollableInterior)
    {
      doc = Browser.getDocument();
      this.simpleEntry = simpleEntry;
      this.globalScopeForTypeLookup = globalScopeForTypeLookup;
      this.sig = FunctionSignature.copyOf(sig);
      this.widthCalculator = widthCalculator;
      this.maxTypeWidth = maxTypeWidth;
      this.scrollableDiv = scrollableDiv;
      this.scrollableInterior = scrollableInterior;
      
      // Create initial layout
      baseDiv = doc.createDivElement();
      baseDiv.setClassName("flexloosewrappinggroup");
      baseDiv.getStyle().setProperty("row-gap", "0.25em");
      rebuild();
    }
    
    public void rebuild() 
    {
      nameEls = new ArrayList<>();
      argEls = new ArrayList<>();
      argTypeFields = new ArrayList<>();
      
      baseDiv.setInnerHTML(UIResources.INSTANCE.getMethodNameBaseHtml().getText());
      firstColonEl = (Element)baseDiv.querySelectorAll(".method_name_colon").item(0);

      InputElement firstNamePartEl = (InputElement)baseDiv.querySelectorAll("plom-autoresizing-input").item(0);
      firstNamePartEl.addEventListener(Event.CHANGE, (evt) -> {
        onCommittedChangeInUi();
      }, false);
      nameEls.add(firstNamePartEl);
      firstNamePartEl.setValue(sig.nameParts.get(0));

      // Don't show a colon after the method name if there is no argument coming after it
      if (sig.argNames.isEmpty())
        baseDiv.querySelector(".method_name_colon").getStyle().setDisplay("none");
      
      // Show the first argument if there is one
      if (!sig.argNames.isEmpty())
      {
        DivElement dummyDiv = doc.createDivElement();
        dummyDiv.setInnerHTML(UIResources.INSTANCE.getMethodNameFirstArgumentHtml().getText());
        InputElement argNameEl = (InputElement)dummyDiv.querySelector("plom-autoresizing-input");
        argNameEl.setValue(sig.argNames.get(0));
        argNameEl.addEventListener(Event.CHANGE, (evt) -> {
          onCommittedChangeInUi();
        }, false);
        argEls.add(argNameEl);
        
        // argument type
        TypeEntryField typeField = new TypeEntryField(sig.argTypes.get(0), (DivElement)dummyDiv.querySelector(".typeEntry"), simpleEntry, false,
            globalScopeForTypeLookup, (context) -> {}, widthCalculator, maxTypeWidth, scrollableDiv, scrollableInterior);
        argTypeFields.add(typeField);
        typeField.setChangeListener((token, isFinal) -> {
          onCommittedChangeInUi();
        });
        typeField.render();

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
      for (int n = 1; n < sig.argNames.size(); n++)
      {
        DivElement dummyDiv = doc.createDivElement();
        dummyDiv.setInnerHTML(UIResources.INSTANCE.getMethodNamePartHtml().getText());
        InputElement namePartEl = (InputElement)dummyDiv.querySelectorAll("plom-autoresizing-input").item(0); 
        namePartEl.setValue(sig.nameParts.get(n));
        namePartEl.addEventListener(Event.CHANGE, (evt) -> {
          onCommittedChangeInUi();
        }, false);
        nameEls.add(namePartEl);
        InputElement argNameEl = (InputElement)dummyDiv.querySelectorAll("plom-autoresizing-input").item(1); 
        argNameEl.setValue(sig.argNames.get(n));
        argNameEl.addEventListener(Event.CHANGE, (evt) -> {
          onCommittedChangeInUi();
        }, false);
        argEls.add(argNameEl);
        
        // argument type
        TypeEntryField typeField = new TypeEntryField(sig.argTypes.get(n), (DivElement)dummyDiv.querySelector(".typeEntry"), simpleEntry, false,
            globalScopeForTypeLookup, (context) -> {}, widthCalculator, maxTypeWidth, scrollableDiv, scrollableInterior);
        argTypeFields.add(typeField);
        typeField.setChangeListener((token, isFinal) -> {
          onCommittedChangeInUi();
        });
        typeField.render();

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
      sig.argNames.add("val");
      sig.argTypes.add(Token.ParameterToken.fromContents("@object", Symbol.AtType));
      if (sig.argNames.size() > 1)
        sig.nameParts.add("with");
      rebuild();
      if (nameEls.size() == 1)
      {
        argEls.get(argEls.size() - 1).focus();
        argEls.get(argEls.size() - 1).select();
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
        sig.argNames.remove(index);
        sig.argTypes.remove(index);
        if (sig.nameParts.size() > 1)
        sig.nameParts.remove(index + 1);
      }
      else
      {
        sig.argNames.remove(index);
        sig.argTypes.remove(index);
        sig.nameParts.remove(index);
      }
      rebuild();
      onCommittedChangeInUi();
    }

    public void syncFromUi()
    {
      for (int n = 0; n < nameEls.size(); n++)
        sig.nameParts.set(n, nameEls.get(n).getValue());
      for (int n = 0; n < argEls.size(); n++)
        sig.argNames.set(n, argEls.get(n).getValue());
      for (int n = 0; n < argTypeFields.size(); n++)
        sig.argTypes.set(n, argTypeFields.get(n).type);
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
  
  static class MethodNameWidget2
  {
    final Document doc;
    final SimpleEntry simpleEntry;
    ModuleCodeRepository repository;
    final ConfigureGlobalScope globalScopeForTypeLookup;
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
    Element sideChoicesDiv;
    DivElement choicesDiv;
    Element scrollableDiv;
    Element scrollableInterior;
    Element cursorOverlay;
    
    public MethodNameWidget2(FunctionSignature sig, SimpleEntry simpleEntry, DivElement choices, Element sideChoices, Element cursorOverlay, ConfigureGlobalScope globalScopeForTypeLookup,   SvgCodeRenderer.SvgTextWidthCalculator widthCalculator, Element divForDeterminingWindowWidth, int maxTypeWidth, Element scrollableDiv, Element scrollableInterior)
    {
      doc = Browser.getDocument();
      this.simpleEntry = simpleEntry;
      this.globalScopeForTypeLookup = globalScopeForTypeLookup;
      this.sig = FunctionSignature.copyOf(sig);
      this.widthCalculator = widthCalculator;
      this.divForDeterminingWindowWidth = divForDeterminingWindowWidth;
      this.maxTypeWidth = maxTypeWidth;
      this.scrollableDiv = scrollableDiv;
      this.scrollableInterior = scrollableInterior;
      this.sideChoicesDiv = sideChoices;
      this.choicesDiv = choices;
      this.cursorOverlay = cursorOverlay;
      
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
      if (sig.argNames.isEmpty())
        baseDiv.querySelector(".method_name_colon").getStyle().setDisplay("none");
      
      // Show the first argument if there is one
      if (!sig.argNames.isEmpty())
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
      for (int n = 1; n < sig.argNames.size(); n++)
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
          choicesDiv,
          cursorOverlay,
          simpleEntry,
          sideChoicesDiv,
          scrollableInterior,
          scrollableDiv, 
          divForDeterminingWindowWidth,
          widthCalculator);
      returnArea.setVariableContextConfigurator(
          globalScopeForTypeLookup,
          (context) -> {
            return;
          });
        returnArea.setListener((isCodeChanged) -> {
          if (isCodeChanged)
          {
            // Updates the code in the repository (this is not actually
            // necessary since the StatementContainer in the variable area
            // is set to the same object as the one in the repository, but
            // I'm doing an explicit update in case that changes)
            sig.argCode.set(argIdx, returnArea.getSingleLineCode());
            
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
        returnArea.setSingleLineCode(sig.argCode.get(argIdx));
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
      sig.argCode.add(new TokenContainer());
      sig.argNames.add("val");
      sig.argTypes.add(Token.ParameterToken.fromContents("@object", Symbol.AtType));
      if (sig.argNames.size() > 1)
        sig.nameParts.add("with");
      rebuild();
      if (nameEls.size() == 1)
      {
        // TODO: Get this code working again
//        argCodeAreas.get(0).showPredictedTokenInput();
//        argEls.get(argEls.size() - 1).focus();
//        argEls.get(argEls.size() - 1).select();
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
        sig.argCode.remove(index);
        sig.argNames.remove(index);
        sig.argTypes.remove(index);
        if (sig.nameParts.size() > 1)
          sig.nameParts.remove(index + 1);
      }
      else
      {
        sig.argCode.remove(index);
        sig.argNames.remove(index);
        sig.argTypes.remove(index);
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
        sig.argCode.set(n, argCodeAreas.get(n).getSingleLineCode());
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
