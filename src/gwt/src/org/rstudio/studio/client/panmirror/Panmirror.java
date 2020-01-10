package org.rstudio.studio.client.panmirror;

import org.rstudio.core.client.ExternalJavaScriptLoader;

import jsinterop.annotations.JsType;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;;



@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Panmirror {

   @JsOverlay
   public static void load(ExternalJavaScriptLoader.Callback onLoaded) {    
      panmirrorLoader_.addCallback(onLoaded);
   }
   
   public static String kEventUpdate;
   public static String kEventSelectionChange;
   
   
   
   @JsOverlay
   private static final ExternalJavaScriptLoader panmirrorLoader_ =
     new ExternalJavaScriptLoader("js/panmirror/panmirror.js");
   
}
