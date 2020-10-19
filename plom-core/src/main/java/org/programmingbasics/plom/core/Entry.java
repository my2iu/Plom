package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.function.Consumer;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.view.LineForPosition;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.Blob;
import elemental.html.DivElement;
import elemental.html.FileReader;
import elemental.html.InputElement;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

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
- null and Null type
- when first creating a string/number, the cursor position should be shown as being after the token or on the token, not before it
- keep track of names of functions so that it can show up in stack traces
- default to a name of empty string to the first piece of code executed
- store function name when an error is thrown
- properly handle focus and loss of focus on type entry fields of the method panel
- do type checking of arguments of a function
- type checking of return type of a function
- object super
- constructor chaining
- separate constructor list
- call constructor from instance method
- constructors cannot return a value
- autogeneration of getter and setters for instance data members
- calls to the repository will eventually be asynchronous on iOS 
- generics
- js native API
- better formatting of saved code
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
- standardize value stuff so that functions always return and pass around direct pointers, but you are expected to copy the contents (unless you specify explicitly something else)? 
 */

public class Entry implements EntryPoint
{
  @Override
  public void onModuleLoad()
  {
    // Load in the built-in primitives of the interpreter into the 
    // code repository so that they can be browsed in the UI
    repository = new ModuleCodeRepository();
    repository.setChainedRepository(makeStdLibRepository());
    // Create a basic main function that can be filled in
    FunctionDescription mainFunc = new FunctionDescription(
        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("var", Symbol.Var),
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":", Symbol.Colon),
                Token.ParameterToken.fromContents("@string", Symbol.AtType)
                ),
            new TokenContainer(
                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                new Token.SimpleToken(":=", Symbol.Assignment),
                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
                ),
            new TokenContainer(
                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
                    new TokenContainer(
                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
                        new Token.SimpleToken("=", Symbol.Eq),
                        new Token.SimpleToken("\"8\"", Symbol.String)
                        ),
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
                                    ))
                            ))
                    ),
                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
                    new StatementContainer(
                        new TokenContainer(
                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
                                new TokenContainer(
                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
                                    ))
                            ))
                    )
                )
            )
        );
    repository.addFunctionAndResetIds(mainFunc);
    
    // Need to have a basic way to run code initially in order to get a better
    // feel for the design of the programming language
    hookRun();
    
    hookLoadSave();
    
    hookSubject();
    
    loadFunctionCodeView("main");
  }

  ModuleCodeRepository repository;
  
  CodePanel codePanel;
  MethodPanel methodPanel;
  ClassPanel classPanel;
  GlobalsPanel globalsPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
  String currentFunctionBeingViewed = null;
  ClassDescription currentMethodClassBeingViewed = null;
  FunctionDescription currentMethodBeingViewed = null;
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

  static ModuleCodeRepository makeStdLibRepository()
  {
    ModuleCodeRepository newRepository = new ModuleCodeRepository();
    newRepository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    try {
      newRepository.loadModule(new PlomTextReader.PlomTextScanner(new PlomTextReader.StringTextReader(UIResources.INSTANCE.getStdLibPlom().getText())));
    }
    catch (PlomReadException e)
    {
      e.printStackTrace();
    }
    newRepository.markAsImported();
    return newRepository;
  }
      
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
          
          addPrimitives(terp, coreTypes);
        });
      } 
      catch (Exception err)
      {
        Browser.getWindow().getConsole().log(err);
      }
    }, false);
  }

  private void addPrimitives(SimpleInterpreter terp, CoreTypeLibrary coreTypes)
  {
    // Stuff for @JS object
    coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "get:"),
        (blockWait, machine) -> {
          Value self = machine.currentScope().lookupThis();
          // We just store the JavaScript pointer as the object itself
          Object toReturn = ((JsPropertyMap<Object>)self.val).get(machine.currentScope().lookup("key").getStringValue());
          Browser.getWindow().getConsole().log(toReturn);
          blockWait.unblockAndReturn(Value.createVoidValue(machine.coreTypes()));
        });
    coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "at:set:"),
        (blockWait, machine) -> {
          Object index = machine.currentScope().lookup("index").val;
          Object value = machine.currentScope().lookup("value").val;
          Value self = machine.currentScope().lookupThis();
          ((JsPropertyMap<Object>)self.val).set((String)index, value);
          blockWait.unblockAndReturn(Value.createVoidValue(coreTypes));
        });
    coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "globals"),
        (blockWait, machine) -> {
          Value v = new Value();
          v.type = machine.currentScope().typeFromToken(Token.ParameterToken.fromContents("@JS object", Symbol.AtType));
          v.val = Js.global();
          blockWait.unblockAndReturn(v);
        });
  }
  
  
  void hookLoadSave()
  {
    Element loadEl = Browser.getDocument().querySelector("a.loadbutton");
    loadEl.addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      InputElement fileInput = Browser.getDocument().createInputElement();
      fileInput.setType("file");
      fileInput.getStyle().setDisplay(Display.NONE);
      Browser.getDocument().getBody().appendChild(fileInput);
      fileInput.setAccept(".plom");
      fileInput.addEventListener(Event.CHANGE, (e) -> {
        e.preventDefault();
        if (fileInput.getFiles().length() != 0)
        {
          FileReader reader = Browser.getWindow().newFileReader();
          reader.setOnload((loadedEvt) -> {
            String read = (String)reader.getResult();
            PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(read);
            PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
            try {
              ModuleCodeRepository newRepository = makeStdLibRepository();
              newRepository.loadModule(lexer);
              repository = newRepository;
              loadGlobalsView();
            }
            catch (PlomReadException e1)
            {
              e1.printStackTrace();
            }
          });
          reader.readAsText(fileInput.getFiles().item(0));
        }
        Browser.getDocument().getBody().removeChild(fileInput);
      }, false);
      fileInput.click();
    }, false);
    Element saveEl = Browser.getDocument().querySelector("a.savebutton");
    saveEl.addEventListener(Event.CLICK, (evt) -> {
      evt.preventDefault();
      AnchorElement saveLink = (AnchorElement)Browser.getDocument().createElement("a");
      try
      {
        StringBuilder out = new StringBuilder();
        repository.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out));
        Blob blob = createBlob(out.toString(), "text/plain");
        saveLink.setHref(createBlobObjectURL(blob));
        saveLink.setDownload("program.plom");
        saveLink.click();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }, false);
  }
 
  static native Blob createBlob(String data, String mimeType) /*-{
    return new $wnd.Blob([data], {type: mimeType});
  }-*/;

  static native String createBlobObjectURL(Blob blob) /*-{
    return URL.createObjectURL(blob);
  }-*/;

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
      if (currentMethodBeingViewed != null)
      {
        // Fill this in properly
        currentMethodBeingViewed.code = codePanel.codeList;
        currentMethodClassBeingViewed.updateMethod(currentMethodBeingViewed);
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
  
  void fillBreadcrumbForFunction(Element breadcrumbEl, FunctionDescription sig)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("." + sig.sig.getDisplayName() + " \u270e");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadFunctionSignatureView(sig);
    }, false);
    breadcrumbEl.appendChild(a);
  }

  void fillBreadcrumbForClass(Element breadcrumbEl, ClassDescription cls)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("@" + cls.name);
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadClassView(cls);
    }, false);
    breadcrumbEl.appendChild(a);
  }

  void fillBreadcrumbForMethod(Element breadcrumbEl, ClassDescription cls, FunctionDescription m)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("@" + cls.name);
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadClassView(cls);
    }, false);
    breadcrumbEl.appendChild(a);
    
    a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("." + m.sig.getDisplayName() + " \u270e");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadMethodSignatureView(cls, m);
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
    fillBreadcrumbForFunction(breadcrumbEl, repository.getFunctionDescription(fnName));
    
    closeCodePanelIfOpen();
    currentFunctionBeingViewed = fnName;
    showCodePanel(repository.getFunctionDescription(fnName).code);
  }

  void loadFunctionSignatureView(FunctionDescription sig)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, sig);
    
    closeCodePanelIfOpen();
    showFunctionPanel(sig);
  }

  void loadClassView(ClassDescription cls)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForClass(breadcrumbEl, cls);
    
    closeCodePanelIfOpen();
    showClassPanel(cls);
  }

  void loadMethodCodeView(ClassDescription cls, FunctionDescription m)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForMethod(breadcrumbEl, cls, m);
    
    closeCodePanelIfOpen();
    currentMethodClassBeingViewed = cls;
    currentMethodBeingViewed = m;
    showCodePanel(m.code);
  }

  void loadMethodSignatureView(ClassDescription cls, FunctionDescription m)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForMethod(breadcrumbEl, cls, m);
    
    closeCodePanelIfOpen();
    showMethodPanel(cls, m);
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
    currentMethodBeingViewed = null;
    currentMethodClassBeingViewed = null;
    codePanel = null;
  }
  
  private void showCodePanel(StatementContainer code)
  {
    codePanel = new CodePanel(getMainDiv());
    codePanel.setVariableContextConfigurator(
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes));
        },
        (context) -> {
          if (currentFunctionBeingViewed == null && currentMethodBeingViewed == null)
            return;

          if (currentMethodClassBeingViewed != null)
          {
            // Create an object scope that will handle this and instance variables
            try {
              Value thisValue = new Value();
              thisValue.type = context.currentScope().typeFromToken(Token.ParameterToken.fromContents("@" + currentMethodClassBeingViewed.name, Symbol.AtType));
              context.pushObjectScope(thisValue);
            } 
            catch (RunException e)
            {
              // Ignore any errors when setting this
            }
          }

          // Add in function arguments
          FunctionDescription fd = null;
          if (currentFunctionBeingViewed != null)
            fd = repository.getFunctionDescription(currentFunctionBeingViewed);
          else if (currentMethodBeingViewed != null)
            fd = currentMethodBeingViewed;
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
          @Override public void loadFunctionSignatureView(FunctionDescription sig) { Entry.this.loadFunctionSignatureView(sig); }
          @Override public void loadFunctionCodeView(FunctionDescription fnName) { Entry.this.loadFunctionCodeView(fnName.sig.getLookupName()); }
          @Override public void loadClassView(ClassDescription cls) { Entry.this.loadClassView(cls); }
        });
  }
  
  private void showFunctionPanel(FunctionDescription sig)
  {
    methodPanel = new MethodPanel(getMainDiv(), repository, sig.sig);
    methodPanel.setListener((newSig, isFinal) -> {
      repository.changeFunctionSignature(newSig, sig);
      loadFunctionCodeView(newSig.getLookupName());
    });
  }

  private void showMethodPanel(ClassDescription cls, FunctionDescription m)
  {
    methodPanel = new MethodPanel(getMainDiv(), repository, m.sig);
    methodPanel.setListener((newSig, isFinal) -> {
      m.sig = newSig;
      cls.updateMethod(m);
      loadMethodCodeView(cls, m);
    });
  }

  private void showClassPanel(ClassDescription cls)
  {
    classPanel = new ClassPanel(getMainDiv(), repository, cls, 
        new ClassPanel.ClassPanelViewSwitcher() {
          @Override public void loadMethodSignatureView(ClassDescription cls, FunctionDescription method)
            { Entry.this.loadMethodSignatureView(cls, method); }
          @Override public void loadMethodCodeView(ClassDescription cls, FunctionDescription method)
            { Entry.this.loadMethodCodeView(cls, method); }
        });
  }

}