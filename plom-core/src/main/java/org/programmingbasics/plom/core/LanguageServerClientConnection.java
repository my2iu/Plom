package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.Consumer;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
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
    worker.addEventListener("message", (evt) -> {
      evt.preventDefault();
      MessageEvent mevt = (MessageEvent)evt;
      handleWorkerMessage(mevt);
    }, false);
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

  public Promise<CodeRepositoryMessages.ReplyMessage> sendLoadModule(String codeStr)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createLoadModuleMessage(requestId, codeStr));
    return waitForReplyFor(requestId);
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
  
  public Promise<Boolean> sendIsStdLib()
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createRequestMessage(MessageType.IS_STDLIB, requestId));
    return waitForReplyFor(requestId).thenNow((msg) -> {
      CodeRepositoryMessages.IsStdLibReplyMessage stdLibMsg = (CodeRepositoryMessages.IsStdLibReplyMessage)msg;
      return stdLibMsg.isStdLib();
    });
  }

  public Promise<Void> sendSaveFunctionCode(String name, StatementContainer code)
  {
    String requestId = getNextId();
    CodeRepositoryMessages.SaveFunctionCodeMessage msg;
    try
    {
      msg = CodeRepositoryMessages.createSaveFunctionCodeMessage(requestId, name, code);
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

  public Promise<String> sendSaveModuleToString(boolean saveClasses)
  {
    String requestId = getNextId();
    worker.postMessage(CodeRepositoryMessages.createSaveModuleToStringMessage(requestId, saveClasses));
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
      FunctionSignature oldSig)
  {
    String requestId = getNextId();
    try {
      worker.postMessage(CodeRepositoryMessages.createChangeFunctionSignatureRequest(requestId, newSig, oldSig));
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

}
