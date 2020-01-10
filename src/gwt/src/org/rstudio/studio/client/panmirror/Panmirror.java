package org.rstudio.studio.client.panmirror;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;

import com.google.gwt.core.client.JavaScriptObject;


public class Panmirror extends JavaScriptObject
{
   public static void get(CommandWithArg<Panmirror> onReady) {    
      panmirrorLoader_.addCallback(() -> {
         onReady.execute(getPanmirror());
      });
   }
   
   protected Panmirror()
   {
   }
   
   final static native Panmirror getPanmirror()  /*-{ return $wnd.panmirror;  }-*/;
   
   private static final ExternalJavaScriptLoader panmirrorLoader_ =
      new ExternalJavaScriptLoader("js/panmirror/panmirror.js");
       
}
