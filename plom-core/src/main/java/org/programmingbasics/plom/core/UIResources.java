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

   @Source("methodnamepartarg.html")
   TextResource getMethodNamePartForArgumentHtml();

   @Source("globalspanel.html")
   TextResource getGlobalsPanelHtml();
   
   @Source("classpanel.html")
   TextResource getClassPanelHtml();
   
   @Source("simpleentrycontents.html")
   TextResource getSimpleEntryContentsHtml();
   
   @Source("autoresizinginput.html")
   TextResource getAutoResizingInputHtml();
}
