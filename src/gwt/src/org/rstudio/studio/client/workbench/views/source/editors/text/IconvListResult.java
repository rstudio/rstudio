package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class IconvListResult extends JavaScriptObject
{
   protected IconvListResult()
   {
   }

   public native final JsArrayString getCommon() /*-{
      return this.common;
   }-*/;

   public native final JsArrayString getAll() /*-{
      return this.all;
   }-*/;
}
