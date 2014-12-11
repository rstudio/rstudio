package org.rstudio.studio.client.server.remote;

import com.google.gwt.core.client.JavaScriptObject;

public class RResult<T> extends JavaScriptObject
{
   protected RResult()
   {
   }
   
   public final native boolean failed() /*-{
      return this.message != null;
   }-*/;
   
   public final native String errorMessage() /*-{
      return "" + this.message;
   }-*/;
   
   public final native T get() /*-{
      return this.result;
   }-*/;
}
