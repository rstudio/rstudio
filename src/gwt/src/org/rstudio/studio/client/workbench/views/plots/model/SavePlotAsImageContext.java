package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class SavePlotAsImageContext extends JavaScriptObject
{
   protected SavePlotAsImageContext()
   {
   }
   
   public final native JsArray<SavePlotAsImageFormat> getFormats() /*-{
      return this.formats;
   }-*/;
   
   public final native FileSystemItem getDirectory() /*-{
      return this.directory;
   }-*/;
   
   public final native String getUniqueFileStem() /*-{
      return this.uniqueFileStem;
   }-*/;
}
