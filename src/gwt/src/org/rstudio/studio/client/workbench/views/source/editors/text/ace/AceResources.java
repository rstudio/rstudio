package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import org.rstudio.core.client.resources.StaticDataResource;

public interface AceResources extends ClientBundle
{
   public static final AceResources INSTANCE = GWT.create(AceResources.class);

   @Source("ace.js")
   StaticDataResource acejs();

   @Source("acesupport.js")
   StaticDataResource acesupportjs();

   @Source("theme.css")
   @NotStrict
   CssResource themecss();
}
