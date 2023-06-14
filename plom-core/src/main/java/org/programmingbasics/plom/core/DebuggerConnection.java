package org.programmingbasics.plom.core;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.client.Browser;
import elemental.events.EventRemover;
import elemental.events.MessageChannel;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.html.IFrameElement;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.Indexable;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * Manages a communication link from the IDE to a Plom program running
 * with a debugger
 */
@JsType
public abstract class DebuggerConnection
{
  public abstract void startConnection();
  public abstract void close();
  
  /**
   * For when a Plom program is run through a service worker providing
   * a virtual web server. This connection relays a MessageChannel
   * through the service worker to the debugger in the Plom program
   */
  public static class ServiceWorkerDebuggerConnection extends DebuggerConnection
  {
    ServiceWorkerDebuggerConnection(IFrameElement iframe)
    {
      this.iframe = iframe;
//      this.targetUrl = targetUrl;
//      this.targetUrl = "*";
    }
    
    IFrameElement iframe;
//    String targetUrl;
    
    MessagePort connectionPort;
    
    // Listens for the initial connection from the Plom window/debugger.
    // Once the connection is made, we no longer need the listener, so
    // it can be removed
    EventRemover windowMessageListener;
    
    @Override public void startConnection()
    {
      // Listen for a connection from the debugger (we have the Plom
      // program connect to us instead of the other way around so as
      // to avoid timing issues with knowing when the Plom program
      // has finished initializing itself)
      windowMessageListener = Browser.getWindow().addEventListener("message", evt -> {
        MessageEvent m = (MessageEvent)evt;
        
        // Check if the message comes from the iframe where we're running the Plom program
        if (!m.getSource().equals(iframe.getContentWindow())) return;
        // Check if the message is what we expect
        if (!Js.isTruthy(m.getData()))
          return;
        JsonObject msgObj = ((JavaScriptObject)m.getData()).cast();
        if (!msgObj.hasKey("type")) return;
        if (!DebuggerEnvironment.INITIAL_ESTABLISH_CONNECTION_STRING.equals(msgObj.getString("type"))) return;
        if (m.getPorts().length() < 1) return;
        connectionPort = (MessagePort)m.getPorts().at(0);
        m.preventDefault();
        Browser.getWindow().getConsole().log("Established");
        if (windowMessageListener != null)
        {
          windowMessageListener.remove();
          windowMessageListener = null;
        }
      }, false);

    }

    @Override public void close()
    {
      if (windowMessageListener != null)
      {
        windowMessageListener.remove();
        windowMessageListener = null;
      }
    }
    
  }
}
