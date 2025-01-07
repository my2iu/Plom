package org.programmingbasics.plom.core.languageserver;

import elemental.client.Browser;
import elemental.events.MessageEvent;
import jsinterop.annotations.JsType;

/**
 * Language server that runs in a web worker that allows Plom to 
 * do incremental compiles or code completion calculations or other 
 * tasks without blocking the UI thread 
 */

@JsType
public class LanguageServerWorker
{
  public LanguageServerWorker()
  {
    
  }
  
  public void start()
  {
    Browser.getWindow().addEventListener("message", (evt) -> {
      MessageEvent mevt = (MessageEvent)evt;
      
    });
  }
}
