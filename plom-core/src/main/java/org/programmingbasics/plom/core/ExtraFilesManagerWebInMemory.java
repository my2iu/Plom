package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import jsinterop.annotations.JsType;

/**
 * Version of the ExtraFilesManager that's suitable for a web IDE
 * where all files are stored in memory.
 * 
 * Tracks extra files for a Plom project by storing all the relevant
 * files in memory (needed for web version of the IDE where there
 * aren't any APIs for accessing files)
 */
@JsType
public class ExtraFilesManagerWebInMemory implements ExtraFilesManager
{
  static class FileInfo
  {
    public String path;
    public ArrayBuffer fileData;
  }
  
  List<FileInfo> files = new ArrayList<>();
  
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
    List<String> filenames = new ArrayList<>();
    for (FileInfo f: files)
      filenames.add(f.path);
    callback.call(filenames);
  }

  @Override
  public void getFileContentsTransferrable(String path, FileContentsCallback callback)
  {
    Optional<FileInfo> fi = files.stream().filter(f -> f.path.equals(path)).findAny();
    if (fi.isPresent())
      callback.call(fi.get().fileData.slice(0));
    else
      callback.call(null);
  }

  @Override
  public void getFileContents(String path, FileContentsCallback callback)
  {
    Optional<FileInfo> fi = files.stream().filter(f -> f.path.equals(path)).findAny();
    if (fi.isPresent())
      callback.call(fi.get().fileData);
    else
      callback.call(null);
  }

  @Override
  public void insertFile(String path, ArrayBuffer data, EmptyCallback callback)
  {
    FileInfo newFile = new FileInfo();
    newFile.path = path;
    newFile.fileData = data;
    
    // Remove any existing file with the same name
    files.removeIf(fi -> fi.path.equals(newFile.path));
    
    // Insert the new file into the file list
    files.add(newFile);
    files.sort(Comparator.comparing((f) -> f.path));

    callback.call();
  }


}
