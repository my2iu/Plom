package org.programmingbasics.plom.core.languageserver;

import org.programmingbasics.plom.core.Main;
import org.programmingbasics.plom.core.WebHelpers;
import org.programmingbasics.plom.core.WebHelpersShunt;
import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.MessageType;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;

import elemental.client.Browser;
import elemental.events.MessageEvent;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * Language server that runs in a web worker that allows Plom to 
 * do incremental compiles or code completion calculations or other 
 * tasks without blocking the UI thread 
 */

@JsType
public class LanguageServerWorker
{
  public LanguageServerWorker()
  {
    repo = new ModuleCodeRepository();
  }
  
  ModuleCodeRepository repo;
  
  public void start()
  {
    Browser.getWindow().addEventListener("message", (evt) -> {
      evt.preventDefault();
      handleMessage((MessageEvent)evt);
    });
  }
  
  void handleMessage(MessageEvent mevt)
  {
    CodeRepositoryMessages.BaseMessage msg = (CodeRepositoryMessages.BaseMessage)mevt.getData();
    switch (msg.getTypeEnum()) 
    {
    case IMPORT_STDLIB:
      ModuleCodeRepository subRepository = new ModuleCodeRepository();
      subRepository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
      try {
        PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(Main.getStdLibCodeText());
        PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
        subRepository.loadModulePlain(lexer, null);
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      subRepository.markAsImported();
      repo.setChainedRepository(subRepository);
      break;
    case LOAD_MODULE:
    {
      CodeRepositoryMessages.LoadModuleMessage loadModuleMsg = (CodeRepositoryMessages.LoadModuleMessage)msg;
      PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(loadModuleMsg.getCode());
      PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
      
//      // Loading extra files from a module is done asynchronously, so
//      // we use promises to keep track of when loading is done.
//      ArrayOf<WebHelpers.Promise<String>> extraFilesPromises = Collections.arrayOf();

      // Load the module, but pass in an extra handler to extract out file information
      try {
        repo.loadModulePlain(lexer, (lex) -> {
          String peek = lex.peekLexInput();
          if ("file".equals(peek)) {
            // Ignore extra files, but it's okay if we encounter them
            return true;
          }
          return false;
        });
        postMessage(CodeRepositoryMessages.createStatusReplyMessage(loadModuleMsg.getRequestId(), true, null));
      } 
      catch (PlomReadException e)
      {
        postMessage(CodeRepositoryMessages.createStatusReplyMessage(loadModuleMsg.getRequestId(), false, e.getMessage()));
        e.printStackTrace();
      }
      break;
    }
    case GET_FUNCTION_DESCRIPTION:
    {
      CodeRepositoryMessages.GetFromNameMessage nameMsg = (CodeRepositoryMessages.GetFromNameMessage)msg; 
      FunctionDescription fd = repo.getFunctionDescription(nameMsg.getName());
      postMessage(CodeRepositoryMessages.createFunctionDescriptionReplyMessage(nameMsg.getRequestId(), fd));
      break;
    }
    case IS_STDLIB:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg; 
      postMessage(CodeRepositoryMessages.createIsStdLibReplyMessage(requestMsg.getRequestId(), repo.isNoStdLibFlag));
      break;
    }
    case SAVE_FUNCTION_CODE:
    {
      CodeRepositoryMessages.SaveFunctionCodeMessage requestMsg = (CodeRepositoryMessages.SaveFunctionCodeMessage)msg;
      try {
        repo.getFunctionDescription(requestMsg.getName()).code = requestMsg.getCodeStatementContainer();
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    default:
      Browser.getWindow().getConsole().log("Language server received unknown message type " + msg.getType());
      break;
    }
    
  }
  
  /** The version of postMessage() that's specifically available in web workers */
  @JsFunction
  static interface WorkerPostMessage
  {
    void postMessage(Object msg);
  }
  WorkerPostMessage postMessageFunction = Js.cast(Js.global().get("postMessage")); 
  
  void postMessage(Object msg)
  {
    postMessageFunction.postMessage(msg);
  }
}
