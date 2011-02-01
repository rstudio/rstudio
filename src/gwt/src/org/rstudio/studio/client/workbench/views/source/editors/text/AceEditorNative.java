package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;

public class AceEditorNative extends JavaScriptObject {

   protected AceEditorNative() {}

   public native final EditSession getSession() /*-{
      return this.getSession();
   }-*/;

   public native final void resize() /*-{
      this.resize();
   }-*/;

   public native final void setShowPrintMargin(boolean show) /*-{
      this.setShowPrintMargin(show);
   }-*/;

   public native final void setPrintMarginColumn(int column) /*-{
      this.setPrintMarginColumn(column);
   }-*/;

   public native final void setHighlightActiveLine(boolean highlight) /*-{
      this.setHighlightActiveLine(highlight);
   }-*/;

   public native final void focus() /*-{
      this.focus();
   }-*/;

   public native final void onChange(Command command) /*-{
      this.getSession().on("change",
              $entry(function () {
                 command.@com.google.gwt.user.client.Command::execute()();
              }));
   }-*/;
}
