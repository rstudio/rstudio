/*
 * AceCompletionAdapter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.dom.client.NativeEvent;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.KeyboardHandler;

public class AceCompletionAdapter
{
   public AceCompletionAdapter(CompletionManager completionManager)
   {
      completionManager_ = completionManager;
   }

   public native final KeyboardHandler getKeyboardHandler() /*-{
      var event = $wnd.require("ace/lib/event");
      var self = this;
      var noop = {command: "null"};
      return {
         handleKeyboard: $entry(function(data, hashId, keyOrText, keyCode, e) {
            if (hashId != -1 || keyCode) {
               if (self.@org.rstudio.studio.client.workbench.views.source.editors.text.AceCompletionAdapter::onKeyDown(Lcom/google/gwt/dom/client/NativeEvent;)(e)) {
                  event.stopEvent(e);
                  return noop; // perform a no-op
               }
               else
                  return false; // allow default behavior
            }
            else {
               if (self.@org.rstudio.studio.client.workbench.views.source.editors.text.AceCompletionAdapter::onTextInput(Ljava/lang/String;)(keyOrText))
                  return noop;
               else
                  return false;
            }
         })
      };
   }-*/;

   private boolean onKeyDown(NativeEvent e)
   {
      return completionManager_.previewKeyDown(e);
   }

   private boolean onTextInput(String text)
   {
      if (text == null)
         return false;

      // Escape key comes in as a character on desktop builds
      if (text.equals("\u001B"))
         return true;

      for (int i = 0; i < text.length(); i++)
         if (completionManager_.previewKeyPress(text.charAt(i)))
            return true;
      return false;
   }

   private CompletionManager completionManager_;
}
