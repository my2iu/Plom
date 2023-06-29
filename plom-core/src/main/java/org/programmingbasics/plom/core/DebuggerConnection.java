package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.LogLevel;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.EventRemover;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.IFrameElement;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
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
    CodeLocationJumper locationJumper = (loc) -> {};
    
    WebHelpers.FixedMessagePort connectionPort;
    
    // Listens for the initial connection from the Plom window/debugger.
    // Once the connection is made, we no longer need the listener, so
    // it can be removed
    EventRemover windowMessageListener;

    public void setCodeLocationJumper(CodeLocationJumper locationJumper)
    {
      this.locationJumper = locationJumper;
    }
    
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
            LogLevel logLevel = LogLevel.from((int)msgObj.getNumber("level"));
            // Read out code position if it's included
            String className = msgObj.getString("class");
            String methodName = msgObj.getString("method");
            boolean isStatic = msgObj.getBoolean("static");
            CodePosition pos = null;
            JsonArray posJson = msgObj.getArray("pos");
            if (posJson != null)
            {
              int [] posArray = new int[posJson.length()];
              for (int n = 0; n < posArray.length; n++)
                posArray[n] = (int)posJson.getNumber(n);
              pos = CodePosition.fromOffsets(posArray);
            }
            ProgramCodeLocation location = null;
            if (className != null || methodName != null || pos != null)
              location = new ProgramCodeLocation(className, methodName, isStatic, pos);
            // Send the message to the screen
            logToConsole(msg, logLevel, location);
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
    
    void logToConsole(String msg, LogLevel logLevel, ProgramCodeLocation codeLocation)
    {
      DivElement msgDiv = Browser.getDocument().createDivElement();
      msgDiv.setClassName("runWebViewConsoleLogEntry");
      Element msgEl;
      // Add extra location information in message string
      // TODO: (perhaps also display line number information)
      if (codeLocation != null)
      {
        String contextString = "";
        if (codeLocation.getClassName() != null)
          contextString += "@" + codeLocation.getClassName();
        if (codeLocation.getFunctionMethodName() != null)
        {
          if (!contextString.isEmpty())
            contextString += " ";
          contextString += "." + codeLocation.getFunctionMethodName();
        }
        msgDiv.setTextContent(!contextString.isEmpty() ? "(" + contextString + ") " : "");
      }

      // Break up message into multiple lines if needed
      boolean isFirstLine = true;
      while (msg.indexOf("\n") >= 0)
      {
        int newLineIdx = msg.indexOf("\n");
        if (!isFirstLine)
          msgDiv.appendChild(Browser.getDocument().createTextNode("\u00a0"));
        msgDiv.appendChild(Browser.getDocument().createTextNode(msg.substring(0, newLineIdx)));
        if (isFirstLine && codeLocation != null)
          msgDiv.appendChild(Browser.getDocument().createTextNode(" \u2192"));
        msgDiv.appendChild(Browser.getDocument().createBRElement());
        isFirstLine = false;
        msg = msg.substring(newLineIdx + 1);
      }
      if (!isFirstLine)
        msgDiv.appendChild(Browser.getDocument().createTextNode("\u00a0"));
      msgDiv.appendChild(Browser.getDocument().createTextNode(msg));
      if (isFirstLine && codeLocation != null)
        msgDiv.appendChild(Browser.getDocument().createTextNode(" \u2192"));
      
      // Clicking an error should jump to the source of the error 
      if (codeLocation != null)
      {
        AnchorElement a = (AnchorElement)Browser.getDocument().createElement("a");
        a.appendChild(msgDiv);
        a.setHref("javascript:void(0)");
        a.setOnclick((evt) -> {
          evt.preventDefault();
          locationJumper.jumpToLocation(codeLocation);
        });
        msgEl = a;
      }
      else
        msgEl = msgDiv;
      consoleDiv.appendChild(msgEl);
    }
  }
  
  /**
   * Function that will be called if the user clicks on errors on 
   * the console in order to jump to different code locations
   */
  @FunctionalInterface
  public static interface CodeLocationJumper
  {
    public void jumpToLocation(ProgramCodeLocation loc);
  }
}
