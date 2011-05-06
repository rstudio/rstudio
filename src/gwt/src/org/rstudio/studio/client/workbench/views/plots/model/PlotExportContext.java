package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class PlotExportContext extends JavaScriptObject
{
   protected PlotExportContext()
   {
   }
   
   public final native JsArray<PlotExportFormat> getFormats() /*-{
      return this.formats;
   }-*/;
   
   public final native FileSystemItem getDirectory() /*-{
      return this.directory;
   }-*/;
   
   public final native String getFilename() /*-{
      return this.filename;
   }-*/;
}
