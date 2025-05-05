package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FileDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.ArrayBuffer;
import elemental.html.DivElement;
import elemental.svg.SVGDocument;
import elemental.svg.SVGSVGElement;
import elemental.util.ArrayOf;
import jsinterop.annotations.JsFunction;

/**
 * UI code for listing global variables
 */
public class GlobalsPanel implements AutoCloseable
{
  Document doc = Browser.getDocument();
  SimpleEntry simpleEntry;
  CodeRepositoryClient repository;
  DivElement mainDiv;
  LoadFunctionCodeViewCallback viewSwitchCallback;
  LoadFunctionSigViewCallback functionSigCallback;
  LoadClassViewCallback classViewCallback;
  LoadTextEditorViewCallback textEditorViewCallback;
  SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
  
  GlobalsPanel(DivElement mainDiv, CodeRepositoryClient repository, LoadFunctionCodeViewCallback callback, LoadFunctionSigViewCallback functionSigCallback, LoadClassViewCallback classViewCallback, LoadTextEditorViewCallback textEditorViewCallback)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
    this.viewSwitchCallback = callback;
    this.functionSigCallback = functionSigCallback;
    this.classViewCallback = classViewCallback;
    this.textEditorViewCallback = textEditorViewCallback;
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());
    rebuildView();
  }

  void rebuildView()
  {
    // Gather up the necessary information from the language server
    class GatheredInfo
    {
      List<ClassDescription> allClasses;
      List<FunctionDescription> allFunctions;
      StatementContainer varDeclCode;
      StatementContainer importedVarDeclCode;
    }
    GatheredInfo gatheredData = new GatheredInfo();

    ArrayOf<Promise<Void>> promises = elemental.util.Collections.arrayOf();
    promises.push(repository.getAllClassesSorted().<Void>thenNow((classes) -> {
      gatheredData.allClasses = classes;
      return null;
    }));
    promises.push(repository.getAllFunctionSorted().<Void>thenNow((fns) -> {
      gatheredData.allFunctions = fns;
      return null;
    }));
    promises.push(repository.getVariableDeclarationCode().<Void>thenNow((code) -> {
      gatheredData.varDeclCode = code;
      return null;
    }));
    promises.push(repository.getImportedVariableDeclarationCode().<Void>thenNow((code) -> {
      gatheredData.importedVarDeclCode = code;
      return null;
    }));
    WebHelpersShunt.promiseAll(promises).thenNow((unused) -> {
      rebuildView(gatheredData.allClasses, gatheredData.allFunctions,
          gatheredData.varDeclCode, gatheredData.importedVarDeclCode);
      return null;
    });
  }
  void rebuildView(List<ClassDescription> allClasses, List<FunctionDescription> allFunctions, StatementContainer varDeclCode, StatementContainer importedVarDeclCode)
  {
    Document doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
    
    // UI elements for the type suggestion and type entry stuff
    simpleEntry = new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
        (DivElement)mainDiv.querySelector("div.sidechoices"),
        (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent"));
    simpleEntry.setVisible(false);

    // For adding classes
    Element newClassAnchor = mainDiv.querySelector(".classesHeading a");
    newClassAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      repository.makeNewUniqueClass()
        .<Void>thenNow((c) -> {
          // Switch to view the class
          classViewCallback.load(c, true);
          return null;
        });
    }, false);

    // List of classes
    Element classListEl = mainDiv.querySelector(".classesList");
    for (ClassDescription cls: allClasses)
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setClassName("plomUiButton");
      a.setTextContent("@" + cls.getName());
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
      repository.makeUniqueEmptyFunction().thenNow((func) -> {
        if (functionSigCallback != null)
          functionSigCallback.load(func, true);
        return null;
      });
    }, false);
    
    // List of functions
    Element functionListEl = mainDiv.querySelector(".functionList");
    for (FunctionDescription fnName: allFunctions)
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setClassName("plomUiButton");
      a.setHref("#");
      a.setTextContent("." + fnName.sig.getLookupName());
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        viewSwitchCallback.load(fnName);
      }, false);
      DivElement div = doc.createDivElement();
      if (fnName.isImported)
        div.getClassList().add("moduleImported");
      AnchorElement deleteAnchor = (AnchorElement)doc.createElement("a");
      deleteAnchor.setClassName("plomUiRemoveButton");
//      deleteAnchor.getStyle().setPaddingLeft(0.75, Unit.EM);
      deleteAnchor.setHref("#");
