package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.view.LineForPosition;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;

/*
TODO:
- functions
- number constants (change inputmode to numeric)
- string constants (allow multi-line strings?)
- valign to middle
- use @ for types?
- keyboard movement
- keyboard entry
- adding a newline in the middle of a function call
- type checking for errors
- functions and methods returning void
- null and Null type
- when first creating a string/number, the cursor position should be shown as being after the token or on the token, not before it
- keep track of names of functions so that it can show up in stack traces
- default to a name of empty string to the first piece of code executed
- store function name when an error is thrown
- properly support passing in arguments for functions
- save code before running things
- always run the main function, not whatever code is showing in the code panel 
 */

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    // Need to have a basic way to run code initially in order to get a better
    // feel for the design of the programming language
    hookRun();
    
    hookSubject();
    
    loadFunctionCodeView("main");
  }

  ModuleCodeRepository repository = new ModuleCodeRepository();
  
  CodePanel codePanel;
  MethodPanel methodPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
  String currentFunctionBeingViewed = null;
  
  void hookRun()
  {
    Element runEl = Browser.getDocument().querySelector("a.runbutton");
    runEl.addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      if (codePanel == null) return;
      SimpleInterpreter terp = new SimpleInterpreter(codePanel.codeList);
      terp.setErrorLogger((err) -> {
        Document doc = Browser.getDocument();
        Element consoleEl = doc.querySelector(".console");
        consoleEl.setInnerHTML("");
        DivElement msg = doc.createDivElement();
        if (err instanceof ParseException)
        {
          ParseException parseErr = (ParseException)err;
          int lineNo = lineNumbers.tokenLine.getOrDefault(parseErr.token, 0);
          if (lineNo == 0)
            msg.setTextContent("Syntax Error");
          else
            msg.setTextContent("Syntax Error (line " + lineNo + ")");
        }
        else if (err instanceof RunException)
        {
          RunException runErr = (RunException)err;
          Token errTok = runErr.getErrorTokenSource();
          int lineNo = 0;
          String errString = "Run Error";
          if (runErr.getMessage() != null && !runErr.getMessage().isEmpty())
            errString = runErr.getMessage();
          if (errTok != null) 
            lineNo = lineNumbers.tokenLine.getOrDefault(errTok, 0);
          if (lineNo == 0)
            msg.setTextContent(errString);
          else
            msg.setTextContent(errString + " (line " + lineNo + ")");
        }
        else if (err.getMessage() != null && !err.getMessage().isEmpty())
        {
          msg.setTextContent(err.getMessage());
        }
        else
        {
          msg.setTextContent(err.toString());
        }
        consoleEl.appendChild(msg);
      });
      try {
        terp.runNoReturn((scope, coreTypes) -> {
          StandardLibrary.createGlobals(terp, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes));
        });
      } 
      catch (Exception err)
      {
        Browser.getWindow().getConsole().log(err);
      }
    }, false);
  }
 
  /**
   * Basic UI for changing the class/method/function
   */
  void hookSubject()
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
//    Element editEl = subjectEl.querySelector(".edit");
    
//    fillBreadcrumbForFunction(breadcrumbEl, FunctionSignature.noArg("main"));
    
//    editEl.setTextContent("\u270e");
//    editEl.addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//    }, false);
  }
  
  void fillBreadcrumbForGlobals(Element breadcrumbEl)
  {
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("Program");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadGlobalsView();
    }, false);
    breadcrumbEl.appendChild(a);
  }
  
  void fillBreadcrumbForFunction(Element breadcrumbEl, FunctionSignature sig)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("." + sig.getLookupName() + " \u270e");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadFunctionSignatureView(sig);
    }, false);
    breadcrumbEl.appendChild(a);
  }
  
  private static DivElement getMainDiv()
  {
    return (DivElement)Browser.getDocument().querySelector("div.main");
  }
  
  void loadFunctionCodeView(String fnName)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, repository.getFunctionDescription(fnName).sig);
    
    closeCodePanelIfOpen();
    currentFunctionBeingViewed = fnName;
    showCodePanel(repository.getFunctionDescription(fnName).code);
  }

  void loadFunctionSignatureView(FunctionSignature sig)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, sig);
    
    closeCodePanelIfOpen();
    showMethodPanel(sig);
  }
  
  void loadGlobalsView()
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForGlobals(breadcrumbEl);
    
    closeCodePanelIfOpen();
    showGlobalsPanel();
  }
  
  private void closeCodePanelIfOpen()
  {
    if (codePanel != null) 
    {
      if (currentFunctionBeingViewed != null)
      {
        repository.getFunctionDescription(currentFunctionBeingViewed).code = codePanel.codeList;
      }
      codePanel.close();
    }
    currentFunctionBeingViewed = null;
    codePanel = null;
  }
  
  private void showCodePanel(StatementContainer code)
  {
    codePanel = new CodePanel(getMainDiv(), (scope, coreTypes) -> {
      StandardLibrary.createGlobals(null, scope, coreTypes);
      scope.setParent(new RepositoryScope(repository, coreTypes));
    });
    codePanel.setListener((isCodeChanged) -> {
      if (isCodeChanged)
      {
        // Update error list
        codePanel.codeErrors.clear();
        try {
          ParseToAst.parseStatementContainer(codePanel.codeList, codePanel.codeErrors);
        }
        catch (Exception e)
        {
          // No errors should be thrown
        }
        // Update line numbers
        lineNumbers.calculateLineNumbersForStatements(codePanel.codeList, 1);
      }
      if (codePanel.cursorPos != null)
      {
        int lineNo = LineForPosition.inCode(codePanel.codeList, codePanel.cursorPos, lineNumbers);
        Element lineEl = Browser.getDocument().querySelector(".lineIndicator");
        lineEl.setTextContent("L" + lineNo);
      }
    });
    
    if (code != null)
      codePanel.setCode(code);
    else
      codePanel.setCode(new StatementContainer());
  }
 

  private void showGlobalsPanel()
  {
    Document doc = Browser.getDocument();
    DivElement mainDiv = getMainDiv();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
    
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
          FunctionSignature.noArg(newFunctionName),
          new StatementContainer());
      repository.addFunction(func);
      
      loadFunctionSignatureView(func.sig);
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
        loadFunctionCodeView(fnName);
      }, false);
      DivElement div = doc.createDivElement();
      div.appendChild(a);
      functionListEl.appendChild(div);
    }
  }
  
  private void showMethodPanel(FunctionSignature sig)
  {
    methodPanel = new MethodPanel(getMainDiv(), sig);
    methodPanel.setListener((newSig, isFinal) -> {
      repository.changeFunctionSignature(newSig, sig);
      loadFunctionCodeView(newSig.getLookupName());
    });
    
  }

}