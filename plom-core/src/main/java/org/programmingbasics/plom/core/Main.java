package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.programmingbasics.plom.core.codestore.RepositoryScope;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FileDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
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
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.LogLevel;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.view.LineForPosition;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.FileReader;
import elemental.html.IFrameElement;
import elemental.html.InputElement;
import elemental.html.ScriptElement;
import elemental.js.util.JsArrayOf;
import elemental.js.util.JsArrayOfString;
import elemental.util.ArrayOf;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

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
  
  public CodeRepositoryClient repository;
  
  CodeWidgetBase codePanel;
  MethodPanel methodPanel;
  ClassPanel classPanel;
  GlobalsPanel globalsPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
  String currentFunctionBeingViewed = null;
  ClassDescription currentMethodClassBeingViewed = null;
  FunctionDescription currentMethodBeingViewed = null;
//  private ErrorLogger errorLogger = createErrorLoggerForConsole(Browser.getDocument().querySelector(".console"));

  /**
   * This flag tells the Plom engine that it is running from a virtual 
   * web server, and there is a debugger environment available that can
   * be used to send debug messages and control messages back to the
   * main IDE. 
   */
  public static boolean debuggerEnvironmentAvailableFlag = false;
  
  /**
   * The url of the parent window of the Plom program. This parent
   * window holds the IDE that the debugger will try to connect
   * to
   */
  static String debuggerIdeParentWindowOrigin;

  public static void configureDebuggerEnvironmentAt(String ideParentWindowOrigin)
  {
    // I'm having problems writing these static variables from JS
    // (the values written don't seem to be picked up the Java side),
    // so I'm using a static method to set them.
    debuggerEnvironmentAvailableFlag = true;
    debuggerIdeParentWindowOrigin = ideParentWindowOrigin;
  }

  
  /** The File System Access API uses async iterators, and GWT doesn't
   * work well with async iterators, so to work with them, we need some
   * external JS code to be injected in
   */
  static WebHelpers.AsyncIteratorCollector asyncIteratorToArray;
  public static void setAsyncIteratorToArray(WebHelpers.AsyncIteratorCollector fn) { asyncIteratorToArray = fn; }
  
  public static ErrorLogger createErrorLoggerForConsole()
  {
    return new ErrorLogger() {
      @Override public void error(Object err)
      {
        Browser.getWindow().getConsole().log(err);
      }
      @Override public void warn(Object err)
      {
        Browser.getWindow().getConsole().log(err);
      }
      @Override public void debugLog(Object value)
      {
        Browser.getWindow().getConsole().log(value);
      }
      @Override public void log(String msg, LogLevel logLevel, ProgramCodeLocation location)
      {
        Browser.getWindow().getConsole().log(msg);
      }
    };
  }
  
  public ErrorLogger createErrorLoggerForDiv(Element consoleEl)
  {
    return new ErrorLogger() {
      private void logErr(Object err, LogLevel logLevel)
      {
        String msgString;
        if (err instanceof ParseException)
        {
          ParseException parseErr = (ParseException)err;
          int lineNo = lineNumbers.tokenLine.getOrDefault(parseErr.token, 0);
          if (lineNo == 0)
            msgString = "Syntax Error";
          else
            msgString = "Syntax Error (line " + lineNo + ")";
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
            msgString = errString;
          else
            msgString = errString + " (line " + lineNo + ")";
        }
        else if (err instanceof Throwable && ((Throwable)err).getMessage() != null && !((Throwable)err).getMessage().isEmpty())
        {
          msgString = ((Throwable)err).getMessage();
        }
        else
        {
          msgString = err.toString();
        }
        debugLog(msgString);
      }
      @Override public void error(Object err)
      {
        logErr(err, ERROR);
      }
      @Override public void warn(Object err)
      {
        logErr(err, WARN);
      }
      @Override public void debugLog(Object value)
      {
        log(value.toString(), DEBUG, null);
      }
      @Override public void log(String value, LogLevel logLevel, ProgramCodeLocation location)
      {
        Document doc = Browser.getDocument();
        consoleEl.setInnerHTML("");
        DivElement msg = doc.createDivElement();
        msg.setTextContent(value);
        consoleEl.appendChild(msg);
      }
    };
  }

  public static DebuggerEnvironment createDebuggerEnvironment()
  {
    if (Browser.getWindow().getLocation().getProtocol().startsWith("plomrun")) 
    {
      // Running as an iOS app in the WKWebView
    }
    else if (Browser.getWindow().getLocation().getProtocol().startsWith("http")
        && Browser.getWindow().getLocation().getHost().equals("webviewbridge.plom.dev"))
    {
      // Running as an Android app in the Android web view
    }
    else
    {
      // Running from a service worker
    }
    return new DebuggerEnvironment.ServiceWorkerDebuggerEnvironment(debuggerIdeParentWindowOrigin);
  }
  
  /**
   * Sets up a debug connection to the Plom program running in the
   * given iframe (the iframe should be pointing to the specified
   * URL)
   */
  public DebuggerConnection makeDebuggerConnection(IFrameElement iframeEl, Element consoleDiv, OnLocationJump onLocationJump)
  {
    DebuggerConnection.ServiceWorkerDebuggerConnection debugConnection;
    debugConnection = new DebuggerConnection.ServiceWorkerDebuggerConnection(iframeEl, consoleDiv);
    // CodeLocationJumper that's suitable for a full IDE with classes, methods, etc.
    debugConnection.setCodeLocationJumper((loc) -> {
      if (loc.getClassName() == null && loc.getFunctionMethodName() != null && loc.getPosition() == null)
      {
        FunctionDescription sig = repository.getFunctionDescription(loc.getFunctionMethodName());
        if (sig != null)
        {
          onLocationJump.onJump();
          loadFunctionSignatureView(sig, false);
        }
      }
      else if (loc.getClassName() == null && loc.getFunctionMethodName() != null && loc.getPosition() != null)
      {
        onLocationJump.onJump();
        loadFunctionCodeView(loc.getFunctionMethodName());
        codePanel.setCursorPosition(loc.getPosition());
      }
      else if (loc.getClassName() != null && loc.getFunctionMethodName() == null)
      {
        ClassDescription cls = repository.findClassWithName(loc.getClassName());
        if (cls != null)
        {
          onLocationJump.onJump();
          loadClassView(cls, false);
        }
      }
      else if (loc.getClassName() != null && loc.getFunctionMethodName() != null)
      {
        ClassDescription cls = repository.findClassWithName(loc.getClassName());
        if (cls != null)
        {
          FunctionDescription method = cls.findMethod(loc.getFunctionMethodName(), loc.isStatic()); 
          if (method != null)
          {
            if (loc.getPosition() != null)
            {
              onLocationJump.onJump();
              loadMethodCodeView(cls, method);
              codePanel.setCursorPosition(loc.getPosition());
            }
            else
            {
              onLocationJump.onJump();
              loadMethodSignatureView(cls, method, false);
            }
          }
        }
      }
    });
    return debugConnection;
  }
  
  @JsFunction
  static interface OnLocationJump 
  {
    public void onJump();
  }
  
  /**
   * Creates a small code fragment that invokes the .main function.
   * This small code fragment can be used as the entrypoint code
   * for a Plom program
   */
  public static StatementContainer makeEntryPointCodeToInvokeMain()
  {
    return new StatementContainer(
        new TokenContainer(
            Token.ParameterToken.fromContents(".main", Symbol.DotVariable)
            ));
  }
  
  public static String getStdLibCodeText()
  {
    return UIResources.INSTANCE.getStdLibPlom().getText();
  }
  
  /** Helper method for JavaScript to make a list of symbols from an array of symbol names */
  public static Collection<Symbol> jsMakeSymbolList(JsArrayOfString symbolNames)
  {
    List<Symbol> symbols = new ArrayList<>();
    for (int n = 0; n < symbolNames.length(); n++)
      symbols.add(Symbol.valueOf(symbolNames.get(n)));
    return symbols;
  }
  
  /** Helper method to add some more excluded tokens from the CodePanel */
  public static void jsCodePanelMoreExcludedTokens(CodePanel codePanel, JsArrayOfString symbolNames)
  {
    for (int n = 0; n < symbolNames.length(); n++)
      codePanel.filterExcludeTokens.add(Symbol.valueOf(symbolNames.get(n)));
  }

  /** Helper method to remove some tokens from the list of excluded tokens from a CodePanel */
  public static void jsCodePanelFewerExcludedTokens(CodePanel codePanel, JsArrayOfString symbolNames)
  {
    for (int n = 0; n < symbolNames.length(); n++)
      codePanel.filterExcludeTokens.remove(Symbol.valueOf(symbolNames.get(n)));
  }

  /** Helper method for JavaScript to make Java Lists from a js array */
  public static <T> List<T> jsMakeListFromArray(JsArrayOf<T> array)
  {
    List<T> list = new ArrayList<>();
    for (int n = 0; n < array.length(); n++)
      list.add(array.get(n));
    return list;
  }
  
  /** Helper method for JavaScript to show a file chooser */
  public static void jsShowFileChooser(String acceptExtension, boolean asString, ShowFileChooserCallback loadCallback)
  {
    InputElement fileInput = Browser.getDocument().createInputElement();
    fileInput.setType("file");
    fileInput.getStyle().setDisplay(Display.NONE);
    Browser.getDocument().getBody().appendChild(fileInput);
    if (acceptExtension != null)
        fileInput.setAccept(acceptExtension);
    fileInput.addEventListener(Event.CHANGE, (e) -> {
        e.preventDefault();
        if (fileInput.getFiles().getLength() != 0)
        {
            FileReader reader = (FileReader)new WebHelpers.FileReader();
//            FileReader reader = Browser.getWindow().newFileReader();
            reader.setOnload((loadedEvt) -> {
                loadCallback.fileLoaded(fileInput.getFiles().item(0).getName(), reader.getResult());
            });
            if (asString)
                reader.readAsText(fileInput.getFiles().item(0));
            else
                reader.readAsArrayBuffer(fileInput.getFiles().item(0));
        }
        Browser.getDocument().getBody().removeChild(fileInput);
    }, false);
    fileInput.click();
  }
  @JsFunction
  public static interface ShowFileChooserCallback
  {
    void fileLoaded(String name, Object result);
  }
  
  public void openFromProjectDir(WebHelpers.FileSystemDirectoryHandle baseDirHandle, CodeRepositoryClient newRepo)
  {
    newRepo.setExtraFilesManager(new ExtraFilesManagerFileSystemAccessApi(baseDirHandle));
    baseDirHandle.getDirectoryHandle("src")
        .then(srcHandle -> {
          return asyncIteratorToArray.<WebHelpers.FileSystemHandle>gather(srcHandle.values()); 
        })
        .then((ArrayOf<WebHelpers.FileSystemHandle> handles) -> {
          ArrayOf<WebHelpers.Promise<String>> srcFiles = elemental.util.Collections.arrayOf();
          for (int n = 0; n < handles.length(); n++)
          {
            if (handles.get(n).getKindEnum() == WebHelpers.FileSystemHandleKind.DIRECTORY)
              continue;
            String filename = handles.get(n).getName();
            WebHelpers.FileSystemFileHandle fileHandle = (WebHelpers.FileSystemFileHandle)handles.get(n);
            srcFiles.insert(srcFiles.length(), fileHandle.getFile()
                .then((file) -> {
                      return WebHelpers.readFileAsText(file).thenNow((code) -> {
                        try {
                          if ("program.plom".equals(filename))
                          {
                            PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(code);
                            PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
                            newRepo.loadModule(lexer);
                          }
                          else
                          {
                            PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(code);
                            PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
                            newRepo.loadClassIntoModule(lexer);
                          }
                        } catch (PlomReadException e)
                        {
                          e.printStackTrace();
                        }
                        return filename;
                      });
            }));
          }
          return WebHelpers.promiseAll(srcFiles);
        })
        .thenNow((done) -> {
            if (newRepo.isNoStdLibFlag)
              newRepo.setChainedRepository(null);
            repository = newRepo;

            repository.refreshExtraFiles(() -> {
                closeCodePanelWithoutSavingIfOpen();  // Usually code panel will save over just loaded code when you switch view

                loadGlobalsView();
            });
            return null;
        });
    
    
    
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
  
  /** Packages up code in a single .js file that's suitable for running in a web browser */
  public WebHelpers.Promise<String> getModuleAsJsonPString(boolean withDebugEnvironment, String debuggerTargetOriginUrl) throws IOException
  {
    // Get module contents as a string
    StringBuilder out = new StringBuilder();
    repository.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out), true);
    
    // Wrap string in js
    return WebHelpersShunt.promiseResolve("plomEngineLoad = plomEngineLoad.then((repository) => {\n"
        + (withDebugEnvironment ? "org.programmingbasics.plom.core.Main.configureDebuggerEnvironmentAt('" + debuggerTargetOriginUrl + "');\n" : "")
        + "var code = `" + PlomTextWriter.escapeTemplateLiteral(out.toString()) + "`;\n"
        + "var skipPromise = loadCodeStringIntoExecutableRepository(code, repository);\n"
        + "return repository;\n"
        + "});\n");
  }

  public WebHelpers.Promise<String> getModuleWithFilesAsString() throws IOException 
  {
    StringBuilder out = new StringBuilder();

    return repository.saveModuleWithExtraFiles(new PlomTextWriter.PlomCodeOutputFormatter(out), true,
        new WebHelpers.PromiseCreator() {
          @Override public <U> WebHelpers.Promise<U> create(WebHelpers.Promise.PromiseConstructorFunction<U> createCallback)
          {
            return new WebHelpers.PromiseClass<>(createCallback);
          }
        },
        new WebHelpers.Promise.All() {
          @Override public <U> WebHelpers.Promise<ArrayOf<U>> all(ArrayOf<WebHelpers.Promise<U>> promises)
          {
            return WebHelpers.promiseAll(promises);
          }
        },
        buf -> Browser.getWindow().newUint8Array(buf, 0, buf.getByteLength()))
    .thenNow((dummy) -> out.toString());

  }

  public WebHelpers.Promise<Void> saveModuleAndClasses(SaveModuleCallback moduleSaver, SaveClassCallback classSaver, DeleteClassCallback classDeleter)
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
          CodeRepositoryClient.saveClass(new PlomTextWriter.PlomCodeOutputFormatter(out), cls);
          classSaver.saveClass(cls.getName(), out.toString());
        }
      }
    } catch (IOException e) {
      // Ignore errors
      e.printStackTrace();
    }
    return WebHelpers.PromiseClass.<Void>resolve(null);
  }
  
  /**
   * Bundles up all code and related files into a single zip that can
   * be uploaded as a website and run
   */
  public WebHelpers.Promise<Object> exportAsZip(WebHelpers.JSZip zip, String plomSystemFilePrefix, boolean exportAsBase64)
  {
    // Check if there's an index.html defined in the extra files, if not, create one
    List<FileDescription> extraFiles = repository.getAllExtraFilesSorted();
    if (!extraFiles.stream().anyMatch(fd -> "web/index.html".equals(fd.filePath)))
      zip.filePromiseArrayBuffer("index.html", WebHelpers.fetch(plomSystemFilePrefix + "plomweb.html").then(response -> response.arrayBuffer()));
    
    // Various required js files
    String plomDirectLoc;
    if (Js.isTruthy(Browser.getDocument().querySelector("iframe#plomcore")))
      plomDirectLoc = ((ScriptElement)((IFrameElement)Browser.getDocument().querySelector("iframe#plomcore")).getContentDocument().querySelector("script")).getSrc();
    else
      plomDirectLoc = plomSystemFilePrefix + "plomcore/plomdirect.js";
    zip.filePromiseArrayBuffer("plomdirect.js", WebHelpers.fetch(plomDirectLoc).then(response -> response.arrayBuffer()));
    zip.filePromiseArrayBuffer("plomStdlibPrimitives.js", WebHelpers.fetch(plomSystemFilePrefix + "plomStdlibPrimitives.js").then(response -> response.arrayBuffer()));
    zip.filePromiseArrayBuffer("plomUi.js", WebHelpers.fetch(plomSystemFilePrefix + "plomUi.js").then(response -> response.arrayBuffer()));
    
    // Extra files
    WebHelpers.PromiseCreator promiseCreator = new WebHelpers.PromiseCreator() {
      @Override public <U> WebHelpers.Promise<U> create(WebHelpers.Promise.PromiseConstructorFunction<U> createCallback)
      {
        return new WebHelpers.PromiseClass<>(createCallback);
      }
    };
    ExtraFilesManager fileManager = repository.getExtraFilesManager();
    for (FileDescription fd: extraFiles)
    {
      String nameInZip = fd.getPath();
      if (!nameInZip.startsWith("web/")) continue;
      nameInZip = nameInZip.substring("web/".length());
      zip.filePromiseArrayBuffer(nameInZip, promiseCreator.create((resolve, reject) -> {
        fileManager.getFileContents(fd.getPath(), contents -> {
          resolve.accept(contents);
        });
      }));
    }
    
    // Main Plom code
    try {
      zip.filePromiseString("main.plom.js", getModuleAsJsonPString(false, null));
          // Insert BOM at the beginning to label it as UTF-8 
//          .thenNow(str -> "\ufeff" + str));
          // Actually, the BOM is not appreciated by some browsers when served from some servers
    } catch (IOException e) {
      Browser.getWindow().getConsole().log(e);
    }
    
    WebHelpers.JSZipGenerateAsyncOptions zipOptions = (WebHelpers.JSZipGenerateAsyncOptions)JsPropertyMap.of();
    if (exportAsBase64)
      zipOptions.setTypeBase64();
    else
      zipOptions.setTypeBlob();
    zipOptions.setCompressionDeflate();
    return zip.generateAsync(zipOptions);
  }
  
  /**
   * Call this to adjust the layout/dimensions of things after a resize
   */
  public void updateAfterResize()
  {
    if (codePanel != null)
      codePanel.updateAfterResize();
  }
  
  /** Can be called from outside if there is a change to the extra files
   * of a project (e.g. the OS file system detects that new files were added etc.). 
   */
  public void updateExtraFiles()
  {
    repository.refreshExtraFiles(() -> {
      if (globalsPanel != null)
        globalsPanel.rebuildFileList();
    });
  }

  /**
   * Some versions of Plom have an on-screen back button, and they need to
   * listen for changes in the breadcrumbs in case they need to hide it
   */
  public void setBackBreadcrumbListener(BackBreadcrumbListener listener)
  {
    backBreadcrumbListener = listener; 
  }
  
  @JsFunction public static interface BackBreadcrumbListener
  {
    public void fireNotification();
  }
  BackBreadcrumbListener backBreadcrumbListener;
  
  /** Call this to go back up the breadcrumb chain */ 
  Runnable backBreadcrumb = null;
  
  public void goBack()
  {
    if (backBreadcrumb != null)
      backBreadcrumb.run();
  }
  
  public boolean canGoBack()
  {
    return backBreadcrumb != null;
  }
  
  private void setBackBreadcrumb(Runnable backAction)
  {
    backBreadcrumb = backAction;
    if (backBreadcrumbListener != null)
      backBreadcrumbListener.fireNotification();
  }
  
  
  /**
   * Basic UI for changing the class/method/function
   */
  public void hookSubject()
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
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
    setBackBreadcrumb(null);
  }
  
  void fillBreadcrumbForFunction(Element breadcrumbEl, FunctionDescription sig)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    setBackBreadcrumb(this::loadGlobalsView);
    Document doc = Browser.getDocument();
    
    breadcrumbEl.appendChild(doc.createTextNode(" "));
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
    setBackBreadcrumb(this::loadGlobalsView);
    Document doc = Browser.getDocument();
    
    breadcrumbEl.appendChild(doc.createTextNode(" "));
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
    
    breadcrumbEl.appendChild(doc.createTextNode(" "));
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent("@" + cls.getName());
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadClassView(cls, false);
    }, false);
    breadcrumbEl.appendChild(a);
    setBackBreadcrumb(() -> loadClassView(cls, false));
    
    breadcrumbEl.appendChild(doc.createTextNode(" "));
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
  
  private Element getBreadcrumbEl()
  {
    Element subjectEl = Browser.getDocument().querySelector(".subject");
    Element breadcrumbEl = 
        subjectEl != null ? subjectEl.querySelector(".breadcrumb")
            : Browser.getDocument().querySelector(".breadcrumb");
    return breadcrumbEl;
  }
  
  public void loadFunctionCodeView(String fnName)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, repository.getFunctionDescription(fnName));
    
    closeCodePanelIfOpen();
    currentFunctionBeingViewed = fnName;
    showCodePanel(repository.getFunctionDescription(fnName).code);
  }

  void loadFunctionSignatureView(FunctionDescription sig, boolean isNew)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForFunction(breadcrumbEl, sig);
    
    closeCodePanelIfOpen();
    showFunctionPanel(sig, isNew);
  }

  void loadClassView(ClassDescription cls, boolean isNew)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForClass(breadcrumbEl, cls);
    
    closeCodePanelIfOpen();
    showClassPanel(cls, isNew);
  }

  void loadMethodCodeView(ClassDescription cls, FunctionDescription m)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForMethod(breadcrumbEl, cls, m);
    
    closeCodePanelIfOpen();
    currentMethodClassBeingViewed = cls;
    currentMethodBeingViewed = m;
    showCodePanel(m.code);
  }

  void loadMethodSignatureView(ClassDescription cls, FunctionDescription m, boolean isNew)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForMethod(breadcrumbEl, cls, m);
    
    closeCodePanelIfOpen();
    showMethodPanel(cls, m, isNew);
  }

  public void loadGlobalsView()
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
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
    if (globalsPanel != null)
    {
      globalsPanel.close();
      globalsPanel = null;
    }
  }
  
  private void closeCodePanelIfOpen()
  {
    saveCodeToRepository();
    closeCodePanelWithoutSavingIfOpen();
  }
  
  private void showCodePanel(StatementContainer code)
  {
    codePanel = CodePanel.forFullScreen(getMainDiv(), true);
    if (repository.isNoStdLibFlag)
      // Normally, the "primitive" keyword isn't available unless we're editing a standard library
      codePanel.setExcludeTokens(Collections.emptyList());
    codePanel.setVariableContextConfigurator(
        (scope, coreTypes) -> {
          StandardLibrary.createGlobals(null, scope, coreTypes);
          scope.setParent(new RepositoryScope(repository, coreTypes, null));
        },
        (context) -> {
          if (currentFunctionBeingViewed == null && currentMethodBeingViewed == null)
            return;

          if (currentMethodClassBeingViewed != null)
          {
            if (currentMethodBeingViewed != null)
            {
              context.setIsStaticMethod(currentMethodBeingViewed.sig.isStatic);
              context.setIsConstructorMethod(currentMethodBeingViewed.sig.isConstructor);
            }
            try {
              context.setDefinedClassOfMethod(context.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(currentMethodClassBeingViewed.getName())));
            }
            catch (RunException e)
            {
              // Ignore any errors when setting this
            }
            
            // Create an object scope that will handle this and instance variables
            try {
              Value thisValue = new Value();
              thisValue.type = context.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(currentMethodClassBeingViewed.getName()));
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
            for (int n = 0; n < fd.sig.getNumArgs(); n++)
            {
              String name = fd.sig.getArgName(n);
              UnboundType unboundType = fd.sig.getArgType(n);
              try {
                Type type = context.currentScope().typeFromUnboundTypeFromScope(unboundType);
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
        if (lineEl != null)
        {
          lineEl.getStyle().clearDisplay();
          lineEl.setTextContent("L" + lineNo);
        }
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
      if (isFinal)
        loadFunctionCodeView(newSig.getLookupName());
    });
  }

  private void showMethodPanel(ClassDescription cls, FunctionDescription m, boolean isNew)
  {
    methodPanel = new MethodPanel(getMainDiv(), repository, m.sig, isNew);
    methodPanel.setListener((newSig, isFinal) -> {
      m.sig = newSig;
      cls.updateMethod(m);
      if (isFinal)
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
  
  public static String getAutoResizingInputHtmlText() {
    return UIResources.INSTANCE.getAutoResizingInputHtml().getText();
  }


}
