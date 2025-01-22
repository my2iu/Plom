package org.programmingbasics.plom.core.codestore;

import java.io.IOException;

import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;

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

  @JsType(isNative = true)
  public static interface GetFromNameMessage extends RequestMessage
  {
    @JsProperty(name = "name") String getName();
    @JsProperty(name = "name") void setName(String name);
  }

  public static GetFromNameMessage createGetFromNameMessage(MessageType type, String id, String name)
  {
    GetFromNameMessage msg = (GetFromNameMessage)createRequestMessage(type, id);
    msg.setName(name);
    return msg;
  }

  @JsType(isNative = true)
  public static interface FunctionDescriptionReplyMessage extends ReplyMessage
  {
    @JsProperty(name = "signature") String getSignature();
    @JsProperty(name = "signature") void setSignature(String signature);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default void setFunctionDescription(FunctionDescription fd)
    {
      try {
        if (fd == null) return;
        StringBuilder strBuilder = new StringBuilder();
        PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
        ModuleCodeRepository.saveFunctionSignature(out, fd.sig);
        setSignature(strBuilder.toString());
        
        strBuilder = new StringBuilder();
        out = new PlomCodeOutputFormatter(strBuilder);
        PlomTextWriter.writeStatementContainer(out, fd.code);
        setCode(strBuilder.toString());
      } 
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    @JsOverlay default FunctionDescription getFunctionDescription() throws PlomReadException
    {
      if (getSignature() == null) return null;
      
      PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(getSignature());
      PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
      FunctionSignature sig = ModuleCodeRepository.loadFunctionSignature(lexer);

      in = new PlomTextReader.StringTextReader(getCode());
      lexer = new PlomTextReader.PlomTextScanner(in);
      StatementContainer code = PlomTextReader.readStatementContainer(lexer);
      return new FunctionDescription(sig, code);
    }
  }

  public static FunctionDescriptionReplyMessage createFunctionDescriptionReplyMessage(String replyId, FunctionDescription fd)
  {
    FunctionDescriptionReplyMessage msg = (FunctionDescriptionReplyMessage)createReplyMessage(MessageType.REPLY, replyId);
    msg.setFunctionDescription(fd);
    return msg;
  }

  public static enum MessageType
  {
    REPLY("reply"), 
    IMPORT_STDLIB("importStdLib"), 
    LOAD_MODULE("loadModule"),
    GET_FUNCTION_DESCRIPTION("functionDescription");
    private MessageType(String val)
    {
      this.value = val;
    }
    String value;
  }
}
