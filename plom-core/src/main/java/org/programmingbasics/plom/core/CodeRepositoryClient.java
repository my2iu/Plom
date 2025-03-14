package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.programmingbasics.plom.core.CodeWidgetBase.CodeCompletionSuggester;
import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FileDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringToString;
import jsinterop.annotations.JsType;

@JsType
public class CodeRepositoryClient // extends org.programmingbasics.plom.core.codestore.ModuleCodeRepository
{
  /**
   * Cached information about whether the module in the language server
   * has the isNoStdLib flag set or not. (Flag is set if the module
   * represents a library that has no standard library, so extra
   * low-level instructions are allowed.)
   */
  boolean cachedIsNoStdLib = false;
  
  /** Tracks all the extra non-Plom files stored in the module */
  List<FileDescription> extraFiles = new ArrayList<>();
  
  /** Handles operations on extra files */
  ExtraFilesManager fileManager;
  
  /** For communicating with the language server worker */
  LanguageServerClientConnection languageServer;
  
  public CodeRepositoryClient(LanguageServerClientConnection clientConnection)
  {
    this.languageServer = clientConnection;
//    localRepo = new ModuleCodeRepository();
  }

  public void importStdLibRepository()
  {
    languageServer.sendImportStdLibRepository();
//    ModuleCodeRepository subRepository = new ModuleCodeRepository();
//    subRepository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
//    try {
//      PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(Main.getStdLibCodeText());
//      PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
//      subRepository.loadModulePlain(lexer, null);
//    }
//    catch (PlomReadException e)
//    {
//      e.printStackTrace();
//    }
//    subRepository.markAsImported();
  }
  
  public List<FileDescription> getAllExtraFilesSorted()
  {
//    toFixLater();
    List<FileDescription> toReturn = new ArrayList<>();
    for (FileDescription f: extraFiles)
      toReturn.add(f);
//    gatherAll((codeRepo) -> {
//      if (codeRepo instanceof CodeRepositoryClient)
//      {
//        CodeRepositoryClient extraCodeRepo = (CodeRepositoryClient)codeRepo;
//        for (FileDescription f: extraCodeRepo.extraFiles)
//          toReturn.add(f);
//      }
//    });
    toReturn.sort(Comparator.comparing(f -> f.getPath()));
    return toReturn;
  }
  
  public boolean hasExtraFile(String path)
  {
    return extraFiles.stream().anyMatch((file) -> file.getPath().equals(path));
  }

  /** Refresh the internal list of extra files in the module */
  public void refreshExtraFiles(ExtraFilesManager.EmptyCallback callback)
  {
//    toFixLater();
    if (fileManager == null) 
    {
      callback.call();
      return;
    }
    fileManager.getFileList(filenames -> {
      extraFiles.clear();
      for (String name: filenames)
      {
        FileDescription file = new FileDescription();
        file.filePath = name;
        // TODO: handle imported status
        extraFiles.add(file);
      }
      callback.call();
    });

  }
  
  public void setExtraFilesManager(ExtraFilesManager newFileManager)
  {
//    toFixLater();
    fileManager = newFileManager; 
  }
  
  public ExtraFilesManager getExtraFilesManager() { return fileManager; }
  
  /**
   * Marks all the contents of this module as being imported
   */
//  public void markAsImported()
//  {
//    toFix();
//    localRepo.markAsImported();
//    for (FileDescription f: extraFiles)
//      f.setImported(true);
//  }
  
