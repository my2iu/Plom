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
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.view.LineForPosition;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import jsinterop.annotations.JsType;

@JsType
public class Main
{
  @Deprecated void createSampleCode()
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
  }
  
  public ModuleCodeRepository repository;
  
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

  public static ModuleCodeRepository makeStdLibRepository()
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
  
  /** If any code is currently being displayed in the code panel, save it
   * to the repository
   */
  public void saveCodeToRepository()
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
  
  public String getModuleAsString() throws IOException 
  {
    StringBuilder out = new StringBuilder();
    repository.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out));
    return out.toString();
  }
  
  /**
   * Basic UI for changing the class/method/function
   */
  public void hookSubject()
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
  
  public void loadFunctionCodeView(String fnName)
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

  public void loadGlobalsView()
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
          @Override public void loadFunctionSignatureView(FunctionDescription sig) { Main.this.loadFunctionSignatureView(sig); }
          @Override public void loadFunctionCodeView(FunctionDescription fnName) { Main.this.loadFunctionCodeView(fnName.sig.getLookupName()); }
          @Override public void loadClassView(ClassDescription cls) { Main.this.loadClassView(cls); }
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
            { Main.this.loadMethodSignatureView(cls, method); }
          @Override public void loadMethodCodeView(ClassDescription cls, FunctionDescription method)
            { Main.this.loadMethodCodeView(cls, method); }
        });
  }

}
