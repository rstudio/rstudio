package org.rstudio.studio.client.workbench.views.console.shell.editor.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PasteStrategy;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PlainTextEditor;

public class InterceptPasteStrategy implements PasteStrategy
{
   static class ClipboardData extends JavaScriptObject
   {
      public static native ClipboardData fromEvent(NativeEvent evt) /*-{
         return evt.clipboardData || window.clipboardData;
      }-*/;

      protected ClipboardData()
      {
      }

      public native final String getPlainText() /*-{
         return this.getData('Text') || this.getData('text/plain');
      }-*/;

   }

   public void initialize(PlainTextEditor editor, Element textContainer)
   {
      editor_ = editor;
      textContainer_ = textContainer;

      hookPaste(textContainer_);
   }

   private native void hookPaste(Element el) /*-{
      el.onpaste = this.@org.rstudio.studio.client.workbench.views.console.shell.editor.impl.TextareaPasteStrategy::onPaste(Lcom/google/gwt/dom/client/NativeEvent;);
   }-*/;

   @SuppressWarnings("unused")
   private void onPaste(NativeEvent event)
   {
      try
      {
         ClipboardData clipboard = ClipboardData.fromEvent(event);
         if (clipboard != null)
         {
            String plainText = clipboard.getPlainText();
            if (plainText != null)
            {
               event.preventDefault();
               event.stopPropagation();

               editor_.replaceSelection(plainText, true);
            }
         }
      }
      catch (Exception e)
      {
         // If the direct clipboard interaction method doesn't work for
         // some reason, fall back to textarea hack
      }
   }


   private PlainTextEditor editor_;
   private Element textContainer_;
}