  public WebHelpers.Promise<Void> saveModuleWithExtraFiles(final PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses, WebHelpers.PromiseCreator promiseCreator, WebHelpers.Promise.All promiseAll, Function<ArrayBuffer, Uint8Array> bufToUint8Array) throws IOException
  {
    // Save out the module contents
    return languageServer.sendSaveModuleToString(saveClasses, true)
      .then((code) -> {
        out.append(code);

        // Launch promises to get contents of files
        List<String> filePaths = new ArrayList<>();
        ArrayOf<WebHelpers.Promise<ArrayBuffer>> fileContentPromises = Collections.arrayOf();
        for (FileDescription f: extraFiles)
        {
          filePaths.add(f.getPath());
          fileContentPromises.push(promiseCreator.create((resolve, reject) -> {
            fileManager.getFileContents(f.getPath(), contents -> {
              resolve.accept(contents);
            });
          }));
        }
        
        // Wait until all the file contents have been retrieved
        return promiseAll.all(fileContentPromises).thenNow(fileContents -> {
          for (int n = 0; n < filePaths.size(); n++)
          {
            out.token("file");

            out.append(" ");
            out.append("\"");
            out.append(PlomTextWriter.escapeStringLiteral(filePaths.get(n)));
            out.append("\"");

            out.token("{");
            ArrayBuffer buf = fileContents.get(n);
            out.append(WebHelpers.Base64EncoderDecoder.encodeToString(bufToUint8Array.apply(buf), false));
            out.append("}");
            out.newline();
          }
          
          // Close out the module
          out.token("}");

          return null;
        });
      });

    
  }
  
  
  public WebHelpers.Promise<Void> loadModule(String codeStr) throws PlomReadException
  {
    // Loading extra files from a module is done asynchronously, so
    // we use promises to keep track of when loading is done.
    ArrayOf<WebHelpers.Promise<String>> extraFilesPromises = Collections.arrayOf();

    Promise<Void> moduleLoad = languageServer.sendLoadModule(codeStr)
      .<MapFromStringToString>thenNow((files) -> {
        // In certain restricted scenarios (web version of Plom where
        // everything has to be packed in a single file), there might be
        // entire extra files stored in the Module. The contents of these 
        // files will be passed back from the language server worker
        // so that these files can be stored on disk in their proper
        // location (assuming we're running Plom as an app with storage access) 
        if (files == null)
          return null;
        ArrayOfString keys = files.keys();
        for (int n = 0; n < keys.length(); n++)
        {
          String filePath = keys.get(n);
          final Uint8Array rawData = Base64EncoderDecoder.decodeBase64ToUint8Array(files.get(filePath));
          extraFilesPromises.push(WebHelpersShunt.newPromise((resolve, reject) -> {
            getExtraFilesManager().insertFile(filePath, rawData.getBuffer(), () -> {
              resolve.accept(filePath);
            });
          }));
        }
        return null;
      })
      .catchErrNow((err) -> {
        if (err instanceof String)
          Browser.getWindow().getConsole().log((String)err);
        else if (err instanceof Throwable)
          Browser.getWindow().getConsole().log(((Throwable)err).getMessage());
        return null;
      })
      .<Boolean>then((unused) -> {
        // Cache the stdlib flag of the loaded module
        return languageServer.sendIsStdLib();
      })
      .<Void>thenNow((stdlibFlag) -> {
        cachedIsNoStdLib = stdlibFlag;
        return null;
      });
    
    return moduleLoad
        .then((unused) -> WebHelpersShunt.promiseAll(extraFilesPromises))
        .thenNow(doneArray -> null);
  }
  

  public Promise<Void> loadClassStringIntoModule(String code) 
  {
    return languageServer.sendLoadClass(code);
  }

//  public void saveModule(PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses) throws IOException
//  {
//    toFix();
//    localRepo.saveModule(out, saveClasses);
//  }
  
  public Promise<String> saveModuleToString(boolean saveClasses)
  {
    return languageServer.sendSaveModuleToString(saveClasses, false);
  }
  
  public boolean isNoStdLibFlag()
  {
    return cachedIsNoStdLib;
  }
  
//  public void loadBuiltInPrimitives(List<StdLibClass> stdLibClasses, List<StdLibMethod> stdLibMethods)
//  {
//    toFix();
//    localRepo.loadBuiltInPrimitives(stdLibClasses, stdLibMethods);
//  }


//  public void setChainedRepository(ModuleCodeRepository other)
//  {
//    toFix();
//    localRepo.setChainedRepository(other);
//  }
  
  public Promise<List<ClassDescription>> getDeletedClasses()
  {
    return languageServer.sendGetDeletedClasses();
  }
  
  public Promise<List<ClassDescription>> getClasses()
  {
    return languageServer.sendGetModuleClasses();
  }
  
  public Promise<List<ClassDescription>> getAllClassesSorted()
  {
    return languageServer.sendGetAllClassesSorted();
  }

//  public ClassDescription addClassAndResetIds(String name)
//  {
//    toFix();
//    return localRepo.addClassAndResetIds(name);
//  }

//  public void deleteClassAndResetIds(ModuleCodeRepository module, int id)
//  {
//    toFix();
//    localRepo.deleteClassAndResetIds(module, id);
//  }
  
  public Promise<Void> deleteClass(ClassDescription cls)
  {
    return languageServer.sendDeleteClass(cls);
  }

//  public boolean hasClassWithName(String name)
//  {
//    toFix();
//    return localRepo.hasClassWithName(name);
//  }

