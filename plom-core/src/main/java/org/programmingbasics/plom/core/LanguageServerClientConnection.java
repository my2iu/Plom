package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.Consumer;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.MessageType;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.ReplyMessage;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;

import elemental.events.MessageEvent;
import elemental.html.Worker;
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
        CodeRepositoryMessages.FunctionDescriptionReplyMessage fdMsg = (CodeRepositoryMessages.FunctionDescriptionReplyMessage)msg;
        return fdMsg.getFunctionDescription();
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
}
