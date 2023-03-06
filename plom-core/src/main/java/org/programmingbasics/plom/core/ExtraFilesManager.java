package org.programmingbasics.plom.core;

import jsinterop.annotations.JsType;

/**
 * Abstracts out operations that can be used to manage the extra
 * files of a project (i.e. web files)
 */
@JsType
public interface ExtraFilesManager
{
  /**
   * Show UI for adding a new file to the project
   */
  public void newFileUi();

}