  public Promise<ClassDescription> findClassWithName(String name)
  {
    return languageServer.sendFindClass(name);
  }
  
  public Promise<FunctionDescription> getFunctionDescription(String name)
  {
    return languageServer.sendGetFunctionDescription(name);
  }

  public Promise<FunctionDescription> getFunctionDescriptionWithId(int id)
  {
    return languageServer.sendGetFunctionDescriptionFromId(id);
  }

  public Promise<Void> saveFunctionCode(int id, StatementContainer code)
  {
//    partialFix();
//    localRepo.getFunctionDescription(name).code = code;
    // TODO: synchronize things with the promise
    return languageServer.sendSaveFunctionCode(id, code);
  }
  
  public Promise<Void> changeFunctionSignature(FunctionSignature newSig, FunctionDescription oldFn)
  {
    Promise<Void> toReturn = languageServer.sendChangeFunctionSignature(newSig, oldFn.getId());
    oldFn.sig = newSig;
    return toReturn;
  }

  public Promise<List<FunctionDescription>> getAllFunctionSorted()
  {
    return languageServer.sendGetAllFunctionsSorted();
  }
  
  public Promise<Void> deleteFunction(FunctionSignature sig)
  {
    return languageServer.sendDeleteFunction(sig);
  }
  
//  public void deleteFunctionAndResetIds(ModuleCodeRepository module, int id)
//  {
//    toFix();
//    localRepo.deleteFunctionAndResetIds(module, id);
//  }
  
  public Promise<StatementContainer> getVariableDeclarationCode()
  {
    return languageServer.sendGetVariableDeclarationCode();
  }
  
  public void setVariableDeclarationCode(StatementContainer code)
  {
    languageServer.sendSetVariableDeclarationCode(code);
  }

  public Promise<StatementContainer> getImportedVariableDeclarationCode()
  {
    return languageServer.sendGetImportedVariableDeclarationCode();
  }
  
  public Promise<FunctionDescription> makeUniqueEmptyFunction()
  {
    return languageServer.sendMakeNewEmptyFunction();
  }

  public Promise<ClassDescription> makeNewUniqueClass()
  {
    return languageServer.sendMakeNewEmptyClass();
  }

  public Promise<FunctionDescription> makeNewUniqueMethod(ClassDescription cls, boolean isStatic, boolean isConstructor)
  {
    return languageServer.sendMakeNewEmptyMethod(cls, isStatic, isConstructor);
  }

  public Promise<ClassDescription> reloadClass(ClassDescription cl)
  {
    return languageServer.sendFindClass(cl.getName());
  }
  
  /** Updates the info in a class, but not the method information */
  public Promise<Void> updateClassBaseInfo(String className, ClassDescription cl)
  {
    return languageServer.sendUpdateClassBaseInfo(className, cl);
  }

  public Promise<Void> deleteClassMethod(ClassDescription cls, FunctionDescription fn)
  {
    cls.deleteMethod(fn.getId());
    return languageServer.sendDeleteClassMethod(cls, fn);
  }
  
  public Promise<Void> changeMethodSignature(ClassDescription cls, FunctionSignature newSig, FunctionDescription oldFn)
  {
    Promise<Void> toReturn = languageServer.sendChangeMethodSignature(cls, newSig, oldFn.getId());
    oldFn.sig = newSig;
    cls.updateMethod(oldFn);
    return toReturn;
  }

  public Promise<Void> saveMethodCode(ClassDescription cls,
      FunctionDescription method, StatementContainer code)
  {
    // Fill this in properly
    method.code = code;
    cls.updateMethod(method);
    return languageServer.sendSaveMethodCode(cls, method.sig, code);
  }

  // Context to be used for code completion suggestions
  public Promise<Void> setCodeCompletionContext(Integer currentFunction, String currentClass, FunctionSignature currentMethod, StatementContainer currentCode, CodePosition currentPos)
  {
    return languageServer.sendSetCodeCompletionContext(currentFunction, currentClass, currentMethod, currentCode, currentPos);
  }
  
  public Promise<List<String>> gatherTypeSuggestions(String val, boolean allowVoid)
  {
    return languageServer.sendGatherTypeSuggestions(val, allowVoid);
  }

  public Promise<List<String>> gatherVariableSuggestions(String val)
  {
    return languageServer.sendGatherVariableSuggestions(val);
  }

  public Promise<List<String>> gatherMemberSuggestions(String val)
  {
    return languageServer.sendGatherMemberSuggestions(val);
  }

