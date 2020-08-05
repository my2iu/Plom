package org.programmingbasics.plom.core;

import java.util.function.Consumer;

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
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
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
- properly handle focus and loss of focus on type entry fields of the method panel
- do type checking of arguments of a function
- type checking of return type of a function
- move standard library code back out of the repository and into the standard library so that people don't need to have a repository for normal stuff (and so that standard library can't be modified)
- global variables
- objects
- generics
- js native API
- saving and loading
- export + import projects
- github integration
- debugger
- standard library
- Android + iOS apps
- web lessons
- tutorial mode
- export as html5 program
- external images, external html
- run in a separate browser window
- transpile to JavaScript
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
  GlobalsPanel globalsPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
  String currentFunctionBeingViewed = null;
  Consumer<Throwable> errorLogger = (err) -> {
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
  }; 
      
      
  void hookRun()
  {
    Element runEl = Browser.getDocument().querySelector("a.runbutton");
    runEl.addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      saveCodeToRepository();
      // Find code to run
      FunctionDescription fd = repository.getFunctionDescription("main");
      if (fd == null)
      {
        errorLogger.accept(new RunException("No main function"));
        return;
      }
      SimpleInterpreter terp = new SimpleInterpreter(fd.code);
      terp.setErrorLogger(errorLogger);
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
 
  /** If any code is currently being displayed in the code panel, save it
   * to the repository
   */
  void saveCodeToRepository()
  {
    if (codePanel != null) 
    {
      if (currentFunctionBeingViewed != null)
      {
        repository.getFunctionDescription(currentFunctionBeingViewed).code = codePanel.codeList;
      }
    }
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
    a.setTextContent("." + sig.getDisplayName() + " \u270e");
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
    saveCodeToRepository();
    if (codePanel != null) 
    {
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
    codePanel.setVariableContextConfigurator((context) -> {
      // Add in function arguments
      if (currentFunctionBeingViewed != null)
      {
        FunctionDescription fd = repository.getFunctionDescription(currentFunctionBeingViewed);
        if (fd != null)
        {
          context.pushNewScope();
          for (int n = 0; n < fd.sig.argNames.size(); n++)
          {
            String name = fd.sig.argNames.get(n);
            Token.ParameterToken typeToken = fd.sig.argTypes.get(n);
            try {
              Type type = context.currentScope().typeFromToken(typeToken);
              context.currentScope().addVariable(name, type, new Value());
            }
            catch (RunException e)
            {
              // Ignore the argument if it doesn't have a valid type
            }
          }
        }
      }
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
    DivElement mainDiv = getMainDiv();
    
    globalsPanel = new GlobalsPanel(mainDiv, repository,
        new GlobalsPanel.GlobalsPanelViewSwitcher() {
          @Override public void loadFunctionSignatureView(FunctionSignature sig) { Entry.this.loadFunctionSignatureView(sig); }
          @Override public void loadFunctionCodeView(String fnName) { Entry.this.loadFunctionCodeView(fnName); }
        });
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