package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class FunctionStart extends JavaScriptObject
{
   protected FunctionStart()
   {}

   public native final String getLabel() /*-{
      return this.label;
   }-*/;

   public native final Position getStart() /*-{
      return this.start;
   }-*/;

   public native final JsArray<FunctionStart> getChildren() /*-{
      return this.children;
   }-*/;

}
