package org.rstudio.studio.client.workbench.views.plots.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SavePlotAsImageFormat extends JavaScriptObject
{  
   protected SavePlotAsImageFormat()
   {
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getExtension() /*-{
      return this.extension;
   }-*/;
}
