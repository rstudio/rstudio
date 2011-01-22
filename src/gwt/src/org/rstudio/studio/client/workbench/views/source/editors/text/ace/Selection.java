package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Selection extends JavaScriptObject
{
   protected Selection() {}

   public native final Range getRange() /*-{
      return this.getRange();
   }-*/;
}
