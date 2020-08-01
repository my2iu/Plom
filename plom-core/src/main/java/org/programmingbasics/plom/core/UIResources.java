package org.programmingbasics.plom.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface UIResources extends ClientBundle
{
   public static final UIResources INSTANCE = GWT.create(UIResources.class);

   @Source("codepanel.html")
   TextResource getCodePanelHtml();

   @Source("cursor.html")
   TextResource getCursorHtml();

   @Source("methodpanel.html")
   TextResource getMethodPanelHtml();

   @Source("globalspanel.html")
   TextResource getGlobalsPanelHtml();
   
   @Source("classpanel.html")
   TextResource getClassPanelHtml();
   
   @Source("simpleentrycontents.html")
   TextResource getSimpleEntryContentsHtml();
}