//      deleteAnchor.setTextContent("X");
      if (fnName.isImported)
        deleteAnchor.getStyle().setDisplay(Display.NONE);
      deleteAnchor.addEventListener(Event.CLICK, (evt) -> {
        evt.preventDefault();
        repository.deleteFunction(fnName.sig)
          .thenNow((unused) -> {
            rebuildView();
            return null;
          });
      }, false);
      div.appendChild(a);
      div.appendChild(doc.createTextNode(" "));
      div.appendChild(deleteAnchor);
      functionListEl.appendChild(div);
    }
    
    // For adding global variables
//    Element newGlobalAnchor = mainDiv.querySelector(".globalVarsHeading a");
//    newGlobalAnchor.addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//      String newVarName = "";
//      int newId = repository.addGlobalVarAndResetIds(newVarName, Token.ParameterToken.fromContents("@object", Symbol.AtType));
//      rebuildView();
//      NodeList nodes = mainDiv.querySelectorAll("div.global_var");
//      // Assume ids are linear
//      if (newId < nodes.length())
//      {
//        Element el = (Element)nodes.item(newId);
//        el.scrollIntoView();
//        ((InputElement)el.querySelector("plom-autoresizing-input")).focus();
//        ((InputElement)el.querySelector("plom-autoresizing-input")).select();
//      }
//    }, false);
   
//    List<DivElement> globalVarDivs = new ArrayList<>();
//    for (VariableDescription v: repository.getAllGlobalVarsSorted())
//    {
//      addGlobalVarEntry(mainDiv, v, globalVarDivs);
//    }
    
    // Code panel for variables
    SubCodeArea variableArea = SubCodeArea.forVariableDeclaration(
        mainDiv.querySelector(".globalVarsCode"), 
        new CodeWidgetInputPanels(
            (DivElement)mainDiv.querySelector("div.choices"),
            new SimpleEntry((DivElement)mainDiv.querySelector("div.simpleentry"),
                (DivElement)mainDiv.querySelector("div.sidechoices"),
                (DivElement)mainDiv.querySelector("div.sidechoices div.sidechoicescontent")),
            new NumberEntry((DivElement)mainDiv.querySelector("div.numberentry")),
            new CodeWidgetCursorOverlay((Element)mainDiv.querySelector("svg.cursoroverlay")),
            true),
        (Element)mainDiv.querySelector(".scrollable-interior"),
        mainDiv.querySelector(".globaldetails"), 
        (Element)mainDiv.querySelector(".classesHeading"),
        widthCalculator);
    variableArea.setVariableContextConfigurator(
        repository.makeCodeCompletionSuggesterNoContext());
    variableArea.setListener((isCodeChanged) -> {
      if (isCodeChanged)
      {
        // Updates the code in the repository (this is not actually
        // necessary since the StatementContainer in the variable area
        // is set to the same object as the one in the repository, but
        // I'm doing an explicit update in case that changes)
        repository.setVariableDeclarationCode(variableArea.codeList);
        
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
    variableArea.setCode(varDeclCode);
    
    // Variable declarations imported into the module
    double clientWidth = mainDiv.querySelector(".classesHeading").getClientWidth();
    SvgCodeRenderer.renderSvgWithHitBoxes(
        (SVGSVGElement)mainDiv.querySelector("svg.globalImportedVarsCode"), 
        importedVarDeclCode, 
        null, null, null, new ErrorList(), widthCalculator, clientWidth, 0, 0, 0, 0);
    
    // For adding Files
    Element newFilesAnchor = mainDiv.querySelector(".extraFilesHeading a");
    newFilesAnchor.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      repository.getExtraFilesManager().newFileUi("web", () -> {
        repository.refreshExtraFiles(() -> {
          rebuildFileList();
        });
      });
//      // Find a unique function name
//      String newFunctionName = ModuleCodeRepository.findUniqueName("function", (name) -> repository.getFunctionWithName(name) == null);
//      FunctionDescription func = new FunctionDescription(
//          FunctionSignature.from(UnboundType.forClassLookupName("void"), newFunctionName),
//          new StatementContainer());
//      repository.addFunctionAndResetIds(func);
//      
//      if (functionSigCallback != null)
//        functionSigCallback.load(func, true);
    }, false);
    
    rebuildFileList();
  }

  public void rebuildFileList()
  {
    // List of functions
    Element filesListEl = mainDiv.querySelector(".extraFilesList");
    filesListEl.setInnerHTML("");
    for (FileDescription file: repository.getAllExtraFilesSorted())
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setClassName("plomUiButton");
      a.setHref("#");
      a.setTextContent(file.getPath());
      String fileName = file.getPath();
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
        if (fileName.endsWith(".html")
            || fileName.endsWith(".txt")
            || fileName.endsWith(".md")
            || fileName.endsWith(".js"))
        {
          repository.getExtraFilesManager().getFileContents(fileName, (ArrayBuffer arrbuf) -> {
            String contents = WebHelpers.decoder.decode(arrbuf);
            textEditorViewCallback.load(fileName, contents);
          });
        }
//        viewSwitchCallback.load(fnName);
      }, false);
      DivElement div = doc.createDivElement();
      if (file.isImported)
        div.getClassList().add("moduleImported");
