package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.ast.LineNumberTracker;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FileDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.LogLevel;
import org.programmingbasics.plom.core.view.LineForPosition;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.html.AnchorElement;
import elemental.html.ArrayBuffer;
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
  
  private CodeRepositoryClient repository;
  
  CodeWidgetBase codePanel;
  MethodPanel methodPanel;
  ClassPanel classPanel;
  TextEditorPanel textEditorPanel;
  GlobalsPanel globalsPanel;
  LineNumberTracker lineNumbers = new LineNumberTracker();
//  String currentFunctionBeingViewed = null;
  Integer currentFunctionIdBeingViewed = null;
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
  
  /**
   * An id string that identifies which instance of the debugger execution
   * this is 
   */
  static String debuggerExecutionId;

  public static void configureDebuggerEnvironmentAt(String ideParentWindowOrigin, String debuggerExecutionId)
  {
    // I'm having problems writing these static variables from JS
    // (the values written don't seem to be picked up the Java side),
    // so I'm using a static method to set them.
    debuggerEnvironmentAvailableFlag = true;
    debuggerIdeParentWindowOrigin = ideParentWindowOrigin;
    Main.debuggerExecutionId = debuggerExecutionId;
  }

  public static boolean getDebuggerEnvironmentAvailableFlag() { return debuggerEnvironmentAvailableFlag; }
  
  /** The File System Access API uses async iterators, and GWT doesn't
   * work well with async iterators, so to work with them, we need some
   * external JS code to be injected in
   */
  static WebHelpers.AsyncIteratorCollector asyncIteratorToArray;
  public static void setAsyncIteratorToArray(WebHelpers.AsyncIteratorCollector fn) { asyncIteratorToArray = fn; }
  
  public static ErrorLogger createErrorLoggerForConsole()
  {
    return new ErrorLogger() {
      @Override public void error(Object err, ProgramCodeLocation location)
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
      @Override public void error(Object err, ProgramCodeLocation location)
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
    return new DebuggerEnvironment.ServiceWorkerDebuggerEnvironment(debuggerIdeParentWindowOrigin, debuggerExecutionId);
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
        getRepository().getFunctionDescription(loc.getFunctionMethodName())
          .<Void>thenNow((FunctionDescription sig) -> {
            if (sig != null)
            {
              onLocationJump.onJump();
              loadFunctionSignatureView(sig, false);
            }
            return null;
          });
      }
      else if (loc.getClassName() == null && loc.getFunctionMethodName() != null && loc.getPosition() != null)
      {
        onLocationJump.onJump();
        loadFunctionCodeView(loc.getFunctionMethodName()).<Void>thenNow((_void) -> {
          codePanel.setCursorPosition(loc.getPosition());
          return null;
        });
      }
      else if (loc.getClassName() != null && loc.getFunctionMethodName() == null)
      {
        getRepository().findClassWithName(loc.getClassName())
          .thenNow((cls) -> {
            if (cls != null)
            {
              onLocationJump.onJump();
              loadClassView(cls, false);
            }
            return null;
          });
      }
      else if (loc.getClassName() != null && loc.getFunctionMethodName() != null)
      {
        getRepository().findClassWithName(loc.getClassName())
          .thenNow((cls) -> {
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
            return null;
          });
      }
    });
    return debugConnection;
  }
  
  public static String base64Encode(ArrayBuffer buf)
  {
    return WebHelpers.Base64EncoderDecoder.encodeToString(Browser.getWindow().newUint8Array(buf, 0, buf.getByteLength()), false);
  }
  
  public CodeRepositoryClient getRepository()
  {
    return repository;
  }

  public void setRepository(CodeRepositoryClient repository)
  {
    this.repository = repository;
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
                            newRepo.loadModule(code);
                          }
                          else
                          {
                            newRepo.loadClassStringIntoModule(code);
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
//            if (newRepo.isNoStdLibFlag())
//              newRepo.setChainedRepository(null);
            setRepository(newRepo);

            getRepository().refreshExtraFiles(() -> {
                closeCodePanelWithoutSavingIfOpen();  // Usually code panel will save over just loaded code when you switch view

                loadGlobalsView();
            });
            return null;
        });
  }

  /** Allows you to set special helper functions for fast-animating the software keyboard on Android */
  public void setAndroidKeyboardAnimator(KeyboardAnimator show, KeyboardAnimator hide)
  {
    showKeyboard = show;
    hideKeyboard = hide;
  }
  @JsFunction
  public static interface KeyboardAnimator
  {
    void animate();
  }
  static KeyboardAnimator showKeyboard, hideKeyboard;
  
  static void hookFastAndroidKeyboard(Element el)
  {
    el.addEventListener(Event.FOCUS, (e) -> {
      if (Main.showKeyboard != null)
        Main.showKeyboard.animate();
    }, false);
    el.addEventListener(Event.BLUR, (e) -> {
      if (Main.hideKeyboard == null)
        return;

      // See if we're switching focus to another input element or textarea,
      // meaning we don't have to hide the keyboard just to reshow it again
      Element newFocus = (Element)Js.asPropertyMap(e).get("relatedTarget");
      if ("INPUT".equals(newFocus.getTagName())
          || "TEXTAREA".equals(newFocus.getTagName())
          || "PLOM-AUTORESIZING-INPUT".equals(newFocus.getTagName()))
      {
        // Don't do anything. Let the keyboard stay visible
      }
      else
      {
        Main.hideKeyboard.animate();
      }
    }, false);
  }

  /**
   * Mobile Safari sometimes gives focus to an element, but it
   * doesn't deploy the soft keyboard. Here, I'll try different
   * ways to give focus to an element
   */
  public static void forceFocusAndShowKeyboard(Element e, boolean withSelect)
  {
    e.focus();  // Documentation is unclear as to whether select() also sets focus or not
    if (withSelect)
      ((InputElement)e).select();
    // It turns out that Mobile Safari won't let you control focus
    // unless it happens as a result of a user interaction, but some
    // of the UI updates are done in promises, so they don't qualify
    // for programmatically taking focus
//    if (Browser.getWindow().getNavigator().getVendor().contains("Apple Computer"))
//    {
//      // Not sure what to do here
//    }
  }
  
  
  /** If any code is currently being displayed in the code panel, save it
   * to the repository
   */
  public void saveCodeToRepository()
  {
    if (codePanel != null) 
    {
      if (currentFunctionIdBeingViewed != null)
      {
        getRepository().saveFunctionCode(currentFunctionIdBeingViewed, codePanel.codeList);
      }
      if (currentMethodBeingViewed != null)
     {
        getRepository().saveMethodCode(currentMethodClassBeingViewed, currentMethodBeingViewed, codePanel.codeList);
      }
    }
    if (textEditorPanel != null)
    {
      textEditorPanel.save();
    }
  }
  
  public Promise<String> getModuleAsString() throws IOException 
  {
    return getRepository().saveModuleToString(true);
  }
  
  /** Packages up code in a single .js file that's suitable for running in a web browser */
  public WebHelpers.Promise<String> getModuleAsJsonPString(boolean withDebugEnvironment, String debuggerTargetOriginUrl, String executionId) throws IOException
  {
    // Get module contents as a string
    return getRepository().saveModuleToString(true)
      .<String>thenNow((code) -> {
        // Wrap string in js
        return "plomEngineLoad = plomEngineLoad.then((repository) => {\n"
            + (withDebugEnvironment ? "org.programmingbasics.plom.core.Main.configureDebuggerEnvironmentAt('" + debuggerTargetOriginUrl + "', '" + executionId + "');\n" : "")
            + "var code = `" + PlomTextWriter.escapeTemplateLiteral(code) + "`;\n"
            + "var skipPromise = loadCodeStringIntoExecutableRepository(code, repository);\n"
            + "return repository;\n"
            + "});\n";
      });
  }

  public WebHelpers.Promise<String> getModuleWithFilesAsString() throws IOException 
  {
    StringBuilder out = new StringBuilder();

    return getRepository().saveModuleWithExtraFiles(new PlomTextWriter.PlomCodeOutputFormatter(out), true,
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
    Promise<Void> moduleSaved = getRepository().saveModuleToString(false)
        .thenNow((code) -> {
          moduleSaver.saveModule(code);
          return null;
        })
        .then((unused) -> getRepository().getDeletedClasses())
        .thenNow((deletedClasses) -> {
          // Delete any classes that no longer exist
          for (ClassDescription cls: deletedClasses)
          {
            if (cls.getOriginalName() == null) continue;
            classDeleter.deleteClass(cls.getOriginalName());
          }
          return null;
        })
        .then((unused) -> getRepository().getClasses())
        .thenNow((classes) -> {
          for (ClassDescription cls: classes)
          {
            if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
            {
              if (cls.getOriginalName() == null) continue;
              if (cls.getOriginalName().equals(cls.getName())) continue;
              classDeleter.deleteClass(cls.getOriginalName());
            }
          }
          for (ClassDescription cls: classes)
          {
            if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
            {
              if (cls.getOriginalName() == null) continue;
              if (cls.getOriginalName().equals(cls.getName())) continue;
              classDeleter.deleteClass(cls.getOriginalName());
            }
          }
          return null;
        })
        .then((unused) -> getRepository().getClasses())
        .thenNow((classes) -> {
          // Save all classes
          try {
            for (ClassDescription cls: classes)
            {
              if (!cls.isBuiltIn || cls.hasNonBuiltInMethods())
              {
                StringBuilder out = new StringBuilder();
                ModuleCodeRepository.saveClass(new PlomTextWriter.PlomCodeOutputFormatter(out), cls);
                classSaver.saveClass(cls.getName(), out.toString());
              }
            }
          } catch (IOException e) {
            // Ignore errors
            e.printStackTrace();
          }
          return null;
        });

    return moduleSaved;
  }
  
  /**
   * Bundles up all code and related files into a single zip that can
   * be uploaded as a website and run
   */
  public WebHelpers.Promise<Object> exportAsZip(WebHelpers.JSZip zip, String plomSystemFilePrefix, boolean exportAsBase64)
  {
    // Check if there's an index.html defined in the extra files, if not, create one
    List<FileDescription> extraFiles = getRepository().getAllExtraFilesSorted();
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
    ExtraFilesManager fileManager = getRepository().getExtraFilesManager();
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
      zip.filePromiseString("main.plom.js", getModuleAsJsonPString(false, null, null));
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
    getRepository().refreshExtraFiles(() -> {
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

  void fillBreadcrumbForExtraFiles(Element breadcrumbEl, String fileName)
  {
    fillBreadcrumbForGlobals(breadcrumbEl);
    setBackBreadcrumb(this::loadGlobalsView);
    Document doc = Browser.getDocument();
    
    breadcrumbEl.appendChild(doc.createTextNode(" "));
    AnchorElement a = (AnchorElement)doc.createElement("a");
    a.setClassName("breadcrumb-item");
    a.setTextContent(fileName);
    a.setHref("#");
    a.addEventListener(Event.CLICK, (e) -> {
      e.preventDefault();
      loadTextEditorView(fileName, "this is incomplete");
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
  
  public Promise<Void> loadFunctionCodeView(String name)
  {
    return getRepository().getFunctionDescription(name)
        .<Void>then((fd) -> {
          return loadFunctionCodeViewFromFunctionId(fd.getId());
        });
  }
  
  public Promise<Void> loadFunctionCodeViewFromFunctionId(int fnId)
  {
    return getRepository().getFunctionDescriptionWithId(fnId).<Void>thenNow((fd) -> {
      Element breadcrumbEl = getBreadcrumbEl(); 
      breadcrumbEl.setInnerHTML("");
      fillBreadcrumbForFunction(breadcrumbEl, fd);
      
      closeCodePanelIfOpen();
      currentFunctionIdBeingViewed = fnId;
      showCodePanel(fd.code);
      return null;
    });
    
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
    getRepository().reloadClass(cls)
      .thenNow((refreshedClass) -> {
        showClassPanel(refreshedClass, isNew);
        return null;
      });
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

  public void loadTextEditorView(String fileName, String fileContents)
  {
    Element breadcrumbEl = getBreadcrumbEl(); 
    breadcrumbEl.setInnerHTML("");
    fillBreadcrumbForExtraFiles(breadcrumbEl, fileName);
    
//    closeCodePanelIfOpen();
//    showGlobalsPanel();
    showTextEditorPanel(fileName, fileContents);
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
    currentFunctionIdBeingViewed = null;
    currentMethodBeingViewed = null;
    currentMethodClassBeingViewed = null;
    codePanel = null;
    if (globalsPanel != null)
    {
      globalsPanel.close();
      globalsPanel = null;
    }
    if (textEditorPanel != null)
    {
      textEditorPanel.close();
      textEditorPanel = null;
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
    if (getRepository().isNoStdLibFlag())
      // Normally, the "primitive" keyword isn't available unless we're editing a standard library
      codePanel.setExcludeTokens(Collections.emptyList());
    codePanel.setVariableContextConfigurator(
        repository.makeCodeCompletionSuggesterWithContext(currentFunctionIdBeingViewed, currentMethodClassBeingViewed, (currentMethodBeingViewed == null ? null : currentMethodBeingViewed.sig)));
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
    
    globalsPanel = new GlobalsPanel(mainDiv, getRepository(),
        (FunctionDescription fnName) -> Main.this.loadFunctionCodeViewFromFunctionId(fnName.getId()),
        (FunctionDescription sig, boolean isNew) -> loadFunctionSignatureView(sig, isNew),
        (ClassDescription cls, boolean isNew) -> loadClassView(cls, isNew),
        (String fileName, String fileContents) -> loadTextEditorView(fileName, fileContents)
        );
  }
  
  private void showFunctionPanel(FunctionDescription sig, boolean isNew)
  {
    methodPanel = new MethodPanel(getMainDiv(), getRepository(), sig.sig, isNew);
    methodPanel.setListener((newSig, isFinal) -> {
      getRepository().changeFunctionSignature(newSig, sig);
      sig.sig = FunctionSignature.copyOf(newSig);
      if (isFinal)
        loadFunctionCodeViewFromFunctionId(sig.getId());
    });
  }

  private void showMethodPanel(ClassDescription cls, FunctionDescription m, boolean isNew)
  {
    methodPanel = new MethodPanel(getMainDiv(), getRepository(), m.sig, isNew);
    methodPanel.setListener((newSig, isFinal) -> {
      getRepository().changeMethodSignature(cls, newSig, m)
        .thenNow((unused) -> {
          m.sig = FunctionSignature.copyOf(newSig);
          if (isFinal)
            loadMethodCodeView(cls, m);
          return null;
        });
    });
  }

  private void showClassPanel(ClassDescription cls, boolean isNew)
  {
    classPanel = new ClassPanel(getMainDiv(), getRepository(), cls, 
        (ClassDescription c, FunctionDescription method) -> loadMethodCodeView(c, method),
        (ClassDescription c, FunctionDescription method, boolean isNewMethod) -> loadMethodSignatureView(c, method, isNewMethod),
        () -> loadGlobalsView(),
        isNew);
  }

  private void showTextEditorPanel(String fileName, String fileContents)
  {
    textEditorPanel = new TextEditorPanel(getMainDiv(), getRepository(), 
        () -> loadGlobalsView(),
        fileName,
        fileContents);
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
