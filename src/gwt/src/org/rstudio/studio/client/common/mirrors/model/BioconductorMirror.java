package org.rstudio.studio.client.common.mirrors.model;

import com.google.gwt.core.client.JavaScriptObject;

public class BioconductorMirror extends JavaScriptObject
{
   protected BioconductorMirror()
   {
   }
   
   public final static native BioconductorMirror create(String name,
                                                        String url) /*-{
      var mirror = new Object();
      mirror.name = name;
      mirror.url = url;
      return mirror;
   }-*/;

   public final native String getName() /*-{
      return this.name;
   }-*/;
 
   public final native String getURL() /*-{
      return this.url;
   }-*/;
}
