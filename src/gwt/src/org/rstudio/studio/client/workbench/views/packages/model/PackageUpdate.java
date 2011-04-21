package org.rstudio.studio.client.workbench.views.packages.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PackageUpdate extends JavaScriptObject
{
   protected PackageUpdate()
   {
   }
   
   public final native String getPackageName() /*-{
      return this.packageName;
   }-*/;
   
   public final native String getLibPath() /*-{
      return this.libPath;
   }-*/;

   public final native String getInstalled() /*-{
      return this.installed;
   }-*/;

   public final native String getAvailable() /*-{
      return this.available;
   }-*/;
   
  
   
   public final native String getNewsUrl() /*-{
      return this.newsUrl;
   }-*/;
}
