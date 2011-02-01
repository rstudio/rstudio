package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Selection extends JavaScriptObject
{
   protected Selection() {}

   public native final Range getRange() /*-{
      return this.getRange();
   }-*/;

   public native final void setSelectionRange(Range range) /*-{
      this.setSelectionRange(range);
   }-*/;

   public native final Position getCursor() /*-{
      return this.getCursor();
   }-*/;

   public native final void moveCursorTo(int row,
                                         int column,
                                         boolean preventUpdateDesiredColumn) /*-{
      this.moveCursorTo(row, column, preventUpdateDesiredColumn);
   }-*/;

}
