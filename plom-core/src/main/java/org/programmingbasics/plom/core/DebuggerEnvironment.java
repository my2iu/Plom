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
  static final String INITIAL_ESTABLISH_CONNECTION_STRING = "Plom debug setup";
  
  static final String MESSAGE_TYPE_LOG = "LOG";

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
    
    /**
     * url showing the origin of the IDE window where a debug
     * connection should be made to (just a security measure to make 
     * sure that debug messages go to the right place).
     */
    String targetOriginUrl;

    
    ErrorLogger errorLogger = new ErrorLogger() {
      @Override public void error(Object errObj)
      {
        Browser.getWindow().getConsole().log(errObj);
        if (port != null)
        {
          JsonObject msg = Json.createObject();
          msg.put("type", MESSAGE_TYPE_LOG);
          msg.put("msg", errObj.toString());
          port.postMessage(msg);
        }
      }
      @Override public void log(Object value)
      {
        Browser.getWindow().getConsole().log(value);
        if (port != null)
        {
          JsonObject msg = Json.createObject();
          msg.put("type", MESSAGE_TYPE_LOG);
          msg.put("msg", value.toString());
          port.postMessage(msg);
        }
      }

    };
    
    WebHelpers.FixedMessagePort port;
    
    public void startConnection()
    {
      // Create a message channel for more secure, less busy connection
      MessageChannel channel = Browser.getWindow().newMessageChannel();
      port = (WebHelpers.FixedMessagePort)channel.getPort2();
      listenMessageChannelPort(channel.getPort2());

      // Send the message channel to the parent of the iframe (presumably, 
      // it's the IDE)
      JsonObject msgObj = Json.createObject();
      msgObj.put("type", INITIAL_ESTABLISH_CONNECTION_STRING);
      ArrayOf<MessagePort> ports = Collections.arrayOf();
      ports.push(channel.getPort1());
      Browser.getWindow().getParent().postMessage(msgObj, targetOriginUrl, (Indexable) ports);
      
      // TODO: Wait until the debugger makes a connection so that we can 
      // get debug commands first (e.g. should we wait before executing 
      // anything etc)
    }
    
    private void listenMessageChannelPort(MessagePort port)
    {
      port.addEventListener("message", evt -> {
        MessageEvent mevt = (MessageEvent)evt;
        JsonObject msgObj = (JsonObject)mevt.getData();


      }, false);
      port.start();
    }

    
    @Override public ErrorLogger getErrorLogger()
    {
      return errorLogger;
    }


    
  }
}
