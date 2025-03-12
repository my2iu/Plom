package org.programmingbasics.plom.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.programmingbasics.plom.core.WebHelpers.Promise;
import org.programmingbasics.plom.core.WebHelpers.Promise.Consumer;
import org.programmingbasics.plom.core.WebHelpers.Promise.PromiseConstructorFunction;
import org.programmingbasics.plom.core.WebHelpersShunt.ByteArrayUint8Array;
import org.programmingbasics.plom.core.WebHelpersShunt.JsEmulatedPromise;
import org.programmingbasics.plom.core.ast.PlomTextReader;
import org.programmingbasics.plom.core.ast.PlomTextReader.PlomReadException;
import org.programmingbasics.plom.core.ast.PlomTextWriter;
import org.programmingbasics.plom.core.ast.PlomTextWriter.PlomCodeOutputFormatter;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.codestore.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.interpreter.StandardLibrary;
import org.programmingbasics.plom.core.interpreter.UnboundType;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import elemental.util.ArrayOf;
import elemental.util.MapFromStringToString;
import junit.framework.TestCase;

public class CodeRepositoryClientTest extends TestCase
{
  @Test
  public void testSaveExtraFiles() throws IOException, InterruptedException, ExecutionException
  {
    StringBuilder strBuilder = new StringBuilder();

    // Make a fake language server backend
    ModuleCodeRepository workerRepo = new ModuleCodeRepository();
    workerRepo.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
    workerRepo.addFunction(new FunctionDescription(
        FunctionSignature.from(UnboundType.forClassLookupName("number"), "get"),
        new StatementContainer(
            new TokenContainer(
                new Token.SimpleToken("return", Symbol.Return),
                new Token.SimpleToken("3", Symbol.Number)))));
    LanguageServerClientConnection connection = new LanguageServerClientConnection() {
      @Override public Promise<String> sendSaveModuleToString(boolean saveClasses,
          boolean isOpen)
      {
        Assert.assertTrue(isOpen);
        Assert.assertTrue(saveClasses);
        StringBuilder out = new StringBuilder();
        try {
          workerRepo.saveOpenModule(new PlomTextWriter.PlomCodeOutputFormatter(out), saveClasses);
          return WebHelpersShunt.promiseResolve(out.toString());
        }
        catch (IOException e)
        {
          Assert.fail();
          throw new IllegalArgumentException(e);
        }
      }
    };
    
    // Try saving out a repository with extra files in it
    CodeRepositoryClient repository = new CodeRepositoryClient(connection);
    Promise<Void> promise = new WebHelpersShunt.JsEmulatedPromise<Void>((resolve, reject) -> {
      repository.setExtraFilesManager(new ExtraFilesManagerWebInMemory());
      repository.getExtraFilesManager().insertFile("web/test.txt", 
          ByteArrayUint8Array.fromByteArray("hello".getBytes(StandardCharsets.UTF_8)).getBuffer(), 
          () -> {
            repository.refreshExtraFiles(() -> {
              resolve.accept(null);
            });
          });
    })
    .then(dummy -> {
      PlomCodeOutputFormatter out = new PlomCodeOutputFormatter(strBuilder);

      try {
        return repository.saveModuleWithExtraFiles(out, true,
            new WebHelpers.PromiseCreator() {
              @Override public <U> WebHelpers.Promise<U> create(PromiseConstructorFunction<U> createCallback)
              {
                return new WebHelpersShunt.JsEmulatedPromise<>(createCallback);
              }
            },
            new WebHelpers.Promise.All() {
              @Override public <U> WebHelpers.Promise<ArrayOf<U>> all(ArrayOf<WebHelpers.Promise<U>> promises)
              {
                return WebHelpersShunt.JsEmulatedPromise.promiseAll(promises);
              }
            },
            buf -> {
              if (buf instanceof ByteArrayUint8Array) 
                return (Uint8Array)buf; 
              else 
                return null; 
            });
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException(e);
      }
    });
    
    ((WebHelpersShunt.JsEmulatedPromise<Void>)promise).future.get();
    Assert.assertEquals(" module .{program} {\n" + 
        " vardecls {\n" +
        " }\n" +
        " function . {get } { @ {number } } { } {\n" + 
        " return 3\n" + 
        " }\n" +
        " file \"web/test.txt\" {aGVsbG9}\n" + 
        " }",
        strBuilder.toString());
  }
  
