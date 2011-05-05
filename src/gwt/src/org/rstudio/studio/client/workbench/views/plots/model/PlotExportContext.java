package org.rstudio.studio.client.workbench.views.plots.model;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class PlotExportContext extends JavaScriptObject
{
   protected PlotExportContext()
   {
   }
   
   public final native JsArrayString getFormats() /*-{
      return this.formats;
   }-*/;
   
   public final native FileSystemItem getDirectory() /*-{
      return this.directory;
   }-*/;
   
   public final native String getFilename() /*-{
      return this.filename;
   }-*/;
}
