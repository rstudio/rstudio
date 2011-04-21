package org.rstudio.studio.client.common.cran.model;

import com.google.gwt.core.client.JavaScriptObject;

public class CRANMirror extends JavaScriptObject
{
   protected CRANMirror()
   {
   }
   
   public final static native CRANMirror empty() /*-{
      var cranMirror = new Object();
      cranMirror.name = "";
      cranMirror.host = "";
      cranMirror.url = "";
      cranMirror.country = "";
      return cranMirror;
   }-*/;
   
   public final boolean isEmpty()
   {
      return getName() == null || getName().length() == 0;
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getHost() /*-{
      return this.host;
   }-*/;

   public final native String getURL() /*-{
      return this.url;
   }-*/;
   
   public final native String getCountry() /*-{
      return this.country;
   }-*/;

   public final String getDisplay()
   {
      return getName()  +" - " + getHost();
   }
}
