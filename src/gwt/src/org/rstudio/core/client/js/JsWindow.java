package org.rstudio.core.client.js;

public class JsWindow
{
   public static native JsObject getProp(String prop) /*-{
      return $wnd[prop];
   }-*/;
}
