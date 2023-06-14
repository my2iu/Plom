package org.programmingbasics.plom.core;

import elemental.client.Browser;
import elemental.events.MessageChannel;
import elemental.events.MessagePort;
import elemental.html.IFrameElement;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.Indexable;
import jsinterop.annotations.JsType;

/**
 * Manages a communication link from the IDE to a Plom program running
 * with a debugger
 */
@JsType
public abstract class DebuggerConnection
{
  public abstract void connect();
  public abstract void close();
  
  /**
   * For when a Plom program is run through a service worker providing
   * a virtual web server. This connection relays a MessageChannel
   * through the service worker to the debugger in the Plom program
   */
  public static class ServiceWorkerDebuggerConnection extends DebuggerConnection
  {
    ServiceWorkerDebuggerConnection(IFrameElement iframe, String targetUrl)
    {
      this.iframe = iframe;
      this.targetUrl = targetUrl;
      this.targetUrl = "*";
    }
    
    IFrameElement iframe;
    String targetUrl;
    
    MessagePort connectionPort;
    
    @Override public void connect()
    {
      // Create a message channel for more secure, less busy connection
      MessageChannel channel = Browser.getWindow().newMessageChannel();
      connectionPort = channel.getPort2();

      // Send the message channel to the iframe with the Plom program
      JsonObject msgObj = Json.createObject();
      msgObj.put("type", DebuggerEnvironment.INITIAL_ESTABLISH_CONNECTION_STRING);
      ArrayOf<MessagePort> ports = Collections.arrayOf();
      ports.push(channel.getPort1());
      iframe.getContentWindow().postMessage("hey", targetUrl, (Indexable) ports);
    }

    @Override public void close()
    {
      // TODO Auto-generated method stub
      
    }
    
  }
}
