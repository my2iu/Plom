package org.programmingbasics.plom.core.codestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.ast.CodePosition;
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
import elemental.util.MapFromStringToString;
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
  public static interface CancellableReplyMessage extends ReplyMessage
  {
    @JsProperty(name = "cancelled") boolean isCancelled();
    @JsProperty(name = "cancelled") void setCancelled(boolean cancelled);
  }

  public static CancellableReplyMessage createCancellableReplyMessage(String id, boolean cancelled)
  {
    CancellableReplyMessage msg = (CancellableReplyMessage)createReplyMessage(MessageType.REPLY, id);
    msg.setCancelled(cancelled);
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

  public static LoadModuleMessage createLoadClassMessage(String id, String code)
  {
    LoadModuleMessage msg = (LoadModuleMessage)createRequestMessage(MessageType.LOAD_CLASS, id);
    msg.setCode(code);
    return msg;
  }

  @JsType(isNative = true)
  public static interface LoadModuleReply extends StatusReplyMessage
  {
    @JsProperty(name = "files") MapFromStringToString getFiles();
    @JsProperty(name = "files") void setFiles(MapFromStringToString files);
  }
  
  public static LoadModuleReply createLoadModuleReply(String replyId, boolean ok, String errorMessage, MapFromStringToString files)
  {
    LoadModuleReply msg = (LoadModuleReply)createStatusReplyMessage(replyId, ok, errorMessage);
    if (files != null)
      msg.setFiles(files);
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
  public static interface GetFromIdMessage extends RequestMessage
  {
    @JsProperty(name = "id") int getId();
    @JsProperty(name = "id") void setId(int id);
  }

  public static GetFromIdMessage createGetFromIdMessage(MessageType type, String requestId, int id)
  {
    GetFromIdMessage msg = (GetFromIdMessage)createRequestMessage(type, requestId);
    msg.setId(id);
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
//    @JsProperty(name = "name") String getName();
//    @JsProperty(name = "name") void setName(String name);
    @JsProperty(name = "functionId") int getFunctionId();
    @JsProperty(name = "functionId") void setFunctionId(int id);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default StatementContainer getCodeStatementContainer() throws PlomReadException { return stringToStatementContainer(getCode()); }
    @JsOverlay default void setCodeStatementContainer(StatementContainer code) throws IOException { setCode(statementContainerToString(code)); }
  }

  public static SaveFunctionCodeMessage createSaveFunctionCodeMessage(String id, int functionId, StatementContainer code) throws IOException
  {
    SaveFunctionCodeMessage msg = (SaveFunctionCodeMessage)createRequestMessage(MessageType.SAVE_FUNCTION_CODE, id);
    msg.setFunctionId(functionId);
    msg.setCodeStatementContainer(code);
    return msg;
  }

  @JsType(isNative = true)
  public static interface SaveMethodCodeMessage extends RequestMessage
  {
    @JsProperty(name = "class") String getClassName();
    @JsProperty(name = "class") void setClassName(String className);
    @JsProperty(name = "methodId") Integer getMethodId();
    @JsProperty(name = "methodId") void setMethodId(Integer id);
//    @JsProperty(name = "sig") String getSignature();
//    @JsProperty(name = "sig") void setSignature(String sig);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default StatementContainer getCodeStatementContainer() throws PlomReadException { return stringToStatementContainer(getCode()); }
    @JsOverlay default void setCodeStatementContainer(StatementContainer code) throws IOException { setCode(statementContainerToString(code)); }
  }

  public static SaveMethodCodeMessage createSaveMethodCodeMessage(String id, String className, int methodId, StatementContainer code) throws IOException
  {
    SaveMethodCodeMessage msg = (SaveMethodCodeMessage)createRequestMessage(MessageType.SAVE_METHOD_CODE, id);
    msg.setClassName(className);
    msg.setMethodId(methodId);
//    msg.setSignature(signatureToString(sig));
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
    @JsProperty(name = "functionId") int getId();
    @JsProperty(name = "functionId") void setId(int id);
    @JsOverlay default void setAsFunctionDescription(FunctionDescription fd)
    {
      try {
        if (fd == null) return;
        setSignature(signatureToString(fd.sig));
        setImported(fd.isImported);
        setCode(statementContainerToString(fd.code));
        setId(fd.getId());
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
      FunctionDescription fd = FunctionDescription.withForcedId(sig, code, getId());
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
    @JsProperty(name = "classId") int getId();
    @JsProperty(name = "classId") void setId(int id);
    
    @JsOverlay default ClassDescription getAsClassDescription() throws PlomReadException
    {
      ClassDescription cl = ClassDescription.withForcedId(getName(), getOriginalName(), getId());
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
      setId(cl.getId());
    }
    @JsOverlay default void setAsClassDescriptionWithoutMethods(ClassDescription cl) throws IOException
    {
      setName(cl.getName());
      setOriginalName(cl.getOriginalName());
      UnboundTypeJson parent = (UnboundTypeJson)createEmptyObject();
      parent.setAsUnboundType(cl.parent);
      setParent(parent);
      setMethods(Collections.arrayOf());
      setVariableDeclarationCode(statementContainerToString(cl.getVariableDeclarationCode()));
      setBuiltIn(cl.isBuiltIn);
      setImported(cl.isImported);
      setId(cl.getId());
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
    @JsProperty(name = "open") boolean isOpen();
    @JsProperty(name = "open") void setOpen(boolean open);
  }

  public static SaveModuleToStringMessage createSaveModuleToStringMessage(String id, boolean saveClasses, boolean isOpen)
  {
    SaveModuleToStringMessage msg = (SaveModuleToStringMessage)createRequestMessage(MessageType.SAVE_MODULE_TO_STRING, id);
    msg.setSaveClasses(saveClasses);
    msg.setOpen(isOpen);
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
    @JsProperty(name = "functionId") int getFunctionId();
    @JsProperty(name = "functionId") void setFunctionId(int id);
    @JsProperty(name = "newSig") String getNewSignature();
    @JsProperty(name = "newSig") void setNewSignature(String sig);
  }

  public static ChangeFunctionSignatureRequest createChangeFunctionSignatureRequest(String id, FunctionSignature newSig, int functionId) throws IOException
  {
    ChangeFunctionSignatureRequest msg = (ChangeFunctionSignatureRequest)createRequestMessage(MessageType.CHANGE_FUNCTION_SIGNATURE, id);
    msg.setFunctionId(functionId);
    msg.setNewSignature(signatureToString(newSig));
    return msg;
  }

  @JsType(isNative = true)
  public static interface ChangeMethodSignatureRequest extends ChangeFunctionSignatureRequest
  {
    @JsProperty(name = "class") String getClassName();
    @JsProperty(name = "class") void setClassName(String className);
  }

  public static ChangeFunctionSignatureRequest createChangeMethodSignatureRequest(String id, String className, FunctionSignature newSig, int functionId) throws IOException
  {
    ChangeMethodSignatureRequest msg = (ChangeMethodSignatureRequest)createChangeFunctionSignatureRequest(id, newSig, functionId);
    msg.setTypeEnum(MessageType.CHANGE_METHOD_SIGNATURE);
    msg.setClassName(className);
    return msg;
  }

  @JsType(isNative = true)
  public static interface UpdateClassBaseInfoRequest extends RequestMessage
  {
    @JsProperty(name = "name") String getLookupName();
    @JsProperty(name = "name") void setLookupName(String name);
    @JsProperty(name = "class") ClassDescriptionJson getClassDescription();
    @JsProperty(name = "class") void setClassDescription(ClassDescriptionJson cl);
  }

  public static UpdateClassBaseInfoRequest createUpdateClassBaseInfoRequest(String id, String name, ClassDescription cl) throws IOException
  {
    UpdateClassBaseInfoRequest msg = (UpdateClassBaseInfoRequest)createRequestMessage(MessageType.UPDATE_CLASS_BASE_INFO, id);
    msg.setLookupName(name);
    ClassDescriptionJson json = (ClassDescriptionJson)createEmptyObject();
    json.setAsClassDescriptionWithoutMethods(cl);
    msg.setClassDescription(json);
    return msg;
  }

  @JsType(isNative = true)
  public static interface DeleteClassMethodRequest extends RequestMessage
  {
    @JsProperty(name = "class") String getClassName();
    @JsProperty(name = "class") void setClassName(String name);
    @JsProperty(name = "method") String getMethodSignature();
    @JsProperty(name = "method") void setMethodSignature(String signature);
  }

  public static DeleteClassMethodRequest createDeleteClassMethodRequest(String id, ClassDescription cl, FunctionDescription fn) throws IOException
  {
    DeleteClassMethodRequest msg = (DeleteClassMethodRequest)createRequestMessage(MessageType.DELETE_CLASS_METHOD, id);
    msg.setClassName(cl.getName());
    msg.setMethodSignature(signatureToString(fn.sig));
    return msg;
  }

  @JsType(isNative = true)
  public static interface SetCodeCompletionContextRequest extends RequestMessage
  {
    @JsProperty(name = "function") Integer getCurrentFunction();
    @JsProperty(name = "function") void setCurrentFunction(Integer fnId);
    @JsProperty(name = "class") String getCurrentClass();
    @JsProperty(name = "class") void setCurrentClass(String className);
    @JsProperty(name = "method") String getCurrentMethodSignature();
    @JsProperty(name = "method") void setCurrentMethodSignature(String signature);
    @JsProperty(name = "code") String getCode();
    @JsProperty(name = "code") void setCode(String code);
    @JsOverlay default StatementContainer getCodeStatementContainer() throws PlomReadException { return stringToStatementContainer(getCode()); }
    @JsOverlay default void setCodeStatementContainer(StatementContainer code) throws IOException { setCode(statementContainerToString(code)); }
    @JsProperty(name = "pos") String getCodePositionString();
    @JsProperty(name = "pos") void setCodePositionString(String pos);
    @JsOverlay default CodePosition getCodePosition() { return CodePosition.fromString(getCodePositionString()); }
    @JsOverlay default void setCodePosition(CodePosition pos) { setCodePositionString(pos.toString()); }
  }

  public static SetCodeCompletionContextRequest createSetCodeCompletionRequest(String id,
      Integer currentFunction, String currentClass, FunctionSignature currentMethod, StatementContainer currentCode, CodePosition currentPos) throws IOException
  {
    SetCodeCompletionContextRequest msg = (SetCodeCompletionContextRequest)createRequestMessage(MessageType.SET_CODE_COMPLETION_CONTEXT, id);
    if (currentFunction != null)
      msg.setCurrentFunction(currentFunction);
    if (currentClass != null)
      msg.setCurrentClass(currentClass);
    if (currentMethod != null)
      msg.setCurrentMethodSignature(signatureToString(currentMethod));
    if (currentCode != null)
      msg.setCodeStatementContainer(currentCode);
    if (currentPos != null)
      msg.setCodePosition(currentPos);
    return msg;
  }

//  @JsType(isNative = true)
//  public static interface SetCodeCompletionContextReplyMessage extends CancellableReplyMessage
//  {
//    @JsProperty(name = "cancelled") boolean isCancelled();
//    @JsProperty(name = "cancelled") void setCancelled(boolean cancelled);
//  }
//
//  public static CancellableReplyMessage createCancellableReplyMessage(MessageType type, String id, boolean cancelled)
//  {
//    CancellableReplyMessage msg = (CancellableReplyMessage)createCancellableReplyMessage(type, id, cancelled);
//    return msg;
//  }

  @JsType(isNative = true)
  public static interface GatherSuggestionsRequest extends RequestMessage
  {
    @JsProperty(name = "query") String getQuery();
    @JsProperty(name = "query") void setQuery(String query);
  }

  public static GatherSuggestionsRequest createGatherSuggestionsRequest(MessageType type, String requestId, String query)
  {
    GatherSuggestionsRequest msg = (GatherSuggestionsRequest)createRequestMessage(type, requestId);
    msg.setQuery(query);
    return msg;
  }
  
  public static GatherSuggestionsRequest createGatherVariableSuggestionsRequest(String requestId, String query)
  {
    return createGatherSuggestionsRequest(MessageType.GATHER_VARIABLE_SUGGESTIONS, requestId, query);
  }

  public static GatherSuggestionsRequest createGatherMemberSuggestionsRequest(String requestId, String query)
  {
    return createGatherSuggestionsRequest(MessageType.GATHER_MEMBER_SUGGESTIONS, requestId, query);
  }
  
  @JsType(isNative = true)
  public static interface GatherTypeSuggestionsRequest extends GatherSuggestionsRequest
  {
    @JsProperty(name = "void") boolean getAllowVoid();
    @JsProperty(name = "void") void setAllowVoid(boolean allowVoid);
  }

  public static GatherTypeSuggestionsRequest createGatherTypeSuggestionsRequest(String requestId, String query, boolean allowVoid)
  {
    GatherTypeSuggestionsRequest msg = (GatherTypeSuggestionsRequest)createGatherSuggestionsRequest(MessageType.GATHER_TYPE_SUGGESTIONS, requestId, query);
    msg.setAllowVoid(allowVoid);
    return msg;
  }

  @JsType(isNative = true)
  public static interface GatherStaticMemberSuggestionsRequest extends GatherSuggestionsRequest
  {
    @JsProperty(name = "constructors") boolean getIncludeConstructors();
    @JsProperty(name = "constructors") void setIncludeConstructors(boolean constructors);
    @JsProperty(name = "nonconstructors") boolean getIncludeNonConstructors();
    @JsProperty(name = "nonconstructors") void setIncludeNonConstructors(boolean nonconstructors);
  }

  public static GatherSuggestionsRequest createGatherStaticMemberSuggestionsRequest(String requestId, String query, boolean includeNonConstructors, boolean includeConstructors)
  {
    GatherStaticMemberSuggestionsRequest msg = (GatherStaticMemberSuggestionsRequest)createGatherSuggestionsRequest(MessageType.GATHER_STATIC_MEMBER_SUGGESTIONS, requestId, query);
    msg.setIncludeConstructors(includeConstructors);
    msg.setIncludeNonConstructors(includeNonConstructors);
    return msg;
  }

  @JsType(isNative = true)
  public static interface GatherSuggestionsReply extends CancellableReplyMessage
  {
    @JsProperty(name = "suggestions") ArrayOf<String> getSuggestions();
    @JsProperty(name = "suggestions") void setSuggestions(ArrayOf<String> suggestions);
    @JsOverlay default List<String> getSuggestionsList()
    {
      if (getSuggestions() == null) return null;
      return arrayOfToList(getSuggestions(), json -> json);
    }
    @JsOverlay default void setSuggestionsList(List<String> suggestions)
    {
      if (suggestions != null)
        setSuggestions(listToArrayOf(suggestions, str -> str));
    }
  }

  public static GatherSuggestionsReply createGatherSuggestionsReply(String id, boolean cancelled, List<String> suggestions)
  {
    GatherSuggestionsReply msg = (GatherSuggestionsReply)createCancellableReplyMessage(id, cancelled);
    if (suggestions != null)
      msg.setSuggestionsList(suggestions);
    return msg;
  }

  public static RequestMessage createGatherExpectedTypeTokensRequest(String requestId)
  {
    return createRequestMessage(MessageType.GATHER_EXPECTED_TYPE_TOKENS, requestId);
  }

  @JsType(isNative = true)
  public static interface GatherExpectedTypeTokensReply extends CancellableReplyMessage
  {
    @JsProperty(name = "expectedType") ArrayOf<String> getExpectedType();
    @JsProperty(name = "expectedType") void setExpectedType(ArrayOf<String> tokens);
    @JsOverlay default List<Token> getExpectedTypeTokens() throws PlomReadException
    {
      if (getExpectedType() == null) return null;
      return arrayOfToList(getExpectedType(), str -> stringToToken(str));
    }
    @JsOverlay default void setExpectedTypeTokens(List<Token> tokens) throws IOException
    {
      if (tokens != null)
        setExpectedType(listToArrayOf(tokens, tok -> tokenToString(tok)));
    }
  }

  public static GatherExpectedTypeTokensReply createGatherExpectedTypeTokensReply(String id, boolean cancelled, List<Token> expectedType) throws IOException
  {
    GatherExpectedTypeTokensReply msg = (GatherExpectedTypeTokensReply)createCancellableReplyMessage(id, cancelled);
    msg.setExpectedTypeTokens(expectedType);
    return msg;
  }

  
  public static enum MessageType
  {
    REPLY("reply"), 
    IMPORT_STDLIB("importStdLib"), 
    LOAD_MODULE("loadModule"),
    GET_FUNCTION_DESCRIPTION("functionDescription"),
    GET_FUNCTION_DESCRIPTION_FROM_ID("functionDescriptionFromId"),
    GET_CLASS_DESCRIPTION("classDescription"),
    IS_STDLIB("isStdLib"),
    SAVE_FUNCTION_CODE("saveFunctionCode"),
    GET_ALL_CLASSES_SORTED("getAllClassesSorted"),
    GET_ALL_FUNCTIONS_SORTED("getAllFunctionsSorted"),
    GET_DELETED_CLASSES("getDeletedClasses"),
    GET_MODULE_CLASSES("getModuleClasses"),
    GET_VARDECL_CODE("getVarDeclCode"),
    GET_IMPORTED_VARDECL_CODE("getImportedVarDeclCode"),
    SET_VARDECL_CODE("setVarDeclCode"), 
    MAKE_NEW_EMPTY_FUNCTION("makeNewEmptyFunction"),
    MAKE_NEW_EMPTY_CLASS("makeNewEmptyClass"),
    MAKE_NEW_EMPTY_METHOD("makeNewEmptyMethod"),
    SAVE_MODULE_TO_STRING("saveModuleToString"),
    CHANGE_FUNCTION_SIGNATURE("changeFunctionSignature"),
    DELETE_FUNCTION("deleteFunction"),
    DELETE_CLASS("deleteClass"),
    UPDATE_CLASS_BASE_INFO("updateClassBaseInfo"),
    DELETE_CLASS_METHOD("deleteClassMethod"), 
    CHANGE_METHOD_SIGNATURE("changeMethodSignature"), 
    SAVE_METHOD_CODE("saveMethodCode"), 
    LOAD_CLASS("loadClass"),
    SET_CODE_COMPLETION_CONTEXT("setCodeCompletionContext"),
    GATHER_TYPE_SUGGESTIONS("gatherTypeSuggestions"), 
    GATHER_VARIABLE_SUGGESTIONS("gatherVariableSuggestions"), 
    GATHER_MEMBER_SUGGESTIONS("gatherMemberSuggestions"),
    GATHER_STATIC_MEMBER_SUGGESTIONS("gatherStaticMemberSuggestions"),
    GATHER_EXPECTED_TYPE_TOKENS("gatherExpectedTypeTokens");
    private MessageType(String val)
    {
      this.value = val;
    }
    String value;
  }
}
