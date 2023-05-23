package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.http.client.URL;

import elemental.client.Browser;
import elemental.html.ArrayBuffer;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsType;

/**
 * Version of the ExtraFilesManager that's adapted for a mobile app
 * where there's a bridge interface to native code. This assumes that
 * the bridge is accessed through web calls.
 */
@JsType
public class ExtraFilesManagerBridge implements ExtraFilesManager
{
  public ExtraFilesManagerBridge(String bridgeUrl)
  {
    this.bridgeUrl = bridgeUrl;
  }
  
  String bridgeUrl;
  
  @Override
  public void newFileUi(String pathPrefix, EmptyCallback callback)
  {
    
    WebHelpers.fetch(bridgeUrl + "newFileUi?pathPrefix=" + URL.encodePathSegment(pathPrefix))
      .then((response) -> {
        return response.text();
      }).thenNow((text) -> {
        // Ignore the callback since in Android, we just return a dummy result immediately
        // and just manually refresh the file list from native later on
//        callback.call();
        return null;
      });
//    Main.jsShowFileChooser(null, false, (name, result) -> {
//      if (pathPrefix.endsWith("/")) pathPrefix.substring(0, pathPrefix.length() - 1);
//      insertFile(pathPrefix + "/" + name, (ArrayBuffer)result, callback);
//    });
  }

  @Override
  public void getFileList(FileListCallback callback)
  {
    // TODO: Pass in base paths and rejection filters
    WebHelpers.fetch(bridgeUrl + "listFiles")
      .then((response) -> {
        return response.json();
      }).thenNow((json) -> {
        List<String> filenames = new ArrayList<>();
        JsonArray filesJson = ((JsonObject)json).getArray("files");
        for (int n = 0; n < filesJson.length(); n++)
          filenames.add(filesJson.getString(n));
        callback.call(filenames);
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
    WebHelpers.fetch(bridgeUrl + "getFile?path=" + URL.encodePathSegment(path))
      .then((response) -> {
        return response.arrayBuffer();
      }).thenNow((arrbuf) -> {
        callback.call(arrbuf);
        return null;
      }).catchErrNow((err) -> {
        callback.call(null);
        return null;
      });
  }

  @Override
  public void insertFile(String path, ArrayBuffer data, EmptyCallback callback)
  {
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
}
