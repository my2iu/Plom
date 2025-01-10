package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.GlobalsPanel.LoadClassViewCallback;
import org.programmingbasics.plom.core.GlobalsPanel.LoadFunctionCodeViewCallback;
import org.programmingbasics.plom.core.GlobalsPanel.LoadFunctionSigViewCallback;
import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.DivElement;
import elemental.svg.SVGDocument;

public class FileViewerPanel implements AutoCloseable
{
  Document doc = Browser.getDocument();
  CodeRepositoryClient repository;
  DivElement mainDiv;

  public FileViewerPanel(DivElement mainDiv, CodeRepositoryClient repository)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
//    this.viewSwitchCallback = callback;
//    this.functionSigCallback = functionSigCallback;
//    this.classViewCallback = classViewCallback;
//    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());
    rebuildView();
  }
  
  void rebuildView()
  {
    Document doc = Browser.getDocument();
    mainDiv.setInnerHTML(UIResources.INSTANCE.getGlobalsPanelHtml().getText());
  }
  
  @Override public void close()
  {
  }
}
