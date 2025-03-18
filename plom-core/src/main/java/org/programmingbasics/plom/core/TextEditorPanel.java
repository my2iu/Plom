package org.programmingbasics.plom.core;

import org.programmingbasics.plom.core.view.SvgCodeRenderer;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.events.Event;
import elemental.html.DivElement;
import elemental.html.TextAreaElement;
import elemental.svg.SVGDocument;
import jsinterop.annotations.JsFunction;

/**
 * UI panel for generic text editing (modifying .html and .js files etc)
 */
public class TextEditorPanel implements AutoCloseable
{
  Document doc = Browser.getDocument();
  DivElement mainDiv;
  CodeRepositoryClient repository; 
  ExitEditorViewCallback exitCallback;
  SvgCodeRenderer.SvgTextWidthCalculator widthCalculator;
  TextAreaElement textArea;
  String fileName;
  String fileContents;
  boolean isModified;
  
  TextEditorPanel(DivElement mainDiv, CodeRepositoryClient repository, ExitEditorViewCallback exitCallback, String fileName, String fileContents)
  {
    this.mainDiv = mainDiv;
    this.repository = repository;
    this.exitCallback = exitCallback;
    this.fileName = fileName;
    this.fileContents = fileContents;
    widthCalculator = new SvgCodeRenderer.SvgTextWidthCalculator((SVGDocument)Browser.getDocument());
    isModified = false;

    rebuildView();
  }
  
  public void rebuildView()
  {
    mainDiv.setInnerHTML(UIResources.INSTANCE.getExtraFilesTextPanelHtml().getText());

    textArea = (TextAreaElement)mainDiv.querySelector("textarea");
    textArea.setValue(fileContents);
    textArea.addEventListener(Event.INPUT, (e) -> {
      isModified = true;
    }, false);
  }

  @JsFunction
  public static interface ExitEditorViewCallback
  {
    void exit();
  }

  public void save()
  {
    if (!isModified)
      return;
    isModified = false;
    
    repository.getExtraFilesManager().insertFile(fileName,
        WebHelpers.encoder.encode(textArea.getValue()).getBuffer(), 
        () -> {});
  }
  
  @Override
  public void close()
  {
  }
  
}
