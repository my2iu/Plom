package org.programmingbasics.plom.core.languageserver;

import java.io.IOException;
import java.util.List;

import org.programmingbasics.plom.core.CodeRepositoryClient;
import org.programmingbasics.plom.core.Main;
import org.programmingbasics.plom.core.WebHelpers;
import org.programmingbasics.plom.core.WebHelpersShunt;
import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.ClassDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.FunctionDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.MessageType;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.UnboundType;

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
      FunctionDescriptionJson json = (FunctionDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      json.setAsFunctionDescription(fd);
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(nameMsg.getRequestId(), json));
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
    case GET_ALL_CLASSES_SORTED:
    {
      try {
        CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
        List<ClassDescription> classes = repo.getAllClassesSorted();
        ArrayOf<ClassDescriptionJson> json = CodeRepositoryMessages.listToArrayOf(classes, (cl) -> {
          ClassDescriptionJson clJson = (ClassDescriptionJson)CodeRepositoryMessages.createEmptyObject();
          clJson.setAsClassDescription(cl);
          return clJson;
        });
        postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), json));
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      break;
    }
    case GET_ALL_FUNCTIONS_SORTED:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      List<FunctionDescription> fns = repo.getAllFunctionSorted();
      ArrayOf<FunctionDescriptionJson> json = CodeRepositoryMessages.listToArrayOf(fns, (fd) -> {
        FunctionDescriptionJson fdJson = (FunctionDescriptionJson)CodeRepositoryMessages.createEmptyObject();
        fdJson.setAsFunctionDescription(fd);
        return fdJson;
      });
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), json));
      break;
    }
    case GET_VARDECL_CODE:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      String code = null;
      try
      {
        code = CodeRepositoryMessages.statementContainerToString(repo.getVariableDeclarationCode());
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), code));
      break;
    }
    case GET_IMPORTED_VARDECL_CODE:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      String code = null;
      try
      {
        code = CodeRepositoryMessages.statementContainerToString(repo.getImportedVariableDeclarationCode());
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), code));
      break;
    }
    case SET_VARDECL_CODE:
    {
      CodeRepositoryMessages.SetVariableDeclarationCodeMessage requestMsg = (CodeRepositoryMessages.SetVariableDeclarationCodeMessage)msg;
      try {
        repo.setVariableDeclarationCode(requestMsg.getCodeStatementContainer());
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case MAKE_NEW_EMPTY_FUNCTION:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      // Find a unique function name
      String newFunctionName = ModuleCodeRepository.findUniqueName("function", (name) -> repo.getFunctionWithName(name) == null);
      FunctionDescription func = new FunctionDescription(
          FunctionSignature.from(UnboundType.forClassLookupName("void"), newFunctionName),
          new StatementContainer());
      repo.addFunctionAndResetIds(func);
      FunctionDescriptionJson fdJson = (FunctionDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      fdJson.setAsFunctionDescription(func);
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), fdJson));
      break;
    }
    case MAKE_NEW_EMPTY_CLASS:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      // Find a unique class name
      String newClassName = ModuleCodeRepository.findUniqueName("class", (name) -> !repo.hasClassWithName(name));
      ClassDescription c = repo.addClassAndResetIds(newClassName);

      ClassDescriptionJson clJson = (ClassDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      try {
      clJson.setAsClassDescription(c);
      } 
      catch (IOException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), clJson));
      break;
    }
    case MAKE_NEW_EMPTY_METHOD:
    {
      CodeRepositoryMessages.MakeNewUniqueMethodMessage requestMsg = (CodeRepositoryMessages.MakeNewUniqueMethodMessage)msg;

      ClassDescription cls = repo.findClassWithName(requestMsg.getClassName(), true);
      if (cls == null)
      {
        // TODO: Just create the underlying class maybe?
        break;
      }
      String newMethodName = ModuleCodeRepository.findUniqueName("method", (name) -> !cls.hasMethodWithName(name));
      FunctionSignature sig = FunctionSignature.from(UnboundType.forClassLookupName("void"), newMethodName);
      if (requestMsg.isStatic()) sig.setIsStatic(true);
      if (requestMsg.isConstructor()) sig.setIsConstructor(true);
      FunctionDescription func = new FunctionDescription(
          sig,
          new StatementContainer());
      cls.addMethod(func);

      FunctionDescriptionJson fdJson = (FunctionDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      fdJson.setAsFunctionDescription(func);
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), fdJson));
      break;
    }
    case SAVE_MODULE_TO_STRING:
    {
      CodeRepositoryMessages.SaveModuleToStringMessage requestMsg = (CodeRepositoryMessages.SaveModuleToStringMessage)msg;
      StringBuilder out = new StringBuilder();
      try {
        repo.saveModule(new PlomTextWriter.PlomCodeOutputFormatter(out), requestMsg.isSaveClasses());
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(requestMsg.getRequestId(), out.toString()));
      break;
    }
    case GET_CLASS_DESCRIPTION:
    {
      CodeRepositoryMessages.GetFromNameMessage nameMsg = (CodeRepositoryMessages.GetFromNameMessage)msg; 
      ClassDescription cls = repo.findClassWithName(nameMsg.getName(), true);

      ClassDescriptionJson clJson = (ClassDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      try {
        clJson.setAsClassDescription(cls);
      } 
      catch (IOException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(nameMsg.getRequestId(), clJson));
      break;
    }
    case CHANGE_FUNCTION_SIGNATURE:
    {
      CodeRepositoryMessages.ChangeFunctionSignatureRequest requestMsg = (CodeRepositoryMessages.ChangeFunctionSignatureRequest)msg;
      try {
        FunctionSignature oldSig = CodeRepositoryMessages.stringToSignature(requestMsg.getOldSignature());
        FunctionSignature newSig = CodeRepositoryMessages.stringToSignature(requestMsg.getNewSignature());
        repo.getFunctionDescription(oldSig.getLookupName()).sig = newSig;
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case DELETE_FUNCTION:
    {
      CodeRepositoryMessages.GetFromNameMessage nameMsg = (CodeRepositoryMessages.GetFromNameMessage)msg; 
      FunctionDescription fd = repo.getFunctionDescription(nameMsg.getName());
      repo.deleteFunctionAndResetIds(fd.module, fd.id);
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, nameMsg.getRequestId()));
      break;
    }
    case DELETE_CLASS:
    {
      CodeRepositoryMessages.GetFromNameMessage nameMsg = (CodeRepositoryMessages.GetFromNameMessage)msg; 
      ClassDescription cls = repo.findClassWithName(nameMsg.getName(), false);
      if (cls != null)
        repo.deleteClassAndResetIds(cls.module, cls.id);
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, nameMsg.getRequestId()));
      break;
    }
    case UPDATE_CLASS_BASE_INFO:
    {
      CodeRepositoryMessages.UpdateClassBaseInfoRequest requestMsg = (CodeRepositoryMessages.UpdateClassBaseInfoRequest)msg;
      try {
        String className = requestMsg.getLookupName();
        ClassDescription newBaseInfo = requestMsg.getClassDescription().getAsClassDescription();
        ClassDescription cls = repo.findClassWithName(className, false);
        if (cls != null)
        {
          cls.updateBaseInfoFrom(newBaseInfo);
        }
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case DELETE_CLASS_METHOD:
    {
      CodeRepositoryMessages.DeleteClassMethodRequest requestMsg = (CodeRepositoryMessages.DeleteClassMethodRequest)msg;
      try {
        ClassDescription cls = repo.findClassWithName(requestMsg.getClassName(), false);
        FunctionSignature sig = CodeRepositoryMessages.stringToSignature(requestMsg.getMethodSignature());
        FunctionDescription fd = cls.findMethod(sig.getLookupName(), sig.isConstructor || sig.isStatic);
        cls.deleteMethodAndResetIds(fd.id);
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case CHANGE_METHOD_SIGNATURE:
    {
      CodeRepositoryMessages.ChangeMethodSignatureRequest requestMsg = (CodeRepositoryMessages.ChangeMethodSignatureRequest)msg;
      try {
        ClassDescription cls = repo.findClassWithName(requestMsg.getClassName(), false);
        FunctionSignature oldSig = CodeRepositoryMessages.stringToSignature(requestMsg.getOldSignature());
        FunctionSignature newSig = CodeRepositoryMessages.stringToSignature(requestMsg.getNewSignature());
        FunctionDescription fd = cls.findMethod(oldSig.getLookupName(), oldSig.isConstructor || oldSig.isStatic);
        fd.sig = newSig;
        cls.updateMethod(fd);
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case SAVE_METHOD_CODE:
    {
      CodeRepositoryMessages.SaveMethodCodeMessage requestMsg = (CodeRepositoryMessages.SaveMethodCodeMessage)msg;
      try {
        ClassDescription cls = repo.findClassWithName(requestMsg.getClassName(), false);
        FunctionSignature sig = CodeRepositoryMessages.stringToSignature(requestMsg.getSignature());
        FunctionDescription fd = cls.findMethod(sig.getLookupName(), sig.isConstructor || sig.isStatic);
        fd.code = requestMsg.getCodeStatementContainer();
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
