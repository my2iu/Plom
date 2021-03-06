package org.programmingbasics.plom.core;

import java.io.IOException;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
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
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class Main
{
//  @Deprecated void createSampleCode()
//  {
//    // Load in the built-in primitives of the interpreter into the 
//    // code repository so that they can be browsed in the UI
//    repository = new ModuleCodeRepository();
//    repository.setChainedRepository(makeStdLibRepository());
//    // Create a basic main function that can be filled in
//    FunctionDescription mainFunc = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.SimpleToken("var", Symbol.Var),
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":", Symbol.Colon),
//                Token.ParameterToken.fromContents("@string", Symbol.AtType)
//                ),
//            new TokenContainer(
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":=", Symbol.Assignment),
//                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
//                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
//                ),
//            new TokenContainer(
//                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
//                    new TokenContainer(
//                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                        new Token.SimpleToken("=", Symbol.Eq),
//                        new Token.SimpleToken("\"8\"", Symbol.String)
//                        ),
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
//                                    ))
//                            ))
//                    ),
//                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
//                                    ))
//                            ))
//                    )
//                )
//            )
//        );
//    repository.addFunctionAndResetIds(mainFunc);
//  }
  
  public ModuleCodeRepository repository;
  
  CodePanel codePanel;
  MethodPanel methodPanel;
  ClassPanel classPanel;
  GlobalsPanel globalsPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
  String currentFunctionBeingViewed = null;
  ClassDescription currentMethodClassBeingViewed = null;
  FunctionDescription currentMethodBeingViewed = null;
//  private ErrorLogger errorLogger = createErrorLoggerForConsole(Browser.getDocument().querySelector(".console"));

  public ErrorLogger createErrorLoggerForConsole(Element consoleEl)
  {
    return (err) -> {
      Document doc = Browser.getDocument();
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
      else if (err instanceof Throwable && ((Throwable)err).getMessage() != null && !((Throwable)err).getMessage().isEmpty())
      {
        msg.setTextContent(((Throwable)err).getMessage());
      }
      else
      {
        msg.setTextContent(err.toString());
      }
      consoleEl.appendChild(msg);
    };
  }

