package org.programmingbasics.plom.core;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.EventRemover;
import elemental.events.MessageChannel;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.html.DivElement;
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
    ServiceWorkerDebuggerConnection(IFrameElement iframe, Element consoleDiv)
    {
      this.iframe = iframe;
      this.consoleDiv = consoleDiv;
//      this.targetUrl = targetUrl;
//      this.targetUrl = "*";
    }
    
    IFrameElement iframe;
//    String targetUrl;
    
    Element consoleDiv;
    
    WebHelpers.FixedMessagePort connectionPort;
    
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
        m.preventDefault();
        Browser.getWindow().getConsole().log("Established");

        // No longer need to listen for connection messages from the debugger
        if (windowMessageListener != null)
        {
          windowMessageListener.remove();
          windowMessageListener = null;
        }
        
        // Add a listener to the message channel port to the debugger
        connectionPort = (WebHelpers.FixedMessagePort)m.getPorts().at(0);
        listenMessageChannelPort((MessagePort)m.getPorts().at(0));
        
        // TODO: Send all breakpoints and other debug commands to the Plom program
        // TODO: Send a "RUN" message to the Plom program to get it to start running
      }, false);

    }

    private void listenMessageChannelPort(MessagePort port)
    {
      port.addEventListener("message", evt -> {
        MessageEvent mevt = (MessageEvent)evt;
        JsonObject msgObj = (JsonObject)mevt.getData();

        switch (msgObj.getString("type"))
        {
          case DebuggerEnvironment.MESSAGE_TYPE_LOG:
          {
            String msg = msgObj.getString("msg");
            logToConsole(msg);
            break;
            
          }
        }

      }, false);
      connectionPort.start();
    }
    
    @Override public void close()
    {
      if (windowMessageListener != null)
      {
        windowMessageListener.remove();
        windowMessageListener = null;
      }
      if (connectionPort != null)
      {
        connectionPort.close();
        connectionPort = null;
      }
    }
    
    void logToConsole(String msg)
    {
      DivElement msgDiv = Browser.getDocument().createDivElement();
      msgDiv.setTextContent(msg);
      consoleDiv.appendChild(msgDiv);
    }
  }
}
