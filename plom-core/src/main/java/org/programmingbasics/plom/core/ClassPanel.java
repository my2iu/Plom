package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.codestore.RepositoryScope;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;
import elemental.svg.SVGDocument;
import jsinterop.annotations.JsFunction;

/**
 * UI panel for modifying classes
 */
public class ClassPanel
{
  Document doc = Browser.getDocument();
  SimpleEntry simpleEntry;
  DivElement mainDiv;
  CodeRepositoryClient repository; 
  ClassDescription cls;
  LoadMethodCodeViewCallback viewSwitchCallback;
  LoadMethodSigViewCallback methodSigViewCallback;
  ExitClassViewCallback exitCallback;
  SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
  
  ClassPanel(DivElement mainDiv, CodeRepositoryClient repository, ClassDescription cls, LoadMethodCodeViewCallback callback, LoadMethodSigViewCallback methodSigViewCallback, ExitClassViewCallback exitCallback, boolean isNew)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
    this.cls = cls;
    this.viewSwitchCallback = callback;
    this.methodSigViewCallback = methodSigViewCallback;
    this.exitCallback = exitCallback;
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());

    rebuildView(isNew);
  }
  
  public void rebuildView(boolean isNew)
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getClassPanelHtml().getText());

    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"),
        (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent"));
    simpleEntry.setVisible(false);

    // For setting class name
    InputElement nameAnchor = (InputElement)mainDiv.querySelector(".className plom-autoresizing-input");
    nameAnchor.setValue(cls.getName());
    nameAnchor.addEventListener(Event.CHANGE, (e) -> {
      e.preventDefault();
      cls.setName(nameAnchor.getValue());
    }, false);

    // For deleting the whole class
    AnchorElement deleteClassAnchor = (AnchorElement)mainDiv.querySelector(".className a.delete_class");
    deleteClassAnchor.addEventListener(Event.CLICK, (evt) -> {
      repository.deleteClassAndResetIds(cls.module, cls.id);
      exitCallback.exit();
      evt.preventDefault();
    }, false);
    if (cls.isBuiltIn || cls.isImported)
    {
      deleteClassAnchor.getStyle().setDisplay(Display.NONE);
    }
    
    // For setting the supertype
    int maxTypeWidth = mainDiv.querySelector(".extends").getClientWidth();
    TypeEntryField extendsField = new TypeEntryField(cls.parent.mainToken, (DivElement)mainDiv.querySelector(".extends .typeEntry"), simpleEntry, false,
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository.localRepo, coreTypes, null));
        },
        (context) -> {},
        widthCalculator, maxTypeWidth, mainDiv.querySelector(".classdetails"), mainDiv.querySelector(".classdetails .scrollable-interior"));
    extendsField.setChangeListener((newType, isFinal) -> {
      cls.setSuperclass(UnboundType.fromToken(newType));
    });
    extendsField.render();

    
    // For adding methods
    Element newFunctionAnchor = mainDiv.querySelector(".methodsHeading a");
    newFunctionAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      repository.makeNewUniqueMethod(cls, false, false)
        .<Void>then((func) -> {
          return repository.reloadClass(cls).<Void>thenNow((cl) -> {
            methodSigViewCallback.load(cl, func, true);
            return null;
          });
        });
    }, false);
    
    // List of instance methods
    createMethodList(mainDiv.querySelector(".methodsList"), cls.getInstanceMethods());

    // For adding static methods
    mainDiv.querySelector(".staticMethodsHeading a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      repository.makeNewUniqueMethod(cls, true, false)
        .<Void>then((func) -> {
          return repository.reloadClass(cls).<Void>thenNow((cl) -> {
            methodSigViewCallback.load(cl, func, true);
            return null;
          });
        });
    }, false);
    
    // For adding constructor methods
    mainDiv.querySelector(".constructorMethodsHeading a").addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      repository.makeNewUniqueMethod(cls, false, true)
        .<Void>then((func) -> {
          return repository.reloadClass(cls).<Void>thenNow((cl) -> {
            methodSigViewCallback.load(cl, func, true);
            return null;
          });
        });

    }, false);
    
    // List of static methods
    createMethodList(mainDiv.querySelector(".staticMethodsList"), cls.getStaticMethods());

    // List of constructor methods
    createMethodList(mainDiv.querySelector(".constructorMethodsList"), cls.getConstructorMethods());
    
    // Variables
