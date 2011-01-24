package org.rstudio.studio.client.workbench.views.plots.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ManipulatorContext extends JavaScriptObject
{
   protected ManipulatorContext()
   {
   }
   
   public final native String getCode() /*-{
      return this.codeAsText;
   }-*/;

  

}
