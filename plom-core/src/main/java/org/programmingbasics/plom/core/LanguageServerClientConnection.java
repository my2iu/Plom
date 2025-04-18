package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.Consumer;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.ClassDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.FunctionDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.MessageType;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.ReplyMessage;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;

import elemental.events.MessageEvent;
import elemental.html.Worker;
import elemental.util.ArrayOf;
import elemental.util.MapFromStringToString;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class LanguageServerClientConnection
{
  /**
   * WebWorker where the language server is running
   */
  Worker worker;
  public LanguageServerClientConnection(Worker worker)
  {
    this.worker = worker;
    if (worker != null)
    {
      worker.addEventListener("message", (evt) -> {
        evt.preventDefault();
        MessageEvent mevt = (MessageEvent)evt;
        handleWorkerMessage(mevt);
      }, false);
    }
  }

  // For testing
  protected LanguageServerClientConnection() {
    this(null);
  }
  
  private void handleWorkerMessage(MessageEvent mevt)
  {
    CodeRepositoryMessages.BaseMessage msg = (CodeRepositoryMessages.BaseMessage)mevt.getData();
    if (msg.getTypeEnum() == MessageType.REPLY)
    {
      ReplyMessage reply = (ReplyMessage)msg;
      Consumer<ReplyMessage> replyHandler = waitingForReplies.remove(reply.getReplyId());
      replyHandler.accept(reply);
    }
  }

  /** Sometimes we send messages to the language server and expect 
   * a reply. We use a unique message id to route the replies back
   * to the original sender of the message.
   */
  private double nextMessageId = 0;
  private String getNextId()
  {
    double id = nextMessageId;
    nextMessageId++;
    return "" + id;
  }
  
  /**
   * Map of requests that we've sent to the language server worker
   * that are waiting for replies.
   */
  private Map<String, Consumer<CodeRepositoryMessages.ReplyMessage>> waitingForReplies = new HashMap<>();
  private Promise<CodeRepositoryMessages.ReplyMessage> waitForReplyFor(String requestId)
  {
    Promise<CodeRepositoryMessages.ReplyMessage> promise = WebHelpersShunt.<CodeRepositoryMessages.ReplyMessage>newPromise((resolve, reject) -> {
      waitingForReplies.put(requestId, resolve);
    });
    return promise;
  }
  
  public void sendImportStdLibRepository()
  {
    CodeRepositoryMessages.BaseMessage msg = CodeRepositoryMessages.createBaseMessage(MessageType.IMPORT_STDLIB);
    worker.postMessage(msg);
  }

  public Promise<MapFromStringToString> sendLoadModule(String codeStr)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createLoadModuleMessage(requestId, codeStr));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.LoadModuleReply loadReply = (CodeRepositoryMessages.LoadModuleReply)reply;
      if (!loadReply.isOk())
        throw new IllegalArgumentException(loadReply.getErrorMessage());
      return loadReply.getFiles();
    });
  }
  
  public Promise<Void> sendLoadClass(String code)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createLoadClassMessage(requestId, code));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.StatusReplyMessage statusReply = (CodeRepositoryMessages.StatusReplyMessage)reply;
      if (!statusReply.isOk())
        throw new RuntimeException(statusReply.getErrorMessage());
      return null;
    });
  }


  public Promise<FunctionDescription> sendGetFunctionDescription(String name)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGetFromNameMessage(MessageType.GET_FUNCTION_DESCRIPTION, requestId, name));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try
      {
        CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson> fdMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson>)msg;
        return fdMsg.getPayload().getAsFunctionDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<FunctionDescription> sendGetFunctionDescriptionFromId(int id)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGetFromIdMessage(MessageType.GET_FUNCTION_DESCRIPTION_FROM_ID, requestId, id));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try
      {
        CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson> fdMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson>)msg;
        return fdMsg.getPayload().getAsFunctionDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<Boolean> sendIsStdLib()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.IS_STDLIB, requestId));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      CodeRepositoryMessages.IsStdLibReplyMessage stdLibMsg = (CodeRepositoryMessages.IsStdLibReplyMessage)msg;
      return stdLibMsg.isStdLib();
    });
  }

  public Promise<Void> sendSaveFunctionCode(int functionId, StatementContainer code)
  {
    String requestId = getNextId();
    CodeRepositoryMessages.SaveFunctionCodeMessage msg;
    try
    {
      msg = CodeRepositoryMessages.createSaveFunctionCodeMessage(requestId, functionId, code);
      worker.postMessage(msg);
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return WebHelpersShunt.promiseResolve(null);
    }
    return waitForReplyFor(requestId).thenNow((reply) -> {
      return null;
    });
  }

  public Promise<List<ClassDescription>> sendGetAllClassesSorted()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_ALL_CLASSES_SORTED, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>>)reply;
        return CodeRepositoryMessages.arrayOfToList(objReply.getPayload(), (json) -> {
          return json.getAsClassDescription();
        });
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new ArrayList<>();
    });
  }

  public Promise<List<ClassDescription>> sendGetDeletedClasses()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_DELETED_CLASSES, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>>)reply;
        return CodeRepositoryMessages.arrayOfToList(objReply.getPayload(), (json) -> {
          return json.getAsClassDescription();
        });
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new ArrayList<>();
    });
  }

  public Promise<List<ClassDescription>> sendGetModuleClasses()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_MODULE_CLASSES, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<ClassDescriptionJson>>)reply;
        return CodeRepositoryMessages.arrayOfToList(objReply.getPayload(), (json) -> {
          return json.getAsClassDescription();
        });
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new ArrayList<>();
    });
  }

  public Promise<List<FunctionDescription>> sendGetAllFunctionsSorted()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_ALL_FUNCTIONS_SORTED, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<FunctionDescriptionJson>> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<ArrayOf<FunctionDescriptionJson>>)reply;
        return CodeRepositoryMessages.arrayOfToList(objReply.getPayload(), (json) -> {
          return json.getAsFunctionDescription();
        });
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new ArrayList<>();
    });
  }

  public Promise<StatementContainer> sendGetVariableDeclarationCode()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_VARDECL_CODE, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<String> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<String>)reply;
        return CodeRepositoryMessages.stringToStatementContainer(objReply.getPayload());
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new StatementContainer();
    });
  }

  public Promise<StatementContainer> sendGetImportedVariableDeclarationCode()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.GET_IMPORTED_VARDECL_CODE, requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<String> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<String>)reply;
        return CodeRepositoryMessages.stringToStatementContainer(objReply.getPayload());
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      return new StatementContainer();
    });
  }

  public Promise<Void> sendSetVariableDeclarationCode(StatementContainer code)
  {
    String requestId = getNextId();
    try
    {
      worker.postMessage(CodeRepositoryMessages.createSetVariableDeclarationCodeMessage(requestId, code));
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return WebHelpersShunt.promiseResolve(null);
    }
    return waitForReplyFor(requestId).thenNow((reply) -> {
      return null;
    });
  }

  public Promise<FunctionDescription> sendMakeNewEmptyFunction()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.MAKE_NEW_EMPTY_FUNCTION, requestId));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson> fdMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson>)msg;
        return fdMsg.getPayload().getAsFunctionDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<ClassDescription> sendMakeNewEmptyClass()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.MAKE_NEW_EMPTY_CLASS, requestId));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ClassDescriptionJson> clMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<ClassDescriptionJson>)msg;
        return clMsg.getPayload().getAsClassDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<FunctionDescription> sendMakeNewEmptyMethod(ClassDescription cls, boolean isStatic, boolean isConstructor)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createMakeNewUniqueMethodMessage(requestId, cls.getName(), isStatic, isConstructor));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson> fdMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<FunctionDescriptionJson>)msg;
        return fdMsg.getPayload().getAsFunctionDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<String> sendSaveModuleToString(boolean saveClasses, boolean isOpen)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createSaveModuleToStringMessage(requestId, saveClasses, isOpen));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.SingleObjectReplyMessage<String> objReply = (CodeRepositoryMessages.SingleObjectReplyMessage<String>)reply;
      return objReply.getPayload();
    });
  }

  public Promise<ClassDescription> sendFindClass(String name)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGetFromNameMessage(MessageType.GET_CLASS_DESCRIPTION, requestId, name));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      try {
        CodeRepositoryMessages.SingleObjectReplyMessage<ClassDescriptionJson> clMsg = (CodeRepositoryMessages.SingleObjectReplyMessage<ClassDescriptionJson>)msg;
        return clMsg.getPayload().getAsClassDescription();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

  public Promise<Void> sendChangeFunctionSignature(FunctionSignature newSig,
      int functionId)
  {
    String requestId = getNextId();
    try {
      worker.postMessage(CodeRepositoryMessages.createChangeFunctionSignatureRequest(requestId, newSig, functionId));
      return waitForReplyFor(requestId).thenNow((msg) -> {
        CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
        return null;
      });
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  public Promise<Void> sendChangeMethodSignature(ClassDescription cls, FunctionSignature newSig,
      int functionId)
  {
    String requestId = getNextId();
    try {
      worker.postMessage(CodeRepositoryMessages.createChangeMethodSignatureRequest(requestId, cls.getName(), newSig, functionId));
      return waitForReplyFor(requestId).thenNow((msg) -> {
        CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
        return null;
      });
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  public Promise<Void> sendDeleteFunction(FunctionSignature sig)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGetFromNameMessage(MessageType.DELETE_FUNCTION, requestId, sig.getLookupName()));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
      return null;
    });
  }

  public Promise<Void> sendDeleteClass(ClassDescription cls)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGetFromNameMessage(MessageType.DELETE_CLASS, requestId, cls.getName()));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
      return null;
    });
  }

  public Promise<Void> sendUpdateClassBaseInfo(String className,
      ClassDescription cl)
  {
    String requestId = getNextId();
    try {
      worker.postMessage(CodeRepositoryMessages.createUpdateClassBaseInfoRequest(requestId, className, cl));
      return waitForReplyFor(requestId).thenNow((msg) -> {
        CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
        return null;
      });
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  public Promise<Void> sendDeleteClassMethod(ClassDescription cls,
      FunctionDescription fn)
  {
    String requestId = getNextId();
    try {
      worker.postMessage(CodeRepositoryMessages.createDeleteClassMethodRequest(requestId, cls, fn));
      return waitForReplyFor(requestId).thenNow((msg) -> {
        CodeRepositoryMessages.ReplyMessage replyMsg = (CodeRepositoryMessages.ReplyMessage)msg;
        return null;
      });
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  public Promise<Void> sendSaveMethodCode(ClassDescription cls,
      int methodId, StatementContainer code)
  {
    String requestId = getNextId();
    CodeRepositoryMessages.SaveMethodCodeMessage msg;
    try
    {
      msg = CodeRepositoryMessages.createSaveMethodCodeMessage(requestId, cls.getName(), methodId, code);
      worker.postMessage(msg);
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return WebHelpersShunt.promiseResolve(null);
    }
    return waitForReplyFor(requestId).thenNow((reply) -> {
      return null;
    });
  }
  
  Promise<Void> sendSetCodeCompletionContext(Integer currentFunction, String currentClass, FunctionSignature currentMethod, StatementContainer currentCode, CodePosition currentPos)
  {
    String requestId = getNextId();
    try
    {
      worker.postMessage(CodeRepositoryMessages.createSetCodeCompletionRequest(requestId, currentFunction, currentClass, currentMethod, currentCode, currentPos));
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return WebHelpersShunt.promiseResolve(null);
    }
    return waitForReplyFor(requestId).thenNow((reply) -> {
      return null;
    });
  }

  Promise<List<String>> sendGatherTypeSuggestions(String val, boolean allowVoid)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGatherTypeSuggestionsRequest(requestId, val, allowVoid));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.GatherSuggestionsReply suggestionsReply = (CodeRepositoryMessages.GatherSuggestionsReply)reply;
      if (suggestionsReply.isCancelled())
        return null;
      return suggestionsReply.getSuggestionsList();
    });
  }
  
  Promise<List<String>> sendGatherVariableSuggestions(String val)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGatherVariableSuggestionsRequest(requestId, val));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.GatherSuggestionsReply suggestionsReply = (CodeRepositoryMessages.GatherSuggestionsReply)reply;
      if (suggestionsReply.isCancelled())
        return null;
      return suggestionsReply.getSuggestionsList();
    });
  }

  Promise<List<String>> sendGatherMemberSuggestions(String val)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGatherMemberSuggestionsRequest(requestId, val));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.GatherSuggestionsReply suggestionsReply = (CodeRepositoryMessages.GatherSuggestionsReply)reply;
      if (suggestionsReply.isCancelled())
        return null;
      return suggestionsReply.getSuggestionsList();
    });
  }

  Promise<List<String>> sendGatherStaticMemberSuggestions(String val, boolean includeNonConstructors, boolean includeConstructors)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGatherStaticMemberSuggestionsRequest(requestId, val, includeNonConstructors, includeConstructors));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.GatherSuggestionsReply suggestionsReply = (CodeRepositoryMessages.GatherSuggestionsReply)reply;
      if (suggestionsReply.isCancelled())
        return null;
      return suggestionsReply.getSuggestionsList();
    });
  }

  public Promise<List<Token>> sendGatherExpectedTypeTokens()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createGatherExpectedTypeTokensRequest(requestId));
    return waitForReplyFor(requestId).thenNow((reply) -> {
      CodeRepositoryMessages.GatherExpectedTypeTokensReply tokensReply = (CodeRepositoryMessages.GatherExpectedTypeTokensReply)reply;
      if (tokensReply.isCancelled())
        return null;
      try {
        return tokensReply.getExpectedTypeTokens();
      }
      catch (PlomReadException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
  }

}
