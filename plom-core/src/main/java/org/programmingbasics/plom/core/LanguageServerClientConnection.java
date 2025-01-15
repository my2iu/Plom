package org.programmingbasics.plom.core;

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
}
