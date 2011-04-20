package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PackageUpdate extends JavaScriptObject
{
   protected PackageUpdate()
   {
   }
   
   public final native String getPackageName() /*-{
      return this.Package;
   }-*/;

   public final native String getInstalled() /*-{
      return this.Installed;
   }-*/;

   public final native String getAvailable() /*-{
      return this.ReposVer;
   }-*/;

  
}