  @Test
  public void testLoadModuleWithExtraFiles() throws PlomReadException, InterruptedException, ExecutionException
  {
    String codeStr = " module .{program} {\n" + 
        " vardecls {\n" +
        " }\n" +
        " function . {get } { @ {number } } { } {\n" + 
        " return 3\n" + 
        " }\n" +
        " file \"web/test.txt\" {aGVsbG9}\n" + 
        " }";
    
//    PlomTextReader.StringTextReader in = new PlomTextReader.StringTextReader(codeStr);
//    PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(in);

    // Make a fake language server backend
    ModuleCodeRepository workerRepo = new ModuleCodeRepository();
    LanguageServerClientConnection connection = new LanguageServerClientConnection() {
      @Override
      public Promise<MapFromStringToString> sendLoadModule(String codeStr)
      {
        PlomTextReader.StringTextReader inStream = new PlomTextReader.StringTextReader(codeStr);
        PlomTextReader.PlomTextScanner lexer = new PlomTextReader.PlomTextScanner(inStream);
        MapFromStringToString files = elemental.util.Collections.mapFromStringToString();
        try {
          workerRepo.loadModuleCollectExtraFiles(lexer, files);

          return WebHelpersShunt.promiseResolve(files);
        } 
        catch (PlomReadException e)
        {
          return WebHelpersShunt.newPromise(new PromiseConstructorFunction<MapFromStringToString>() {
            @Override public void call(
                Consumer<MapFromStringToString> resolve,
                Consumer<Object> reject)
            {
              reject.accept(e);
            }
          });
        }
      }
      @Override
      public Promise<Boolean> sendIsStdLib()
      {
        return WebHelpersShunt.promiseResolve(workerRepo.isNoStdLibFlag);
      }
      @Override
      public Promise<StatementContainer> sendGetVariableDeclarationCode()
      {
        return WebHelpersShunt.promiseResolve(workerRepo.getVariableDeclarationCode());
      }
    };

    // Try loading a module with extra files
    CodeRepositoryClient loaded = new CodeRepositoryClient(connection);
    loaded.setExtraFilesManager(new ExtraFilesManagerWebInMemory());
    Promise<Void> extraFilesWaiter = loaded.loadModule(codeStr);
    extraFilesWaiter = extraFilesWaiter.then(dummy -> {
      return WebHelpersShunt.newPromise((resolve, reject) -> {
        loaded.refreshExtraFiles(() -> resolve.accept(null));
      });
    });
    
    ((JsEmulatedPromise<Void>)extraFilesWaiter).future.get();
    
    Assert.assertNotNull(workerRepo.getFunctionWithName("get"));
    Assert.assertEquals(0, ((WebHelpersShunt.JsEmulatedPromise<StatementContainer>)loaded.getVariableDeclarationCode()).future.join().statements.size());
    Assert.assertEquals(1, loaded.getAllExtraFilesSorted().size());
    Assert.assertEquals("web/test.txt", loaded.getAllExtraFilesSorted().get(0).getPath());
    
    Promise<ArrayBuffer> fileContentsPromise =  WebHelpersShunt.newPromise((resolve, reject) -> {
      loaded.getExtraFilesManager().getFileContents("web/test.txt", contents -> {
        resolve.accept(contents);
      });
    });
      
    ArrayBuffer fileContents = ((JsEmulatedPromise<ArrayBuffer>)fileContentsPromise).future.get();
    String fileContentsString = new String(((ByteArrayUint8Array)fileContents).data, StandardCharsets.UTF_8);
    Assert.assertEquals("hello", fileContentsString);
  }


}
