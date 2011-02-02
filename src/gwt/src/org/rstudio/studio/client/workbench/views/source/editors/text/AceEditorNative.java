package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.events.HasNativeKeyHandlers;
import org.rstudio.core.client.events.NativeKeyDownEvent;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.events.NativeKeyPressEvent;
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

   public native final void blur() /*-{
      this.blur();
   }-*/;

   public native final void onChange(Command command) /*-{
      this.getSession().on("change",
              $entry(function () {
                 command.@com.google.gwt.user.client.Command::execute()();
              }));
   }-*/;

   public native final void onKeyDown(HasHandlers handlers) /*-{
      var event = $wnd.require("pilot/event");
      event.addListener(this.textInput.getElement(), "keydown", $entry(function(e) {
         return @org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorNative::fireKeyDown(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;)(e, handlers);
      }));
      event.addListener(this.textInput.getElement(), "keypress", $entry(function(e) {
         return @org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorNative::fireKeyPress(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;)(e, handlers);
      }));
   }-*/;

   @SuppressWarnings("unused")
   private static boolean fireKeyDown(NativeEvent event,
                                      HasHandlers handlers)
   {
      return !NativeKeyDownEvent.fire(event, handlers);
   }

   @SuppressWarnings("unused")
   private static boolean fireKeyPress(NativeEvent event,
                                       HasHandlers handlers)
   {
      return !NativeKeyPressEvent.fire(event, handlers);
   }
}
