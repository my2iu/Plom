package org.programmingbasics.plom.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.resources.client.ClientBundle.Source;

public interface UIResources extends ClientBundle
{
   public static final UIResources INSTANCE = GWT.create(UIResources.class);

   @Source("codepanel.html")
   TextResource getCodePanelHtml();

}