  public Promise<List<String>> gatherStaticMemberSuggestions(String val, boolean includeNonConstructors, boolean includeConstructors)
  {
    return languageServer.sendGatherStaticMemberSuggestions(val, includeNonConstructors, includeConstructors);
  }
  
  /** When making a function lambda, it's hard to remember all the
   * names and parameters, so this will do a code suggestions of what
   * tokens are needed to complete a function lambda
   */
  public Promise<List<Token>> gatherExpectedTypeTokens()
  {
    return languageServer.sendGatherExpectedTypeTokens();
  }

  private class CodeCompletionSuggesterClient implements CodeWidgetBase.CodeCompletionSuggester {
    Integer currentFunctionOrNull;
    ClassDescription currentClassOrNull;
    FunctionSignature currentMethodOrNull;
    
    private CodeCompletionSuggesterClient() {}
    private CodeCompletionSuggesterClient(Integer forCurrentFunctionOrNull, ClassDescription forCurrentClassOrNull, FunctionSignature forCurrentMethodOrNull) {
      this.currentClassOrNull = forCurrentClassOrNull;
      this.currentFunctionOrNull = forCurrentFunctionOrNull;
      this.currentMethodOrNull = forCurrentMethodOrNull;
    }
    @Override
    public boolean isCurrentMethodConstructor()
    {
      if (currentMethodOrNull != null)
        return currentMethodOrNull.isConstructor;
      return false;
    }
    @Override
    public boolean isCurrentMethodStatic()
    {
      if (currentMethodOrNull != null)
        return currentMethodOrNull.isStatic;
      return false;
    }   
    @Override
    public Promise<Void> setCodeCompletionContextForTypes()
    {
      return CodeRepositoryClient.this.setCodeCompletionContext(null, null, null, null, null);
    }
    @Override
    public Promise<Void> setCodeCompletionContextFor(StatementContainer code, CodePosition pos)
    {
      String className = currentClassOrNull == null ? null : currentClassOrNull.getName();
      return CodeRepositoryClient.this.setCodeCompletionContext(
          currentFunctionOrNull, className, currentMethodOrNull, code, pos);
    }
    @Override
    public SuggesterClient makeTypeSuggester(boolean allowVoid)
    {
      return new SuggesterClient(null, false) {
        @Override void gatherSuggestions(String val, Consumer<List<String>> callback)
        {
          gatherTypeSuggestions(val, allowVoid).thenNow((suggestions) -> {
            if (suggestions != null)
              callback.accept(suggestions);
            return null;
          });
        }
      };
    }
    @Override
    public SuggesterClient makeVariableSuggester()
    {
      return new SuggesterClient(null, false) {
        @Override void gatherSuggestions(String val, Consumer<List<String>> callback)
        {
          gatherVariableSuggestions(val).thenNow((suggestions) -> {
            if (suggestions != null)
              callback.accept(suggestions);
            return null;
          });
        }
      };
    }
    @Override
    public SuggesterClient makeMemberSuggester()
    {
      return new SuggesterClient(null, true) {
        @Override void gatherSuggestions(String val, Consumer<List<String>> callback)
        {
          gatherMemberSuggestions(val).thenNow((suggestions) -> {
            if (suggestions != null)
              callback.accept(suggestions);
            return null;
          });
        }
      };
    }
    @Override
    public SuggesterClient makeStaticMemberSuggester(boolean includeNonConstructors, boolean includeConstructors)
    {
      return new SuggesterClient(null, true) {
        @Override void gatherSuggestions(String val, Consumer<List<String>> callback)
        {
          gatherStaticMemberSuggestions(val, includeNonConstructors, includeConstructors).thenNow((suggestions) -> {
            if (suggestions != null)
              callback.accept(suggestions);
            return null;
          });
        }
      };
    }
    
    @Override
    public Promise<List<Token>> gatherExpectedTypeTokens()
    {
      return CodeRepositoryClient.this.gatherExpectedTypeTokens();
    }
  }

  CodeWidgetBase.CodeCompletionSuggester makeCodeCompletionSuggesterNoContext()
  {
    return new CodeCompletionSuggesterClient();
  }

  public CodeCompletionSuggester makeCodeCompletionSuggesterWithContext(Integer currentFunctionOrNull, ClassDescription currentClassOrNull, FunctionSignature currentMethodOrNull)
  {
    return new CodeCompletionSuggesterClient(currentFunctionOrNull, currentClassOrNull, currentMethodOrNull);
  }
}
