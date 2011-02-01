package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class EditSession extends JavaScriptObject
{
   protected EditSession() {}

   public native final String getValue() /*-{
      return this.toString();
   }-*/;

   public native final void setValue(String code) /*-{
      this.setValue(code);
   }-*/;

   public native final void insert(Position position, String text) /*-{
      this.insert(position, text);
   }-*/;

   public native final Selection getSelection() /*-{
      return this.getSelection();
   }-*/;

   public native final Position replace(Range range, String text) /*-{
      return this.replace(range, text);
   }-*/;

   public native final String getTextRange(Range range) /*-{
      return this.getTextRange(range);
   }-*/;

   public native final String getLine(int row) /*-{
      return this.getLine(row);
   }-*/;

   public native final void setUseWrapMode(boolean useWrapMode) /*-{
      return this.setUseWrapMode(useWrapMode);
   }-*/;

   /**
    * Number of rows
    */
   public native final int getLength() /*-{
      return this.getLength();
   }-*/;
}
