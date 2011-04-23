package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class PackageInstallContext extends JavaScriptObject
{
   protected PackageInstallContext()
   {
   }
   
   public final native boolean isCRANMirrorConfigured() /*-{
      return this.cranMirrorConfigured[0];
   }-*/;

   public final native String getDefaultLibraryPath() /*-{
      return this.defaultLibraryPath[0];
   }-*/;

   public final native boolean isDefaultLibraryWriteable() /*-{
      return this.defaultLibraryWriteable[0];
   }-*/;
   
   public final native JsArrayString getWriteableLibraryPaths() /*-{
      return this.writeableLibraryPaths;
   }-*/;
   
   public final native String getDefaultUserLibraryPath() /*-{
      return this.defaultUserLibraryPath[0];
   }-*/;
}