//    Element newVarAnchor = mainDiv.querySelector(".varsHeading a");
//    newVarAnchor.addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//      String newVarName = "";
//      int newId = cls.addVarAndResetIds(newVarName, Token.ParameterToken.fromContents("@object", Symbol.AtType));
//      rebuildView(false);
//      
//      NodeList nodes = mainDiv.querySelectorAll("div.class_var");
//      // Assume ids are linear
//      if (newId < nodes.length())
//      {
//        Element el = (Element)nodes.item(newId);
//        el.scrollIntoView();
//        ((InputElement)el.querySelector("plom-autoresizing-input")).focus();
//        ((InputElement)el.querySelector("plom-autoresizing-input")).select();
//      }
//    }, false);
   
    // Code panel for variables
    SubCodeArea variableArea = SubCodeArea.forVariableDeclaration(
        mainDiv.querySelector(".varsCode"), 
        new CodeWidgetInputPanels(
            (DivElement)mainDiv.querySelector("div.choices"),
            new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
                (DivElement)mainDiv.querySelector("div.sidechoices"),
                (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent")),
            new CodeWidgetCursorOverlay((Element)mainDiv.querySelector("svg.cursoroverlay")),
            true),
        (Element)mainDiv.querySelector(".scrollable-interior"),
        mainDiv.querySelector(".classdetails"), 
        (Element)mainDiv.querySelector(".methodsHeading"),
        widthCalculator);
    variableArea.setVariableContextConfigurator(
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository.localRepo, coreTypes, null));
        },
        null);
    variableArea.setListener((isCodeChanged) -> {
      if (isCodeChanged)
      {
        // Updates the code in the repository (this is not actually
        // necessary since the StatementContainer in the variable area
        // is set to the same object as the one in the repository, but
        // I'm doing an explicit update in case that changes)
        cls.setVariableDeclarationCode(variableArea.codeList);
        
        // Update error list
        variableArea.codeErrors.clear();
        try {
          ParseToAst.parseStatementContainer(Symbol.VariableDeclarationOrEmpty, variableArea.codeList, variableArea.codeErrors);
        }
        catch (Exception e)
        {
          // No errors should be thrown
        }
        // Update line numbers
//        lineNumbers.calculateLineNumbersForStatements(codePanel.codeList, 1);
      }
//      if (codePanel.cursorPos != null)
//      {
//        int lineNo = LineForPosition.inCode(codePanel.codeList, codePanel.cursorPos, lineNumbers);
//        Element lineEl = Browser.getDocument().querySelector(".lineIndicator");
//        if (lineEl != null)
//        {
//          lineEl.getStyle().clearDisplay();
//          lineEl.setTextContent("L" + lineNo);
//        }
//      }
    });
    variableArea.setCode(cls.getVariableDeclarationCode());
    
    // When creating a new class, the name should initially be highlight, so that
    // you can immediately set a name for the class
    if (isNew)
    {
      nameAnchor.focus();  // Documentation is unclear as to whether select() also sets focus or not
      nameAnchor.select();
    }
  }

  private void createMethodList(Element methodListEl, List<FunctionDescription> fnList)
  {
    for (FunctionDescription fn: fnList)
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setClassName("plomUiButton");
      a.setHref("#");
      a.setTextContent("." + fn.sig.getLookupName());
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        viewSwitchCallback.load(cls, fn);
      }, false);
      AnchorElement deleteAnchor = (AnchorElement)doc.createElement("a");
      deleteAnchor.setHref("#");
      deleteAnchor.setClassName("plomUiRemoveButton");
      deleteAnchor.addEventListener(Event.CLICK, (evt) -> {
        evt.preventDefault();
        cls.deleteMethodAndResetIds(fn.id);
        rebuildView(false);
      }, false);
      DivElement div = doc.createDivElement();
      div.appendChild(a);
      div.appendChild(doc.createTextNode(" "));
      div.appendChild(deleteAnchor);
      methodListEl.appendChild(div);
    }
  }
  
//  private String classVarInnerHtml()
//  {
//    return GlobalsPanel.varInnerHtml("class_var", "delete_class_var");
//  }
  
//  private void addVarEntry(DivElement mainDiv,
//      VariableDescription v,
//      List<DivElement> varDivs)
//  {
//    String name = v.name;
//    Token.ParameterToken type = v.type; 
//    DivElement div = doc.createDivElement();
//    div.setInnerHTML(classVarInnerHtml());
//    ((InputElement)div.querySelector("plom-autoresizing-input")).setValue(name);
//    varDivs.add(div);
//    mainDiv.querySelector(".varsList").appendChild(div);
//    int maxTypeWidth = div.querySelector(".class_var").getClientWidth();
//    TypeEntryField typeField = new TypeEntryField(type, (DivElement)div.querySelector(".typeEntry"), simpleEntry, false,
//        (scope, coreTypes) -> {
//          StandardLibrary.createGlobals(null, scope, coreTypes);
//          scope.setParent(new RepositoryScope(repository, coreTypes, null));
//        },
//        (context) -> {},
//        widthCalculator, maxTypeWidth, mainDiv.querySelector(".classdetails"), mainDiv.querySelector(".classdetails .scrollable-interior"));
//    typeField.setChangeListener((newType, isFinal) -> {
//      v.type = newType;
//      cls.updateVariable(v);
//    });
//    typeField.render();
//    
//    InputElement nameInput = (InputElement)div.querySelector("plom-autoresizing-input"); 
//    nameInput.addEventListener(Event.CHANGE, (evt) -> {
//      v.name = nameInput.getValue(); 
//      cls.updateVariable(v);
//    }, false);
//    
//    AnchorElement deleteAnchor = (AnchorElement)div.querySelector("a.delete_class_var");
//    deleteAnchor.addEventListener(Event.CLICK, (evt) -> {
//      evt.preventDefault();
//      cls.deleteVarAndResetIds(v.id);
//      rebuildView(false);
//    }, false);
//  }

  @JsFunction
  public static interface ExitClassViewCallback
  {
    void exit();
  }
  
  @JsFunction
  public static interface LoadMethodSigViewCallback
  {
    void load(ClassDescription cls, FunctionDescription method, boolean isNew);
  }
  
  @JsFunction
  public static interface LoadMethodCodeViewCallback
  {
    void load(ClassDescription cls, FunctionDescription method);
  }
}
