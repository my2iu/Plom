package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.programmingbasics.plom.core.WebHelpers.FileSystemHandleKind;
import org.programmingbasics.plom.core.WebHelpers.PromiseClass;

import elemental.html.ArrayBuffer;
import elemental.util.ArrayOf;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/**
 * Version of the ExtraFilesManager that's adapted for the File
 * System Access API that allows a standard web page to read/write
 * directly from the file system.
 */
@JsType
public class ExtraFilesManagerFileSystemAccessApi implements ExtraFilesManager
{
  public ExtraFilesManagerFileSystemAccessApi(WebHelpers.FileSystemDirectoryHandle baseDirHandle)
  {
    this.baseDirHandle = baseDirHandle;
  }
  
  WebHelpers.FileSystemDirectoryHandle baseDirHandle;
  
  @JsFunction
  static interface WriteFile {
    public WebHelpers.Promise<Void> writeFile(String name, ArrayBuffer data);
  }
  
  @Override
  public void newFileUi(String pathPrefix, EmptyCallback callback)
  {
    Main.jsShowFileChooser(null, false, (name, result) -> {
      if (pathPrefix.endsWith("/")) pathPrefix.substring(0, pathPrefix.length() - 1);
      insertFile(pathPrefix + "/" + name, (ArrayBuffer)result, callback);
    });
  }

  @Override
  public void getFileList(FileListCallback callback)
  {
    // TODO: Pass in base paths and rejection filters
    listProjectFiles("", Arrays.<String>asList("[.].*", "src/")).thenNow((results) -> {
      callback.call(results);
      return null;
    });
  }

  @Override
  public void getFileContentsTransferrable(String path, FileContentsCallback callback)
  {
    // getFileContents() returns a new array buffer each time it is called
    // when using the bridge
    getFileContents(path, callback);
  }

  @Override
  public void getFileContents(String path, FileContentsCallback callback)
  {
    getProjectPathHandle(path).thenNow((handle) -> {
      if (handle.getKindEnum() == WebHelpers.FileSystemHandleKind.FILE)
      {
        return ((WebHelpers.FileSystemFileHandle)handle).getFile()
            .then((file) -> WebHelpers.readFileAsArrayBuffer(file))
            .thenNow((arrbuf) -> {
              callback.call(arrbuf);
              return null;
            }).catchErrNow((err) -> {
              callback.call(null);
              return null;
            });
      }
      throw new IllegalArgumentException("Path does not point to a file");
    });
  }

  @Override
  public void insertFile(String path, ArrayBuffer data, EmptyCallback callback)
  {
    throw new IllegalArgumentException("File insertion not implemented yet");
//    FileInfo newFile = new FileInfo();
//    newFile.path = path;
//    newFile.fileData = data;
//    
//    // Remove any existing file with the same name
//    files.removeIf(fi -> fi.path.equals(newFile.path));
//    
//    // Insert the new file into the file list
//    files.add(newFile);
//    files.sort(Comparator.comparing((f) -> f.path));
//
//    callback.call();
  }
  
  /** Gets the FileSystemHandle of the document/directory that represents a certain path
   * inside a project. Returns null if the path could not be traversed.
   */
  WebHelpers.Promise<WebHelpers.FileSystemHandle> getProjectPathHandle(String path)
  {
    WebHelpers.FileSystemDirectoryHandle projectFile = baseDirHandle;
    return getProjectPathHandle(projectFile, path);
  }

  WebHelpers.Promise<WebHelpers.FileSystemHandle> getProjectPathHandle(WebHelpers.FileSystemDirectoryHandle baseHandle, String path)
  {
    WebHelpers.FileSystemDirectoryHandle projectHandle = baseHandle;
    String base = path;
    // Find the base from which we want to gather files
    String baseNextDir;
    String leftover;
    if (base.indexOf("/") >= 0)
    {
      baseNextDir = base.substring(0, base.indexOf("/"));
      leftover = base.substring(base.indexOf("/") + 1);
    }
    else
    {
      baseNextDir = base;
      leftover = "";
    }
    if (!baseNextDir.isEmpty())
    {
      return Main.asyncIteratorToArray.gather(projectHandle.values()).thenNow((handles) -> {
        for (int n = 0; n < handles.length(); n++)
        {
          if (baseNextDir.equals(handles.get(n).getName()))
            return handles.get(n);
        }
        throw new IllegalArgumentException("Path not found");
      }).then((nextHandle) -> {
        if (!leftover.isEmpty())
        {
          if (nextHandle.getKindEnum() == WebHelpers.FileSystemHandleKind.DIRECTORY)
          {
            return getProjectPathHandle((WebHelpers.FileSystemDirectoryHandle)nextHandle, leftover); 
          }
        }
        else
          return PromiseClass.resolve(nextHandle);
        throw new IllegalArgumentException("Path not found");
      });
    }
    return WebHelpers.PromiseClass.resolve(projectHandle);
  }

  WebHelpers.Promise<List<String>> listProjectFiles(String base, List<String> rejectList)
  {
    WebHelpers.Promise<WebHelpers.FileSystemHandle> projectFile = getProjectPathHandle(base);
    // Start searching for files
    return projectFile.then((handle) -> {
      List<String> results = new ArrayList<>();
      String fixedBase = base;
      if (!base.endsWith("/") && !base.isEmpty())
        fixedBase = base + "/";
      if (handle.getKindEnum() == WebHelpers.FileSystemHandleKind.DIRECTORY)
        return listProjectFiles((WebHelpers.FileSystemDirectoryHandle)handle, fixedBase, rejectList, results).thenNow(ignored -> results);
      return WebHelpers.PromiseClass.resolve(results);
    });
  }

  private WebHelpers.Promise<Void> listProjectFiles(WebHelpers.FileSystemDirectoryHandle projectFile, String basePath, List<String> rejectList, List<String> results)
  {
    for (String rejectPattern : rejectList)
    {
      if (basePath.matches(rejectPattern))
        return PromiseClass.resolve(null);
    }
    return Main.asyncIteratorToArray.gather(projectFile.values()).then((handles) -> {
      return listProjectFiles_scanDir(projectFile, basePath, rejectList, results, handles, 0);
    });
  }

  private WebHelpers.Promise<Void> listProjectFiles_scanDir(WebHelpers.FileSystemDirectoryHandle projectFile, String basePath, List<String> rejectList, List<String> results, ArrayOf<WebHelpers.FileSystemHandle> handles, int idx)
  {
    if (idx >= handles.length())
      return PromiseClass.resolve(null);
    WebHelpers.FileSystemHandle f = handles.get(idx);
    if (f.getKindEnum() == FileSystemHandleKind.DIRECTORY)
    {
      return listProjectFiles((WebHelpers.FileSystemDirectoryHandle)f, basePath + f.getName() + "/", rejectList, results).then((ignored) -> {
        return listProjectFiles_scanDir(projectFile, basePath, rejectList, results, handles, idx + 1);
      });
    }
    else if (f.getKindEnum() == FileSystemHandleKind.FILE)
    {
      boolean reject = false;
      String filePath = basePath + f.getName();
      for (String rejectPattern : rejectList)
      {
        if (filePath.matches(rejectPattern))
        {
          reject = true;
          break;
        }
      }
      if (!reject)
        results.add(filePath);
      return listProjectFiles_scanDir(projectFile, basePath, rejectList, results, handles, idx + 1);
    }
    throw new IllegalArgumentException("Unknown file system handle kind");
  }

}
