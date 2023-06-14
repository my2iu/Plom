package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;

import elemental.client.Browser;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.json.JsonObject;
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
    MessagePort port;
    
    public void startConnection()
    {
      Browser.getWindow().addEventListener("message", evt -> {
        MessageEvent m = (MessageEvent)evt;
        Browser.getWindow().getConsole().log(m.getSource());
        Browser.getWindow().getConsole().log(Browser.getWindow().getParent());
        Browser.getWindow().getConsole().log(m.getSource().equals(Browser.getWindow().getParent()));
        if (!Js.isTruthy(m.getData()))
          return;
        JsonObject msgObj = (JsonObject)m.getData();
        if (!msgObj.hasKey("type")) return;
        if (!INITIAL_ESTABLISH_CONNECTION_STRING.equals(msgObj.getString("type"))) return;
        if (m.getPorts().length() < 1) return;
        port = (MessagePort)m.getPorts().at(0);
        m.preventDefault();
        Browser.getWindow().getConsole().log("Established");
      }, false);
    }
    
    
    @Override public ErrorLogger getErrorLogger()
    {
      return null;
    }


    
  }
}
