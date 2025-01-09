package org.programmingbasics.plom.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.programmingbasics.plom.core.WebHelpers.Base64EncoderDecoder;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.MethodArgumentExtractor;
import org.programmingbasics.plom.core.interpreter.ReturnTypeExtractor;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibClass;
import org.programmingbasics.plom.core.interpreter.StandardLibrary.StdLibMethod;
import org.programmingbasics.plom.core.interpreter.UnboundType;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import jsinterop.annotations.JsType;

@JsType
public class ModuleCodeRepository extends org.programmingbasics.plom.core.codestore.ModuleCodeRepository
{
  /** Tracks all the extra non-Plom files stored in the module */
  List<FileDescription> extraFiles = new ArrayList<>();
  
  /** Handles operations on extra files */
  ExtraFilesManager fileManager;
  
  public ModuleCodeRepository()
  {
//    // Create a basic main function that can be filled in
//    FunctionDescription func = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "main"),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.SimpleToken("var", Symbol.Var),
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":", Symbol.Colon),
//                Token.ParameterToken.fromContents("@string", Symbol.AtType)
//                ),
//            new TokenContainer(
//                Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                new Token.SimpleToken(":=", Symbol.Assignment),
//                Token.ParameterToken.fromContents(".input:", Symbol.DotVariable,
//                    new TokenContainer(new Token.SimpleToken("\"Guess a number between 1 and 10\"", Symbol.String)))
//                ),
//            new TokenContainer(
//                new Token.OneExpressionOneBlockToken("if", Symbol.COMPOUND_IF,
//                    new TokenContainer(
//                        Token.ParameterToken.fromContents(".a", Symbol.DotVariable),
//                        new Token.SimpleToken("=", Symbol.Eq),
//                        new Token.SimpleToken("\"8\"", Symbol.String)
//                        ),
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"You guessed correctly\"", Symbol.String)
//                                    ))
//                            ))
//                    ),
//                new Token.OneBlockToken("else", Symbol.COMPOUND_ELSE, 
//                    new StatementContainer(
//                        new TokenContainer(
//                            Token.ParameterToken.fromContents(".print:", Symbol.DotVariable, 
//                                new TokenContainer(
//                                    new Token.SimpleToken("\"Incorrect\"", Symbol.String)
//                                    ))
//                            ))
//                    )
//                )
//            )
//        );
//    functions.put(func.sig.getLookupName(), func);
    
//    FunctionDescription testParamFunc = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@number", Symbol.AtType), Arrays.asList("test"), Arrays.asList("arg1"), Arrays.asList(Token.ParameterToken.fromContents("@number", Symbol.AtType)), null),
//        new StatementContainer());
//    functions.put(testParamFunc.sig.getLookupName(), testParamFunc);
//    
//    FunctionDescription printStringPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print string:", "value", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Prints a string to the screen", Symbol.DUMMY_COMMENT),
//                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
//            ));
//    functions.put(printStringPrimitive.sig.getLookupName(), printStringPrimitive);
//
//    FunctionDescription printPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "print:", "value", Token.ParameterToken.fromContents("@object", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Prints a value to the screen", Symbol.DUMMY_COMMENT),
//                Token.ParameterToken.fromContents(".print string:", Symbol.DotVariable, 
//                    new TokenContainer(
//                        Token.ParameterToken.fromContents(".value", Symbol.DotVariable),
//                        Token.ParameterToken.fromContents(".to string", Symbol.DotVariable))
//                    )
//            )));
//    functions.put(printPrimitive.sig.getLookupName(), printPrimitive);
//
//    FunctionDescription inputPrimitive = new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@string", Symbol.AtType), "input:", "prompt", Token.ParameterToken.fromContents("@string", Symbol.AtType)),
//        new StatementContainer(
//            new TokenContainer(
//                new Token.WideToken("// Displays a prompt asking for input and returns the value entered by the user", Symbol.DUMMY_COMMENT),
//                new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))
//            ));
//    functions.put(inputPrimitive.sig.getLookupName(), inputPrimitive);
//
//    addGlobalVarAndResetIds("var", Token.ParameterToken.fromContents("@object", Symbol.AtType));
//    
//    ClassDescription testClass = addClassAndResetIds("Test");
//    testClass.addMethod(new FunctionDescription(
//        FunctionSignature.from(Token.ParameterToken.fromContents("@void", Symbol.AtType), "new")
//            .setIsConstructor(true),
//        new StatementContainer()));
  }

  public List<FileDescription> getAllExtraFilesSorted()
  {
    List<FileDescription> toReturn = new ArrayList<>();
    gatherAll((codeRepo) -> {
      if (codeRepo instanceof ModuleCodeRepository)
      {
        ModuleCodeRepository extraCodeRepo = (ModuleCodeRepository)codeRepo;
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
