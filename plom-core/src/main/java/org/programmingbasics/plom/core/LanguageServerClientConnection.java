package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.codestore.CodeRepositoryMessages;

import elemental.html.Worker;
import jsinterop.annotations.JsType;

@JsType
public class LanguageServerClientConnection
{
  Worker worker;
  public LanguageServerClientConnection(Worker worker)
  {
    this.worker = worker;
  }
  
  public void sendImportStdLibRepository()
  {
    CodeRepositoryMessages.BaseMessage msg = CodeRepositoryMessages.createBaseMessage("importStdLib");
    worker.postMessage(msg);
  }

  public void loadModule(String codeStr)
  {
    worker.postMessage(CodeRepositoryMessages.createLoadModuleMessage(codeStr));
  }
}
