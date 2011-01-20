package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;

public class AceEditorNative extends JavaScriptObject
{
   protected AceEditorNative()
   {}

   public final native void resize() /*-{
      this.resize();
   }-*/;
}
