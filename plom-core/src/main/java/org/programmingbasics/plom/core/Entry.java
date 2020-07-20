package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.view.LineForPosition;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.InputElement;

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
 */

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    hookCodePanel();
    codePanel.setCode(repository.functions.get("main").code);
    
    // Need to have a basic way to run code initially in order to get a better
    // feel for the design of the programming language
    hookRun();
    
    hookSubject();
  }
  
  
  CodePanel codePanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();

  ModuleCodeRepository repository = new ModuleCodeRepository();
  
  void hookCodePanel()
  {
    codePanel = new CodePanel(getMainDiv());
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
      int lineNo = LineForPosition.inCode(codePanel.codeList, codePanel.cursorPos, lineNumbers);
      Element lineEl = Browser.getDocument().querySelector(".lineIndicator");
      lineEl.setTextContent("L" + lineNo);
    });
  }
 
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
          if (errTok != null) 
            lineNo = lineNumbers.tokenLine.getOrDefault(errTok, 0);
          if (lineNo == 0)
            msg.setTextContent("Run Error");
          else
            msg.setTextContent("Run Error (line " + lineNo + ")");
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
        terp.runNoReturn();
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
    
    fillBreadcrumbForFunction(breadcrumbEl, FunctionSignature.noArg("main"));
    
//    editEl.setTextContent("\u270e");
//    editEl.addEventListener(Event.CLICK, (e) -> {
//      e.preventDefault();
//    }, false);
  }
  
  void fillBreadcrumbForFunction(Element breadcrumbEl, FunctionSignature sig)
  {
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("Program");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      if (codePanel != null) codePanel.close();
      codePanel = null;
      showGlobalsPanel();
      // TODO: Adjust breadcrumbs
    }, false);
    breadcrumbEl.appendChild(a);
    
    a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("." + sig.getLookupName() + " \u270e");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      if (codePanel != null) codePanel.close();
      codePanel = null;
      showMethodPanel(sig);
    }, false);
    breadcrumbEl.appendChild(a);
  }
  
  private static DivElement getMainDiv()
  {
    return (DivElement)Browser.getDocument().querySelector("div.main");
  }
  
  void showGlobalsPanel()
  {
    Document doc = Browser.getDocument();
    DivElement mainDiv = getMainDiv();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
    
    Element functionListEl = mainDiv.querySelector(".functionList");
    
    for (String fnName: repository.functions.keySet())
    {
      AnchorElement a = (AnchorElement)doc.createElement("a");
      a.setHref("#");
      a.setTextContent(fnName);
      a.addEventListener(Event.CLICK, (e) -> {
        e.preventDefault();
      }, false);
      functionListEl.appendChild(a);
    }
  }
  
  void showMethodPanel(FunctionSignature sig)
  {
    DivElement mainDiv = getMainDiv();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getMethodPanelHtml().getText());
    
    // Fill in the function name
    ((InputElement)mainDiv.querySelectorAll("input").item(0)).setValue(sig.getLookupName());
    
    // Just some rough initial styling for type entry fields
    NodeList typeEntryNodes = mainDiv.querySelectorAll(".typeEntry");
    for (int n = 0; n < typeEntryNodes.length(); n++)
    {
      typeEntryNodes.item(n).setTextContent("\u00A0");
    }
    
    AnchorElement okButton = (AnchorElement)mainDiv.querySelector("a.done");
    okButton.addEventListener(Event.CLICK, (e) -> {
      hookCodePanel();
      e.preventDefault();
    }, false);
  }
}