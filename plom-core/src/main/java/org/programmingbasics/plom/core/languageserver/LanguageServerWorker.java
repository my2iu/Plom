package org.programmingbasics.plom.core.languageserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.programmingbasics.plom.core.Main;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.ClassDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.FunctionDescriptionJson;
import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages.MessageType;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.codestore.RepositoryScope;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.suggestions.CodeCompletionContext;
import org.programmingbasics.plom.core.suggestions.MemberSuggester;
import org.programmingbasics.plom.core.suggestions.StaticMemberSuggester;
import org.programmingbasics.plom.core.suggestions.TypeSuggester;
import org.programmingbasics.plom.core.suggestions.VariableSuggester;
import org.programmingbasics.plom.core.view.GatherCodeCompletionInfo;

import elemental.client.Browser;
import elemental.events.MessageEvent;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.MapFromStringTo;
import elemental.util.MapFromStringToString;
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

  // Right now, we can generate code completion suggestions for 
  // only one context at a time.
  CodeCompletionContext currentCodeCompletionContext;
  
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
      MapFromStringToString files = elemental.util.Collections.mapFromStringToString();
      try {
        repo.loadModuleCollectExtraFiles(lexer, files);
        if (repo.isNoStdLibFlag)
          repo.setChainedRepository(null);
        postMessage(CodeRepositoryMessages.createLoadModuleReply(loadModuleMsg.getRequestId(), true, null, files));
      } 
      catch (PlomReadException e)
      {
        postMessage(CodeRepositoryMessages.createLoadModuleReply(loadModuleMsg.getRequestId(), false, e.getMessage(), null));
        e.printStackTrace();
      }
      break;
    }
    case LOAD_CLASS:
    {
      CodeRepositoryMessages.LoadModuleMessage loadModuleMsg = (CodeRepositoryMessages.LoadModuleMessage)msg;
      PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(loadModuleMsg.getCode());
      PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
      
      try {
        repo.loadClassIntoModule(lexer);
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
    case GET_FUNCTION_DESCRIPTION_FROM_ID:
    {
      CodeRepositoryMessages.GetFromIdMessage idMsg = (CodeRepositoryMessages.GetFromIdMessage)msg; 
      FunctionDescription fd = repo.getFunctionDescriptionWithId(idMsg.getId());
      FunctionDescriptionJson json = (FunctionDescriptionJson)CodeRepositoryMessages.createEmptyObject();
      json.setAsFunctionDescription(fd);
      postMessage(CodeRepositoryMessages.createSingleObjectReplyMessage(idMsg.getRequestId(), json));
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
        repo.getFunctionDescriptionWithId(requestMsg.getFunctionId()).code = requestMsg.getCodeStatementContainer();
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
    case GET_DELETED_CLASSES:
    {
      try {
        CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
        List<ClassDescription> classes = repo.deletedClasses;
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
    case GET_MODULE_CLASSES:
    {
      try {
        CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
        List<ClassDescription> classes = repo.classes;
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
      repo.addFunction(func);
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
      ClassDescription c = repo.addClass(newClassName);

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
        if (requestMsg.isOpen())
          repo.saveOpenModule(new PlomTextWriter.PlomCodeOutputFormatter(out), requestMsg.isSaveClasses());
        else
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
        FunctionSignature newSig = CodeRepositoryMessages.stringToSignature(requestMsg.getNewSignature());
        repo.changeFunctionSignature(newSig, requestMsg.getFunctionId());
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
      repo.deleteFunction(fd.module, fd.getId());
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, nameMsg.getRequestId()));
      break;
    }
    case DELETE_CLASS:
    {
      CodeRepositoryMessages.GetFromNameMessage nameMsg = (CodeRepositoryMessages.GetFromNameMessage)msg; 
      ClassDescription cls = repo.findClassWithName(nameMsg.getName(), false);
      if (cls != null)
        repo.deleteClass(cls.module, cls.getId());
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
        cls.deleteMethod(fd.getId());
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
        FunctionSignature newSig = CodeRepositoryMessages.stringToSignature(requestMsg.getNewSignature());
        FunctionDescription fd = cls.findMethodWithId(requestMsg.getFunctionId());
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
        int methodId = requestMsg.getMethodId();
//        FunctionSignature sig = CodeRepositoryMessages.stringToSignature(requestMsg.getSignature());
//        FunctionDescription fd = cls.findMethod(sig.getLookupName(), sig.isConstructor || sig.isStatic);
        FunctionDescription fd = cls.findMethodWithId(methodId);
        fd.code = requestMsg.getCodeStatementContainer();
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createReplyMessage(MessageType.REPLY, requestMsg.getRequestId()));
      break;
    }
    case SET_CODE_COMPLETION_CONTEXT:
    {
      CodeRepositoryMessages.SetCodeCompletionContextRequest requestMsg = (CodeRepositoryMessages.SetCodeCompletionContextRequest)msg;
      try {
        // Depending on the context of where we are in the code, we should add
        // different variables to the scope that can be suggested for code completion
        CodeCompletionContext.Builder suggestionContextBuilder = CodeCompletionContext.builder();
        // Store global variables
        StandardLibrary.createGlobals(null, suggestionContextBuilder.currentScope(), suggestionContextBuilder.coreTypes());
        suggestionContextBuilder.currentScope().setParent(new RepositoryScope(repo, suggestionContextBuilder.coreTypes(), null));
        ////  if (globalConfigurator != null)
        ////  globalConfigurator.configure(suggestionContextBuilder.currentScope(), suggestionContextBuilder.coreTypes());
        // Store any class variables, method parameters
        ClassDescription currentClass = null;
        if (requestMsg.getCurrentClass() != null)
        {
          currentClass = repo.findClassWithName(requestMsg.getCurrentClass(), true);
          try {
            suggestionContextBuilder.setDefinedClassOfMethod(suggestionContextBuilder.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(currentClass.getName())));
          }
          catch (RunException e)
          {
            // Ignore any errors when setting this
          }

          // Create an object scope that will handle this and instance variables
          try {
            Value thisValue = new Value();
            thisValue.type = suggestionContextBuilder.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName(currentClass.getName()));
            suggestionContextBuilder.pushObjectScope(thisValue);
          } 
          catch (RunException e)
          {
            // Ignore any errors when setting this
          }
        }
        // If we're in a method, set the method type
        FunctionSignature currentMethodSignature = null;
        if (requestMsg.getCurrentMethodSignature() != null)
        {
          currentMethodSignature = CodeRepositoryMessages.stringToSignature(requestMsg.getCurrentMethodSignature());
          suggestionContextBuilder.setIsStaticMethod(currentMethodSignature.isStatic);
          suggestionContextBuilder.setIsConstructorMethod(currentMethodSignature.isConstructor);
        }

        // Add in function arguments
        FunctionDescription fd = null; 
        if (requestMsg.getCurrentFunction() != null)
          fd = repo.getFunctionDescriptionWithId(requestMsg.getCurrentFunction());
        else if (currentMethodSignature != null)
          fd = currentClass.findMethod(currentMethodSignature.getLookupName(), currentMethodSignature.isConstructor || currentMethodSignature.isStatic);
        if (fd != null)
        {
          suggestionContextBuilder.pushNewScope();
          for (int n = 0; n < fd.sig.getNumArgs(); n++)
          {
            String name = fd.sig.getArgName(n);
            UnboundType unboundType = fd.sig.getArgType(n);
            try {
              Type type = suggestionContextBuilder.currentScope().typeFromUnboundTypeFromScope(unboundType);
              suggestionContextBuilder.currentScope().addVariable(name, type, new Value());
            }
            catch (RunException e)
            {
              // Ignore the argument if it doesn't have a valid type
            }
          }
        }
        ////      if (variableContextConfigurator != null)
        ////        variableContextConfigurator.accept(suggestionContextBuilder);
        suggestionContextBuilder.pushNewScope();
        CodeCompletionContext suggestionContext = suggestionContextBuilder.build();
        if (requestMsg.getCode() != null && requestMsg.getCodePositionString() != null)
          GatherCodeCompletionInfo.fromStatements(requestMsg.getCodeStatementContainer(), suggestionContext, requestMsg.getCodePosition(), 0);
        currentCodeCompletionContext = suggestionContext;
      }
      catch (PlomReadException e)
      {
        e.printStackTrace();
      }
      postMessage(CodeRepositoryMessages.createCancellableReplyMessage(requestMsg.getRequestId(), false));
      break;
    }
    case GATHER_TYPE_SUGGESTIONS:
    {
      CodeRepositoryMessages.GatherTypeSuggestionsRequest requestMsg = (CodeRepositoryMessages.GatherTypeSuggestionsRequest)msg;
      TypeSuggester suggester = new TypeSuggester(currentCodeCompletionContext, requestMsg.getAllowVoid());
      postMessage(CodeRepositoryMessages.createGatherSuggestionsReply(requestMsg.getRequestId(), false, suggester.gatherSuggestions(requestMsg.getQuery())));
      break;
    }
    case GATHER_VARIABLE_SUGGESTIONS:
    {
      CodeRepositoryMessages.GatherSuggestionsRequest requestMsg = (CodeRepositoryMessages.GatherSuggestionsRequest)msg;
      VariableSuggester suggester = new VariableSuggester(currentCodeCompletionContext); 
      postMessage(CodeRepositoryMessages.createGatherSuggestionsReply(requestMsg.getRequestId(), false, suggester.gatherSuggestions(requestMsg.getQuery())));
      break;
    }
    case GATHER_MEMBER_SUGGESTIONS:
    {
      CodeRepositoryMessages.GatherSuggestionsRequest requestMsg = (CodeRepositoryMessages.GatherSuggestionsRequest)msg;
      MemberSuggester suggester = new MemberSuggester(currentCodeCompletionContext);
      postMessage(CodeRepositoryMessages.createGatherSuggestionsReply(requestMsg.getRequestId(), false, suggester.gatherSuggestions(requestMsg.getQuery())));
      break;
    }
    case GATHER_STATIC_MEMBER_SUGGESTIONS:
    {
      CodeRepositoryMessages.GatherStaticMemberSuggestionsRequest requestMsg = (CodeRepositoryMessages.GatherStaticMemberSuggestionsRequest)msg;
      StaticMemberSuggester suggester = new StaticMemberSuggester(currentCodeCompletionContext, requestMsg.getIncludeNonConstructors(), requestMsg.getIncludeConstructors());
      postMessage(CodeRepositoryMessages.createGatherSuggestionsReply(requestMsg.getRequestId(), false, suggester.gatherSuggestions(requestMsg.getQuery())));
      break;
    }
    case GATHER_EXPECTED_TYPE_TOKENS:
    {
      CodeRepositoryMessages.RequestMessage requestMsg = (CodeRepositoryMessages.RequestMessage)msg;
      Type expectedType = currentCodeCompletionContext.getExpectedExpressionType();
      try {
        if (expectedType != null)
        {
          List<Token> toReturn = makeTokensForType(expectedType);
          postMessage(CodeRepositoryMessages.createGatherExpectedTypeTokensReply(requestMsg.getRequestId(), false, toReturn));
        }
        else
          postMessage(CodeRepositoryMessages.createGatherExpectedTypeTokensReply(requestMsg.getRequestId(), false, null));
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      break;
    }
      
    default:
      Browser.getWindow().getConsole().log("Language server received unknown message type " + msg.getType());
      break;
    }
    
  }
  
  /** Given a type, returns a list of tokens that can be used to create that type in some code */
  static List<Token> makeTokensForType(Type type)
  {
    List<Token> toReturn = new ArrayList<>();
    if (type instanceof Type.LambdaFunctionType)
    {
      Type.LambdaFunctionType lambdaType = (Type.LambdaFunctionType)type;
      Token.ParameterToken newToken = new Token.ParameterToken(
          Token.ParameterToken.splitVarAtColons("f@" + lambdaType.name), 
          Token.ParameterToken.splitVarAtColonsForPostfix("f@" + lambdaType.name), 
          Symbol.FunctionTypeName);
      for (int n = 0; n < lambdaType.args.size(); n++)
      {
        if (lambdaType.optionalArgNames.get(n) != null && !lambdaType.optionalArgNames.get(n).isEmpty())
        {
          newToken.parameters.get(n).tokens.add(Token.ParameterToken.fromContents("." + lambdaType.optionalArgNames.get(n), Symbol.DotVariable));
        }
        newToken.parameters.get(n).tokens.addAll(makeTokensForType(lambdaType.args.get(n)));
      }
      toReturn.add(newToken);
      
      if (lambdaType.returnType != null)
      {
        toReturn.add(new Token.SimpleToken("returns", Symbol.Returns));
        toReturn.addAll(makeTokensForType(lambdaType.returnType));
      }
    }
    else if (type instanceof Type)
    {
      toReturn.add(Token.ParameterToken.fromContents("@" + type.name, Symbol.AtType));
    }
    return toReturn;
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
