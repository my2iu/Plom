package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.ExtraFilesManager.EmptyCallback;

import elemental.html.ArrayBuffer;
import elemental.html.Uint8Array;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/**
 * Abstracts out operations that can be used to manage the extra
 * files of a project (i.e. web files)
 */
@JsType
public interface ExtraFilesManager
{
  /**
   * Show UI for adding a new file to the project. pathPrefix is the
   * path where new files should be added
   */
  public void newFileUi(String pathPrefix, EmptyCallback callback);

  @JsFunction
  public static interface EmptyCallback
  {
    void call();
  }

  /**
   * Finds all the extra files stored in a project and returns them
   */
  public void getFileList(FileListCallback callback);
  
  @JsFunction
  public static interface FileListCallback
  {
    void call(List<String> files);
  }
  
  /**
   * Reads a file and return its contents. The ArrayBuffer containing the file
   * contents should be transferrable to another thread
   */
  public void getFileContentsTransferrable(String path, FileContentsCallback callback);

  /**
   * Reads a file and return its contents. 
   */
  public void getFileContents(String path, FileContentsCallback callback);

  /**
   * Inserts data directly into the files list
   */
  public void insertFile(String path, ArrayBuffer data, EmptyCallback callback);
  
  @JsFunction
  public static interface FileContentsCallback
  {
    void call(ArrayBuffer contents);
  }
}
