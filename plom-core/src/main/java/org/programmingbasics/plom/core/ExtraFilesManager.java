package org.programmingbasics.plom.core;

import java.util.List;

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

  public static interface EmptyCallback
  {
    void call();
  }

  /**
   * Finds all the extra files stored in a project and returns them
   */
  public void getFileList(FileListCallback callback);
  
  public static interface FileListCallback
  {
    void call(List<String> files);
  }
}