//      AnchorElement deleteAnchor = (AnchorElement)doc.createElement("a");
//      deleteAnchor.setClassName("plomUiRemoveButton");
//      deleteAnchor.setHref("#");
//      if (file.isImported)
//        deleteAnchor.getStyle().setDisplay(Display.NONE);
//      deleteAnchor.addEventListener(Event.CLICK, (evt) -> {
//        evt.preventDefault();
//        repository.deleteFunctionAndResetIds(fnName.module, fnName.id);
//        rebuildView();
//      }, false);
      div.appendChild(a);
      div.appendChild(doc.createTextNode(" "));
//      div.appendChild(deleteAnchor);
      filesListEl.appendChild(div);
    }

  }
  
  static String varInnerHtml(String divClass, String deleteLinkClass)
  {
    return "<div class=\"" + divClass + "\">.<plom-autoresizing-input></plom-autoresizing-input> <div class=\"typeEntry\">&nbsp;</div> <a href=\"#\" aria-label=\"delete\" class=\"" + deleteLinkClass + " plomUiRemoveButton\"></a></div>";
  }
  
//  private String globalVarInnerHtml()
//  {
//    return varInnerHtml("global_var", "delete_global_var");
//  }
  
//  private void addGlobalVarEntry(DivElement mainDiv,
//      VariableDescription v,
//      List<DivElement> varDivs)
//  {
//    String name = v.name;
//    Token.ParameterToken type = v.type; 
//    DivElement div = doc.createDivElement();
//    div.setInnerHTML(globalVarInnerHtml());
//    if (v.isImported)
//      div.getClassList().add("moduleImported");
//
//    ((InputElement)div.querySelector("plom-autoresizing-input")).setValue(name);
//    varDivs.add(div);
//    mainDiv.querySelector(".globalVarsList").appendChild(div);
//    int maxTypeWidth = div.querySelector(".global_var").getClientWidth();
//    TypeEntryField typeField = new TypeEntryField(type, (DivElement)div.querySelector(".typeEntry"), simpleEntry, false,
//        (scope, coreTypes) -> {
//          StandardLibrary.createGlobals(null, scope, coreTypes);
//          scope.setParent(new RepositoryScope(repository, coreTypes, null));
//        },
//        (context) -> {},
//        widthCalculator, maxTypeWidth, mainDiv.querySelector(".globaldetails"), mainDiv.querySelector(".globaldetails .scrollable-interior"));
//    typeField.setChangeListener((newType, isFinal) -> {
//      v.type = newType; 
//      repository.updateGlobalVariable(v);
//    });
//    typeField.render();
//    
//    InputElement nameInput = (InputElement)div.querySelector("plom-autoresizing-input"); 
//    nameInput.addEventListener(Event.CHANGE, (evt) -> {
//      v.name = nameInput.getValue(); 
//      repository.updateGlobalVariable(v);
//    }, false);
//    
//    AnchorElement deleteAnchor = (AnchorElement)div.querySelector("a.delete_global_var");
//    if (v.isImported)
//      deleteAnchor.getStyle().setDisplay(Display.NONE);
//    deleteAnchor.addEventListener(Event.CLICK, (evt) -> {
//      evt.preventDefault();
//      repository.deleteGlobalVarAndResetIds(v.module, v.id);
//      rebuildView();
//    }, false);
//  }

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
  @JsFunction
  public static interface LoadTextEditorViewCallback
  {
    void load(String fileName, String contents);
  }
  
  @Override public void close()
  {
  }
}
