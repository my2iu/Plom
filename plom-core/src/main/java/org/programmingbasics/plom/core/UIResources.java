package org.programmingbasics.plom.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface UIResources extends ClientBundle
{
   public static final UIResources INSTANCE = GWT.create(UIResources.class);

   @Source("stdlib.plom")
   TextResource getStdLibPlom();
   
   @Source("codepanel.html")
   TextResource getCodePanelHtml();

   @Source("svgcodepanel.html")
   TextResource getSvgCodePanelHtml();

   @Source("cursor.html")
   TextResource getCursorHtml();

   @Source("methodpanel.html")
   TextResource getMethodPanelHtml();

   @Source("methodnamebase.html")
   TextResource getMethodNameBaseHtml();

   @Source("methodnamefirstarg2.html")
   TextResource getMethodNameFirstArgument2Html();

   @Source("methodnamepart2.html")
   TextResource getMethodNamePart2Html();

   @Source("globalspanel.html")
   TextResource getGlobalsPanelHtml();
   
   @Source("classpanel.html")
   TextResource getClassPanelHtml();
   
   @Source("simpleentrycontents.html")
   TextResource getSimpleEntryContentsHtml();

   @Source("numberentrycontents.html")
   TextResource getNumberEntryContentsHtml();

   @Source("autoresizinginput.html")
   TextResource getAutoResizingInputHtml();
   
   @Source("extrafilestextpanel.html")
   TextResource getExtraFilesTextPanelHtml();

}
