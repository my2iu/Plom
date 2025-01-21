package org.programmingbasics.plom.core.codestore;

import elemental.json.Json;
import elemental.json.JsonObject;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Message formats for sending requests to the language server
 * and the underlying ModuleCodeRepository
 */
public class CodeRepositoryMessages
{
  public static BaseMessage createBaseMessage(MessageType type)
  {
    JsonObject obj = Json.createObject();
    CodeRepositoryMessages.BaseMessage msg = (CodeRepositoryMessages.BaseMessage)obj;
    msg.setTypeEnum(type);
    return msg;
  }

  @JsType(isNative = true)
  public static interface BaseMessage
  {
    @JsProperty(name = "type") String getType();
    @JsProperty(name = "type") void setType(String type);
    @JsOverlay default MessageType getTypeEnum()
    {
      for (MessageType type: MessageType.values())
      {
        if (type.value.equals(getType()))
          return type;
      }
      throw new IllegalArgumentException("Unknown message type " + getType());
    }
    @JsOverlay default void setTypeEnum(MessageType type)
    {
      setType(type.value);
    }
  }
  
  @JsType(isNative = true)
  public static interface RequestMessage extends BaseMessage
  {
    @JsProperty(name = "requestId") String getRequestId();
    @JsProperty(name = "requestId") void setRequestId(String id);
  }

  public static RequestMessage createRequestMessage(MessageType type, String id)
  {
    RequestMessage msg = (RequestMessage)createBaseMessage(type);
    msg.setRequestId(id);
    return msg;
  }

  @JsType(isNative = true)
  public static interface ReplyMessage extends BaseMessage
  {
    @JsProperty(name = "replyId") String getReplyId();
    @JsProperty(name = "replyId") void setReplyId(String id);
  }

  public static ReplyMessage createReplyMessage(MessageType type, String id)
  {
    ReplyMessage msg = (ReplyMessage)createBaseMessage(type);
    msg.setReplyId(id);
    return msg;
  }

  @JsType(isNative = true)
  public static interface StatusReplyMessage extends ReplyMessage
  {
    @JsProperty(name = "ok") boolean isOk();
    @JsProperty(name = "ok") void setOk(boolean ok);
    @JsProperty(name = "errorMessage") String getErrorMessage();
    @JsProperty(name = "errorMessage") void setErrorMessage(String error);
  }

  public static StatusReplyMessage createStatusReplyMessage(String replyId, boolean ok, String errorMessage)
  {
    StatusReplyMessage msg = (StatusReplyMessage)createReplyMessage(MessageType.REPLY, replyId);
    msg.setOk(ok);
    msg.setErrorMessage(errorMessage);
    return msg;
  }

  @JsType(isNative = true)
  public static interface LoadModuleMessage extends RequestMessage
  {
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
  }
  
  public static LoadModuleMessage createLoadModuleMessage(String id, String code)
  {
    LoadModuleMessage msg = (LoadModuleMessage)createRequestMessage(MessageType.LOAD_MODULE, id);
    msg.setCode(code);
    return msg;
  }

  public static enum MessageType
  {
    REPLY("reply"), IMPORT_STDLIB("importStdLib"), LOAD_MODULE("loadModule");
    private MessageType(String val)
    {
      this.value = val;
    }
    String value;
  }
}
