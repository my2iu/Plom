package org.programmingbasics.plom.core.codestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.interpreter.UnboundType;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Message formats for sending requests to the language server
 * and the underlying ModuleCodeRepository
 */
public class CodeRepositoryMessages
{
  public static JsonObject createEmptyObject() { return Json.createObject(); }
  
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

  public static String statementContainerToString(StatementContainer code) throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    PlomTextWriter.writeStatementContainer(out, code);
    return strBuilder.toString();
  }
  
  public static StatementContainer stringToStatementContainer(String code) throws PlomReadException
  {
    if (code == null) return null;
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(code);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    return PlomTextReader.readStatementContainer(lexer);
  }

  private static String tokenContainerToString(TokenContainer code) throws IOException
  {
    if (code == null) return null;
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    PlomTextWriter.writeTokenContainer(out, code);
    return strBuilder.toString();
  }
  
  private static TokenContainer stringToTokenContainer(String code) throws PlomReadException
  {
    if (code == null) return null;
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(code);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    return PlomTextReader.readTokenContainer(lexer);
  }

  private static String tokenToString(Token code) throws IOException
  {
    if (code == null) return null;
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    PlomTextWriter.writeToken(out, code);
    return strBuilder.toString();
  }
  
  private static Token stringToToken(String code) throws PlomReadException
  {
    if (code == null) return null;
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(code);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    return PlomTextReader.readToken(lexer);
  }

  private static String signatureToString(FunctionSignature sig) throws IOException
  {
    StringBuilder strBuilder = new StringBuilder();
    PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);
    ModuleCodeRepository.saveFunctionSignature(out, sig);
    return strBuilder.toString();
  }
  
  public static FunctionSignature stringToSignature(String str) throws PlomReadException
  {
    if (str == null) return null;
    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(str);
    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);
    return ModuleCodeRepository.loadFunctionSignature(lexer);
  }
  
  @JsType(isNative = true)
  public static interface IsStdLibReplyMessage extends ReplyMessage
  {
    @JsProperty(name = "stdlib") boolean isStdLib();
    @JsProperty(name = "stdlib") void setStdLib(boolean stdlib);
  }

  public static IsStdLibReplyMessage createIsStdLibReplyMessage(String replyId, boolean stdlib)
  {
    IsStdLibReplyMessage msg = (IsStdLibReplyMessage)createReplyMessage(MessageType.REPLY, replyId);
    msg.setStdLib(stdlib);
    return msg;
  }

  @JsType(isNative = true)
  public static interface SaveFunctionCodeMessage extends RequestMessage
  {
    @JsProperty(name = "name") String getName();
    @JsProperty(name = "name") void setName(String name);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default StatementContainer getCodeStatementContainer() throws PlomReadException { return stringToStatementContainer(getCode()); }
    @JsOverlay default void setCodeStatementContainer(StatementContainer code) throws IOException { setCode(statementContainerToString(code)); }
  }

  public static SaveFunctionCodeMessage createSaveFunctionCodeMessage(String id, String name, StatementContainer code) throws IOException
  {
    SaveFunctionCodeMessage msg = (SaveFunctionCodeMessage)createRequestMessage(MessageType.SAVE_FUNCTION_CODE, id);
    msg.setName(name);
    msg.setCodeStatementContainer(code);
    return msg;
  }

  @JsType(isNative = true)
  public static interface SingleObjectReplyMessage<T> extends ReplyMessage
  {
    @JsProperty(name = "payload") T getPayload();
    @JsProperty(name = "payload") void setPayload(T data);
  }
  
  public static <U> SingleObjectReplyMessage<U> createSingleObjectReplyMessage(String replyId, U obj)
  {
    SingleObjectReplyMessage<U> msg = (SingleObjectReplyMessage<U>)createReplyMessage(MessageType.REPLY, replyId);
    msg.setPayload(obj);
    return msg;
  }

  @JsType(isNative = true)
  public static interface UnboundTypeJson
  {
    @JsProperty(name = "mainToken") String getMainToken();
    @JsProperty(name = "mainToken") void setMainToken(String mainToken);
    @JsProperty(name = "returnType") String getReturnType();
    @JsProperty(name = "returnType") void setReturnType(String returnType);
    @JsOverlay default UnboundType getAsUnboundType() throws PlomReadException
    {
      if (getMainToken() == null) return null;
      TokenContainer returnType = stringToTokenContainer(getReturnType());
      Token mainToken = stringToToken(getMainToken());
      UnboundType type = new UnboundType();
      type.mainToken = (Token.ParameterToken) mainToken;
      type.returnType = returnType;
      return type;
    }
    @JsOverlay default void setAsUnboundType(UnboundType type) throws IOException
    {
      if (type == null) return;
      setMainToken(tokenToString(type.mainToken));
      setReturnType(tokenContainerToString(type.returnType));
    }
  }

  @JsType(isNative = true)
  public static interface FunctionDescriptionJson
  {
    @JsProperty(name = "signature") String getSignature();
    @JsProperty(name = "signature") void setSignature(String signature);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsProperty(name = "imported") boolean isImported();
    @JsProperty(name = "imported") void setImported(boolean imported);
    @JsOverlay default void setAsFunctionDescription(FunctionDescription fd)
    {
      try {
        if (fd == null) return;
        setSignature(signatureToString(fd.sig));
        setImported(fd.isImported);
        setCode(statementContainerToString(fd.code));
      } 
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    @JsOverlay default FunctionDescription getAsFunctionDescription() throws PlomReadException
    {
      if (getSignature() == null) return null;

      FunctionSignature sig = stringToSignature(getSignature());
      StatementContainer code = stringToStatementContainer(getCode());
      FunctionDescription fd = new FunctionDescription(sig, code);
      fd.setImported(isImported());
      return fd;
    }
  }

  @JsType(isNative = true)
  public static interface ClassDescriptionJson
  {
    @JsProperty(name = "name") String getName();
    @JsProperty(name = "name") void setName(String name);
    @JsProperty(name = "originalName") String getOriginalName();
    @JsProperty(name = "originalName") void setOriginalName(String name);
    @JsProperty(name = "parent") UnboundTypeJson getParent();
    @JsProperty(name = "parent") void setParent(UnboundTypeJson unboundType);
    @JsProperty(name = "methods") ArrayOf<FunctionDescriptionJson> getMethods();
    @JsProperty(name = "methods") void setMethods(ArrayOf<FunctionDescriptionJson> methods);
    @JsProperty(name = "varDecl") String getVariableDeclarationCode();
    @JsProperty(name = "varDecl") void setVariableDeclarationCode(String code);
    @JsProperty(name = "builtIn") boolean isBuiltIn();
    @JsProperty(name = "builtIn") void setBuiltIn(boolean builtIn);
    @JsProperty(name = "imported") boolean isImported();
    @JsProperty(name = "imported") void setImported(boolean isImported);
    
    @JsOverlay default ClassDescription getAsClassDescription() throws PlomReadException
    {
      ClassDescription cl = new ClassDescription(getName(), getOriginalName());
      cl.parent = getParent().getAsUnboundType();
      cl.methods = arrayOfToList(getMethods(), (FunctionDescriptionJson json) -> {
        return json.getAsFunctionDescription();
      });
      cl.setVariableDeclarationCode(stringToStatementContainer(getVariableDeclarationCode()));
      cl.setBuiltIn(isBuiltIn());
      cl.setImported(isImported());
      return cl;
    }
    @JsOverlay default void setAsClassDescription(ClassDescription cl) throws IOException
    {
      setName(cl.getName());
      setOriginalName(cl.getOriginalName());
      UnboundTypeJson parent = (UnboundTypeJson)createEmptyObject();
      parent.setAsUnboundType(cl.parent);
      setParent(parent);
      setMethods(listToArrayOf(cl.methods, (FunctionDescription fn) -> {
        FunctionDescriptionJson json = (FunctionDescriptionJson)createEmptyObject();
        json.setAsFunctionDescription(fn);
        return json;
      }));
      setVariableDeclarationCode(statementContainerToString(cl.getVariableDeclarationCode()));
      setBuiltIn(cl.isBuiltIn);
      setImported(cl.isImported);
    }
  }

  @FunctionalInterface
  public static interface TransformFunction<I, O, E extends Throwable>
  {
    O transform(I in) throws E;
  }
  
  public static <I, O, E extends Throwable> ArrayOf<O> listToArrayOf(List<I> list, TransformFunction<I, O, E> fn) throws E
  {
    ArrayOf<O> arr = Collections.arrayOf();
    for (I obj: list)
    {
      O encoded = fn.transform(obj);
      arr.push(encoded);
    }
    return arr;
  }

  public static <I, O, E extends Throwable> List<O> arrayOfToList(ArrayOf<I> arr, TransformFunction<I, O, E> fn) throws E
  {
    List<O> list = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++)
    {
      I obj = arr.get(i);
      O encoded = fn.transform(obj);
      list.add(encoded);
    }
    return list;
  }

  @JsType(isNative = true)
  public static interface SetVariableDeclarationCodeMessage extends RequestMessage
  {
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default StatementContainer getCodeStatementContainer() throws PlomReadException { return stringToStatementContainer(getCode()); }
    @JsOverlay default void setCodeStatementContainer(StatementContainer code) throws IOException { setCode(statementContainerToString(code)); }
  }

  public static SetVariableDeclarationCodeMessage createSetVariableDeclarationCodeMessage(String id, StatementContainer code) throws IOException
  {
    SetVariableDeclarationCodeMessage msg = (SetVariableDeclarationCodeMessage)createRequestMessage(MessageType.SET_VARDECL_CODE, id);
    msg.setCodeStatementContainer(code);
    return msg;
  }

  @JsType(isNative = true)
  public static interface SaveModuleToStringMessage extends RequestMessage
  {
    @JsProperty(name = "saveClasses") boolean isSaveClasses();
    @JsProperty(name = "saveClasses") void setSaveClasses(boolean saveClasses);
  }

  public static SaveModuleToStringMessage createSaveModuleToStringMessage(String id, boolean saveClasses)
  {
    SaveModuleToStringMessage msg = (SaveModuleToStringMessage)createRequestMessage(MessageType.SAVE_MODULE_TO_STRING, id);
    msg.setSaveClasses(saveClasses);
    return msg;
  }

  @JsType(isNative = true)
  public static interface MakeNewUniqueMethodMessage extends RequestMessage
  {
    @JsProperty(name = "class") String getClassName();
    @JsProperty(name = "class") void setClassName(String className);
    @JsProperty(name = "static") boolean isStatic();
    @JsProperty(name = "static") void setStatic(boolean isStatic);
    @JsProperty(name = "constructor") boolean isConstructor();
    @JsProperty(name = "constructor") void setConstructor(boolean isConstructor);
  }

  public static MakeNewUniqueMethodMessage createMakeNewUniqueMethodMessage(String id, String className, boolean isStatic, boolean isConstructor)
  {
    MakeNewUniqueMethodMessage msg = (MakeNewUniqueMethodMessage)createRequestMessage(MessageType.MAKE_NEW_EMPTY_METHOD, id);
    msg.setClassName(className);
    msg.setConstructor(isConstructor);
    msg.setStatic(isStatic);
    return msg;
  }

  @JsType(isNative = true)
  public static interface ChangeFunctionSignatureRequest extends RequestMessage
  {
    @JsProperty(name = "oldSig") String getOldSignature();
    @JsProperty(name = "oldSig") void setOldSignature(String sig);
    @JsProperty(name = "newSig") String getNewSignature();
    @JsProperty(name = "newSig") void setNewSignature(String sig);
  }

  public static ChangeFunctionSignatureRequest createChangeFunctionSignatureRequest(String id, FunctionSignature newSig, FunctionSignature oldSig) throws IOException
  {
    ChangeFunctionSignatureRequest msg = (ChangeFunctionSignatureRequest)createRequestMessage(MessageType.CHANGE_FUNCTION_SIGNATURE, id);
    msg.setOldSignature(signatureToString(oldSig));
    msg.setNewSignature(signatureToString(newSig));
    return msg;
  }

  public static enum MessageType
  {
    REPLY("reply"), 
    IMPORT_STDLIB("importStdLib"), 
    LOAD_MODULE("loadModule"),
    GET_FUNCTION_DESCRIPTION("functionDescription"),
    GET_CLASS_DESCRIPTION("classDescription"),
    IS_STDLIB("isStdLib"),
    SAVE_FUNCTION_CODE("saveFunctionCode"),
    GET_ALL_CLASSES_SORTED("getAllClassesSorted"),
    GET_ALL_FUNCTIONS_SORTED("getAllFunctionsSorted"),
    GET_VARDECL_CODE("getVarDeclCode"),
    GET_IMPORTED_VARDECL_CODE("getImportedVarDeclCode"),
    SET_VARDECL_CODE("setVarDeclCode"), 
    MAKE_NEW_EMPTY_FUNCTION("makeNewEmptyFunction"),
    MAKE_NEW_EMPTY_CLASS("makeNewEmptyClass"),
    MAKE_NEW_EMPTY_METHOD("makeNewEmptyMethod"),
    SAVE_MODULE_TO_STRING("saveModuleToString"),
    CHANGE_FUNCTION_SIGNATURE("changeFunctionSignature"),
    DELETE_FUNCTION("deleteFunction"),
    DELETE_CLASS("deleteClass");
    private MessageType(String val)
    {
      this.value = val;
    }
    String value;
  }
}
