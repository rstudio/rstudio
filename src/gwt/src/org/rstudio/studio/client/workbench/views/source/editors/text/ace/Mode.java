package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.FunctionStart;

public class Mode extends JavaScriptObject
{
   protected Mode()
   {
   }

   public native final String getCurrentFunction(Position position) /*-{
      if (!this.getCurrentFunction)
         return null;
      return this.getCurrentFunction(position);
   }-*/;

   public native final JsArray<FunctionStart> getFunctionTree() /*-{
      return this.getFunctionTree();
   }-*/;
}
