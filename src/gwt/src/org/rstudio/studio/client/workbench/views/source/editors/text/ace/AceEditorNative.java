package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.CommandWithArg;

import java.util.LinkedList;

public class AceEditorNative extends JavaScriptObject {

   protected AceEditorNative() {}

   public native final EditSession getSession() /*-{
      return this.getSession();
   }-*/;

   public native final Renderer getRenderer() /*-{
      return this.renderer;
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

   public native final void setKeyboardHandler(KeyboardHandler keyboardHandler) /*-{
      this.setKeyboardHandler(keyboardHandler);
   }-*/;

   public native final void onChange(Command command) /*-{
      this.getSession().on("change",
              $entry(function () {
                 command.@com.google.gwt.user.client.Command::execute()();
              }));
   }-*/;

   public final HandlerRegistration delegateEventsTo(HasHandlers handlers)
   {
      final LinkedList<JavaScriptObject> handles = new LinkedList<JavaScriptObject>();
      handles.add(addDomListener(getTextInputElement(), "keydown", handlers));
      handles.add(addDomListener(getTextInputElement(), "keypress", handlers));
      handles.add(addDomListener(this.<Element>cast(), "focus", handlers));
      handles.add(addDomListener(this.<Element>cast(), "blur", handlers));

      return new HandlerRegistration()
      {
         public void removeHandler()
         {
            while (!handles.isEmpty())
               removeDomListener(handles.remove());
         }
      };
   }

   private native Element getTextInputElement() /*-{
      return this.textInput.getElement();
   }-*/;

   private native static JavaScriptObject addDomListener(
         Element element,
         String eventName,
         HasHandlers hasHandlers) /*-{
      var event = $wnd.require("pilot/event");
      var listener = $entry(function(e) {
         @com.google.gwt.event.dom.client.DomEvent::fireNativeEvent(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;Lcom/google/gwt/dom/client/Element;)(e, hasHandlers, element);
      }); 
      event.addListener(element, eventName, listener);
      return $entry(function() {
         event.removeListener(element, eventName, listener);
      });
   }-*/;

   private native static void removeDomListener(JavaScriptObject handle) /*-{
      handle();
   }-*/;

   public static native void createEnvironment(
         CommandWithArg<JavaScriptObject> callback) /*-{
      var require = $wnd.require;

      var config = {
          paths: {
              ace: "../lib/ace",
              pilot: "../support/pilot/lib/pilot",
              mode: "../../js/acemode",
              theme: "../../js/acetheme"
          }
      };

      var deps = [ "pilot/fixoldbrowsers",
                   "pilot/plugin_manager",
                   "pilot/settings",
                   "pilot/environment",
                   "mode/r",
                   "mode/tex",
                   "mode/sweave",
                   "theme/default"];

      require(config);
      require(deps, $entry(function() {
          var catalog = require("pilot/plugin_manager").catalog;
          catalog.registerPlugins([ "pilot/index" ]).then($entry(function() {
              var env = require("pilot/environment").create();
              catalog.startupPlugins({ env: env }).then($entry(function() {
                  callback.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(env);
              }));
          }));
      }));
   }-*/;

   public static native AceEditorNative createEditor(
         JavaScriptObject env,
         Element container) /*-{
      var require = $wnd.require;
      var Editor = require("ace/editor").Editor;
      var Renderer = require("ace/virtual_renderer").VirtualRenderer;
      var UndoManager = require("ace/undomanager").UndoManager;

      var TextMode = require("ace/mode/text").Mode;
      var theme = require("theme/default");

      env.editor = new Editor(new Renderer(container, theme));
      var session = env.editor.getSession();
      session.setMode(new TextMode());
      session.setUndoManager(new UndoManager());
      session.setUseSoftTabs(true);
      session.setTabSize(2);

      // We handle these commands ourselves.
      var canon = require("pilot/canon");
      canon.removeCommand("findnext");
      canon.removeCommand("findprevious");
      canon.removeCommand("find");
      canon.removeCommand("replace");
      canon.removeCommand("togglecomment");
      canon.removeCommand("gotoline");

      return env.editor;
   }-*/;
}
