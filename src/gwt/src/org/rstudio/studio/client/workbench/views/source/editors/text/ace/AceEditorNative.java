/*
 * AceEditorNative.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
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

   public native final void setHighlightSelectedWord(boolean highlight) /*-{
      this.setHighlightSelectedWord(highlight);
   }-*/;

   public native final void setReadOnly(boolean readOnly) /*-{
      this.setReadOnly(readOnly);
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
      var event = $wnd.require("ace/lib/event");
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

   public static native AceEditorNative createEditor(Element container) /*-{
      var require = $wnd.require;
      var loader = require("rstudio/loader");
      return loader.loadEditor(container);
   }-*/;

   public static <T> HandlerRegistration addEventListener(
         JavaScriptObject target,
         String event,
         CommandWithArg<T> command)
   {
      final JavaScriptObject functor = addEventListenerInternal(target,
                                                                event,
                                                                command);
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
            invokeFunctor(functor);
         }
      };
   }

   private static native <T> JavaScriptObject addEventListenerInternal(
         JavaScriptObject target,
         String eventName,
         CommandWithArg<T> command) /*-{
      var callback = $entry(function(arg) {
         command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(arg);
      });

      target.addEventListener(eventName, callback);
      return function() {
         target.removeEventListener(eventName, callback);
      };
   }-*/;

   private static native void invokeFunctor(JavaScriptObject functor) /*-{
      functor();
   }-*/;

   public final native void scrollToRow(int row) /*-{
      this.scrollToRow(row);
   }-*/;

   public final native void scrollToLine(int line, boolean center) /*-{
      this.scrollToLine(line, center);
   }-*/;

   public final native void autoHeight() /*-{
      var editor = this;
      function updateEditorHeight() {
         editor.container.style.height = (Math.max(1, editor.getSession().getScreenLength()) * editor.renderer.lineHeight) + 'px';
         editor.resize();
         editor.renderer.scrollToY(0);
         editor.renderer.scrollToX(0);
      }
      if (!editor.autoHeightAttached) {
         editor.autoHeightAttached = true;
         editor.getSession().getDocument().on("change", updateEditorHeight);
         editor.renderer.$textLayer.on("changeCharacterSize", updateEditorHeight);
      }
      updateEditorHeight();
   }-*/;

   public final native void onCursorChange() /*-{
      this.onCursorChange();
   }-*/;

   public static native void setInsertMatching(boolean insertMatching) /*-{
      $wnd.require("mode/auto_brace_insert").setInsertMatching(insertMatching);
   }-*/;
}
