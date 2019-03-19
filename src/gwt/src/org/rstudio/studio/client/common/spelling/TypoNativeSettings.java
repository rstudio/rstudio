package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Consumer;

class TypoNativeSettings extends JavaScriptObject
{
   protected TypoNativeSettings() {}

   /*
      Since we only care about Async and Async loader for now that's all we're taking.
      The Typo.js interface is a little opaque using JSNI so we are just using a
      Minimum Viable Interface at the moment.
    */
   public static native TypoNativeSettings create(Boolean isAsync, Consumer<TypoNative> asyncCallback)  /*-{
      return {asyncLoad: async, loadedCallback: asyncCallback};
   }-*/;
}
