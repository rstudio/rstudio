/*
 * InterceptPasteStrategy.java
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
      var thiz = this;
      el.onpaste = function (e) {
         e = e || $wnd.event;
         thiz.@org.rstudio.studio.client.workbench.views.console.shell.editor.impl.InterceptPasteStrategy::onPaste(Lcom/google/gwt/dom/client/NativeEvent;)(e);
      };
   }-*/;

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
