/*
 * AceKeyboardPreviewer.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;

import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.KeyboardHandler;

public class AceKeyboardPreviewer
{
   public interface Handler 
   {
      boolean previewKeyDown(JavaScriptObject data, NativeEvent event);
      boolean previewKeyPress(JavaScriptObject data, char charCode);
   }
   
   // NOTE: The 'previewKeyDown()' handler exposed here wraps a 'true' DOM
   // KeyDown event, while the 'previewKeyPress()' handler handles a synthetic
   // Ace event used for text input. In particular, 'previewKeyPress()' can be
   // used to figure out the 'true' character input by the user; for example, if
   // the user were holding Alt and pressed 'A' on macOS, they would get the
   // 'å' character inserted. Nowadays, most browsers provide these keys
   // as part of the 'key' attribute of the event, but that was not always the case.
   public AceKeyboardPreviewer(final CompletionManager completionManager)
   {
      addHandler(new Handler() {

         @Override
         public boolean previewKeyDown(JavaScriptObject data, NativeEvent event)
         {
            return completionManager.previewKeyDown(event);
         }

         @Override
         public boolean previewKeyPress(JavaScriptObject data, char charCode)
         {
            return completionManager.previewKeyPress(charCode);
         }
      });
   }
   
   public void addHandler(Handler handler)
   {
      handlers_.add(handler);
   }
   
 
   public native final KeyboardHandler getKeyboardHandler() /*-{
      var event = $wnd.require("ace/lib/event");
      var self = this;
      var noop = {command: "null"};
      return {
         handleKeyboard: $entry(function(data, hashId, keyOrText, keyCode, e) {
            if (hashId != -1 || keyCode) {
               if (self.@org.rstudio.studio.client.workbench.views.source.editors.text.AceKeyboardPreviewer::onKeyDown(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/dom/client/NativeEvent;)(data, e)) {
                  event.stopEvent(e);
                  return noop; // perform a no-op
               }
               else
                  return false; // allow default behavior
            }
            else {
               if (self.@org.rstudio.studio.client.workbench.views.source.editors.text.AceKeyboardPreviewer::onTextInput(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(data, keyOrText))
                  return noop;
               else
                  return false;
            }
         })
      };
   }-*/;

   private boolean onKeyDown(JavaScriptObject data, NativeEvent e)
   {
      for (Handler handler : handlers_)
      {
         if (handler.previewKeyDown(data, e))
            return true;
      }
      return false;
   }
   

   private boolean onTextInput(JavaScriptObject data, String text)
   {
      if (text == null)
         return false;

      // Escape key comes in as a character on desktop builds
      if (text.equals("\u001B"))
         return true;

      for (Handler handler : handlers_)
      {
         for (int i = 0; i < text.length(); i++)
            if (handler.previewKeyPress(data, text.charAt(i)))
               return true;
      }
      
      return false;
   }
   
   private ArrayList<Handler> handlers_ = new ArrayList<Handler>();
}