//  public ErrorLogger getErrorLogger()
//  {
//    return errorLogger;
//  }
  
  public static String getStdLibCodeText()
  {
    return UIResources.INSTANCE.getStdLibPlom().getText();
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
    repository.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out), true);
    return out.toString();
  }
  
  public void saveModuleAndClasses(SaveModuleCallback moduleSaver, SaveClassCallback classSaver, DeleteClassCallback classDeleter)
  {
    try {
      StringBuilder out = new StringBuilder();
      repository.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out), false);
      moduleSaver.saveModule(out.toString());
      
      // Delete any classes that no longer exist
      for (ClassDescription cls: repository.deletedClasses)
      {
        if (cls.getOriginalName() == null) continue;
        classDeleter.deleteClass(cls.getOriginalName());
      }
      for (ClassDescription cls: repository.classes)
      {
        if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
        {
          if (cls.getOriginalName() == null) continue;
          if (cls.getOriginalName().equals(cls.getName())) continue;
          classDeleter.deleteClass(cls.getOriginalName());
        }
      }
      
      // Save all classes
      for (ClassDescription cls: repository.classes)
      {
        if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
        {
          out = new StringBuilder();
          ModuleCodeRepository.saveClass(new PlomTextWriter.PlomCodeOutputFormatter(out), cls);
          classSaver.saveClass(cls.getName(), out.toString());
        }
      }
    } catch (IOException e) {
      // Ignore errors
      e.printStackTrace();
    }
  }
  
  /**
   * Call this to adjust the layout/dimensions of things after a resize
   */
  public void updateAfterResize()
  {
    if (codePanel != null)
      codePanel.updateAfterResize();
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
      loadFunctionSignatureView(sig, false);
    }, false);
    breadcrumbEl.appendChild(a);
  }

  void fillBreadcrumbForClass(Element breadcrumbEl, ClassDescription cls)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("@" + cls.getName());
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadClassView(cls, false);
    }, false);
    breadcrumbEl.appendChild(a);
  }

  void fillBreadcrumbForMethod(Element breadcrumbEl, ClassDescription cls, FunctionDescription m)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    Document doc = Browser.getDocument();
    
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("@" + cls.getName());
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadClassView(cls, false);
    }, false);
    breadcrumbEl.appendChild(a);
    
    a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("." + m.sig.getDisplayName() + " \u270e");
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadMethodSignatureView(cls, m, false);
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

  void loadFunctionSignatureView(FunctionDescription sig, boolean isNew)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, sig);
    
    closeCodePanelIfOpen();
    showFunctionPanel(sig, isNew);
  }

  void loadClassView(ClassDescription cls, boolean isNew)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForClass(breadcrumbEl, cls);
    
    closeCodePanelIfOpen();
    showClassPanel(cls, isNew);
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

  void loadMethodSignatureView(ClassDescription cls, FunctionDescription m, boolean isNew)
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = subjectEl.querySelector(".breadcrumb");
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForMethod(breadcrumbEl, cls, m);
    
    closeCodePanelIfOpen();
    showMethodPanel(cls, m, isNew);
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
  
  public void closeCodePanelWithoutSavingIfOpen()
  {
    if (codePanel != null) 
    {
      codePanel.close();
    }
    currentFunctionBeingViewed = null;
    currentMethodBeingViewed = null;
    currentMethodClassBeingViewed = null;
    codePanel = null;
  }
  
  private void closeCodePanelIfOpen()
  {
    saveCodeToRepository();
    closeCodePanelWithoutSavingIfOpen();
  }
  
  private void showCodePanel(StatementContainer code)
  {
    codePanel = new CodePanel(getMainDiv(), true);
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
              thisValue.type = context.currentScope().typeFromToken(Token.ParameterToken.fromContents("@" + currentMethodClassBeingViewed.getName(), Symbol.AtType));
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
        (FunctionDescription fnName) -> Main.this.loadFunctionCodeView(fnName.sig.getLookupName()),
        (FunctionDescription sig, boolean isNew) -> loadFunctionSignatureView(sig, isNew),
        (ClassDescription cls, boolean isNew) -> loadClassView(cls, isNew)
        );
  }
  
  private void showFunctionPanel(FunctionDescription sig, boolean isNew)
  {
    methodPanel = new MethodPanel(getMainDiv(), repository, sig.sig, isNew);
    methodPanel.setListener((newSig, isFinal) -> {
      repository.changeFunctionSignature(newSig, sig);
      loadFunctionCodeView(newSig.getLookupName());
    });
  }

  private void showMethodPanel(ClassDescription cls, FunctionDescription m, boolean isNew)
  {
    methodPanel = new MethodPanel(getMainDiv(), repository, m.sig, isNew);
    methodPanel.setListener((newSig, isFinal) -> {
      m.sig = newSig;
      cls.updateMethod(m);
      loadMethodCodeView(cls, m);
    });
  }

  private void showClassPanel(ClassDescription cls, boolean isNew)
  {
    classPanel = new ClassPanel(getMainDiv(), repository, cls, 
        (ClassDescription c, FunctionDescription method) -> loadMethodCodeView(c, method),
        (ClassDescription c, FunctionDescription method, boolean isNewMethod) -> loadMethodSignatureView(c, method, isNewMethod),
        () -> loadGlobalsView(),
        isNew);
  }

  
  @JsFunction public static interface SaveModuleCallback
  {
    public void saveModule(String contents);
  }
  
  @JsFunction public static interface SaveClassCallback
  {
    public void saveClass(String className, String contents);
  }
  
  @JsFunction public static interface DeleteClassCallback
  {
    public void deleteClass(String className);
  }

}
