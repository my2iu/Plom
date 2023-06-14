package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;

import elemental.client.Browser;
import elemental.events.MessageChannel;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.Indexable;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * Keeps track of debugger stuff and interfaces with an IDE to exchange
 * debug information. The main class should maybe be moved to the
 * interpreter package
 */

@JsType
public abstract class DebuggerEnvironment
{
  public static final String INITIAL_ESTABLISH_CONNECTION_STRING = "Plom debug setup";

  public abstract void startConnection();
  public abstract ErrorLogger getErrorLogger();
  
  /**
   * Debugger environment for a Plom program that is being run through
   * a virtual web server provided by a Service Worker. All debug 
   * information will be relayed through this service worker back to
   * the IDE.
   */
  public static class ServiceWorkerDebuggerEnvironment extends DebuggerEnvironment
  {
    ServiceWorkerDebuggerEnvironment(String targetOriginUrl)
    {
      this.targetOriginUrl = targetOriginUrl;
    }
    String targetOriginUrl;
    
    MessagePort port;
    
    public void startConnection()
    {
      // Create a message channel for more secure, less busy connection
      MessageChannel channel = Browser.getWindow().newMessageChannel();
      port = channel.getPort2();

      // Send the message channel to the parent of the iframe (presumably, 
      // it's the IDE)
      JsonObject msgObj = Json.createObject();
      msgObj.put("type", DebuggerEnvironment.INITIAL_ESTABLISH_CONNECTION_STRING);
      ArrayOf<MessagePort> ports = Collections.arrayOf();
      ports.push(channel.getPort1());
      Browser.getWindow().getParent().postMessage(msgObj, targetOriginUrl, (Indexable) ports);
    }
    
    
    @Override public ErrorLogger getErrorLogger()
    {
      return null;
    }


    
  }
}
