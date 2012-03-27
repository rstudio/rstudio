package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class FoldingRules extends JavaScriptObject
{
   protected FoldingRules()
   {}

   public native final Range getFoldWidgetRange(EditSession session,
                                                String foldStyle,
                                                int row) /*-{
      return this.getFoldWidgetRange(session, foldStyle, row) || null;
   }-*/;
}
