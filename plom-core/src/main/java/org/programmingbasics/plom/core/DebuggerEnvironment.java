package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.LogLevel;

import elemental.client.Browser;
import elemental.events.MessageChannel;
import elemental.events.MessageEvent;
import elemental.events.MessagePort;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.Indexable;
import jsinterop.annotations.JsType;

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
    ServiceWorkerDebuggerEnvironment(String targetOriginUrl, String id)
    {
      this.targetOriginUrl = targetOriginUrl;
      this.id = id;
    }
    
    /**
     * url showing the origin of the IDE window where a debug
     * connection should be made to (just a security measure to make 
     * sure that debug messages go to the right place).
     */
    String targetOriginUrl;

    /**
     * ID string used to identify ourselves to the debugger
     */
    String id;
    
    ErrorLogger errorLogger = new ErrorLogger() {
      private void logErr(Object err, LogLevel logLevel, ProgramCodeLocation location)
      {
        String msgString;
        if (err instanceof ParseException)
        {
          ParseException parseErr = (ParseException)err;
//          int lineNo = lineNumbers.tokenLine.getOrDefault(parseErr.token, 0);
//          if (lineNo == 0)
            log("Syntax Error", logLevel, location);
//          else
//            msgString = "Syntax Error (line " + lineNo + ")";
        }
        else if (err instanceof RunException)
        {
          RunException runErr = (RunException)err;
          Token errTok = runErr.getErrorTokenSource();
          int lineNo = 0;
          String errString = gatherChainedErrorMessages(runErr);
          if (errString.isEmpty())
            errString = "Run Error";
//          if (errTok != null) 
//            lineNo = lineNumbers.tokenLine.getOrDefault(errTok, 0);
//          if (lineNo == 0)
//          else
//            msgString = errString + " (line " + lineNo + ")";
          if (location == null)
            location = runErr.getErrorLocation();
          log(errString, logLevel, location);
        }
        else if (err instanceof Throwable && ((Throwable)err).getMessage() != null && !((Throwable)err).getMessage().isEmpty())
        {
          log(((Throwable)err).getMessage(), logLevel, location);
        }
        else
        {
          log(err.toString(), logLevel, location);
        }
      }
      @Override public void warn(Object err)
      {
        logErr(err, WARN, null);
      }
      @Override public void error(Object err, ProgramCodeLocation location)
      {
        logErr(err, ERROR, location);
      }
      String gatherChainedErrorMessages(Throwable e)
      {
        String toReturn = e.getMessage();
        if (toReturn == null || toReturn.isEmpty())
          return "";
        if (e.getCause() != null)
        {
          String chainedMessage = gatherChainedErrorMessages(e.getCause()); 
          if (!chainedMessage.isEmpty())
          toReturn += "\n" + chainedMessage;
        }
        return toReturn;
      }
      @Override public void debugLog(Object value)
      {
        log(value.toString(), DEBUG, null);
      }
      @Override public void log(String value, LogLevel logLevel, ProgramCodeLocation location)
      {
        Browser.getWindow().getConsole().log(value);
        if (port != null)
        {
          JsonObject msg = Json.createObject();
          msg.put("type", MESSAGE_TYPE_LOG);
          msg.put("msg", value.toString());
          msg.put("level", logLevel.getLevel());
          if (location != null)
          {
            if (location.getClassName() != null)
              msg.put("class", location.getClassName());
            if (location.getFunctionMethodName() != null)
              msg.put("method", location.getFunctionMethodName());
            if (location.getClassName() != null && location.getFunctionMethodName() != null)
              msg.put("static", location.isStatic());
            if (location.getPosition() != null)
            {
              CodePosition pos = location.getPosition();
              JsonArray posArray = Json.createArray();
              for (int n = 0; pos.hasOffset(n); n++)
                posArray.set(n, pos.getOffset(n));
              msg.put("pos", posArray);
            }
          }
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
      msgObj.put("id", id);
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
