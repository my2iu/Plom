package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import jsinterop.annotations.JsType;

@JsType
public class CodeRepositoryClient extends org.programmingbasics.plom.core.codestore.ModuleCodeRepository
{
  /** Tracks all the extra non-Plom files stored in the module */
  List<FileDescription> extraFiles = new ArrayList<>();
  
  /** Handles operations on extra files */
  ExtraFilesManager fileManager;
  
  /** For communicating with the language server worker */
  LanguageServerClientConnection languageServer;
  
  public CodeRepositoryClient(LanguageServerClientConnection clientConnection)
  {
    this.languageServer = clientConnection;
  }

  public void importStdLibRepository()
  {
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
    setChainedRepository(subRepository);
  }
  
  public static CodeRepositoryClient makeRepositoryWithStdLib(LanguageServerClientConnection connection)
  {
    CodeRepositoryClient toReturn = new CodeRepositoryClient(connection);
    return toReturn;
  }
  
  public List<FileDescription> getAllExtraFilesSorted()
  {
    List<FileDescription> toReturn = new ArrayList<>();
    gatherAll((codeRepo) -> {
      if (codeRepo instanceof CodeRepositoryClient)
      {
        CodeRepositoryClient extraCodeRepo = (CodeRepositoryClient)codeRepo;
        for (FileDescription f: extraCodeRepo.extraFiles)
          toReturn.add(f);
      }
    });
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
    fileManager = newFileManager; 
  }
  
  public ExtraFilesManager getExtraFilesManager() { return fileManager; }
  
  /**
   * Marks all the contents of this module as being imported
   */
  public void markAsImported()
  {
    super.markAsImported();
    for (FileDescription f: extraFiles)
      f.setImported(true);
  }
  
  public WebHelpers.Promise<Void> saveModuleWithExtraFiles(final PlomTextWriter.PlomCodeOutputFormatter out, boolean saveClasses, WebHelpers.PromiseCreator promiseCreator, WebHelpers.Promise.All promiseAll, Function<ArrayBuffer, Uint8Array> bufToUint8Array) throws IOException
  {
    // Save out the module contents
    saveOpenModule(out, saveClasses);

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
    
  }
  
  
  public WebHelpers.Promise<Void> loadModule(PlomTextReader.PlomTextScanner lexer) throws PlomReadException
  {
    // Loading extra files from a module is done asynchronously, so
    // we use promises to keep track of when loading is done.
    ArrayOf<WebHelpers.Promise<String>> extraFilesPromises = Collections.arrayOf();

    // Load the module, but pass in an extra handler to extract out file information
    loadModulePlain(lexer, (lex) -> {
      String peek = lex.peekLexInput();
      if ("file".equals(peek)) {
        lex.expectToken("file");
        lex.swallowOptionalNewlineToken();
        String fileNameString = lex.lexInput();
        if (!fileNameString.startsWith("\""))
          throw new PlomReadException("Expecting a string for file name", lex);
        if (!fileNameString.endsWith("\""))
          throw new PlomReadException("Expecting a string for file name", lex);
        final String filePath = fileNameString.substring(1, fileNameString.length() - 1);
        lex.swallowOptionalNewlineToken();
        lex.expectToken("{");
        String fileData = lex.lexBase64();
        lex.swallowOptionalNewlineToken();
        lex.expectToken("}");
        lex.swallowOptionalNewlineToken();
        final Uint8Array rawData = Base64EncoderDecoder.decodeBase64ToUint8Array(fileData);
        extraFilesPromises.push(WebHelpersShunt.newPromise((resolve, reject) -> {
          getExtraFilesManager().insertFile(filePath, rawData.getBuffer(), () -> {
            resolve.accept(filePath);
          });
        }));
        return true;
      }
      return false;
    });
    
    return WebHelpersShunt.promiseAll(extraFilesPromises).thenNow(doneArray -> null);
  }
}
