package org.programmingbasics.plom.core.codestore;

import elemental.json.Json;
import elemental.json.JsonObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Message formats for sending requests to the language server
 * and the underlying ModuleCodeRepository
 */
public class CodeRepositoryMessages
{
  public static BaseMessage createBaseMessage(String type)
  {
    JsonObject obj = Json.createObject();
    CodeRepositoryMessages.BaseMessage msg = (CodeRepositoryMessages.BaseMessage)obj;
    msg.setType(type);
    return msg;
  }
  
  public static LoadModuleMessage createLoadModuleMessage(String code)
  {
    LoadModuleMessage msg = (LoadModuleMessage)createBaseMessage("loadModule");
    msg.setCode(code);
    return msg;
  }
  
  @JsType(isNative = true)
  public static interface BaseMessage
  {
    @JsProperty(name = "type") String getType();
    @JsProperty(name = "type") void setType(String type);
  }
  
  @JsType(isNative = true)
  public static interface LoadModuleMessage
  {
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
  }
}
