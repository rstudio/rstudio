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
      var event = $wnd.require("pilot/event");
      var self = this;
      var noop = {command: "null"};
      return {
         handleKeyboard: $entry(function(data, hashId, keyOrText, keyCode, e) {
            if (hashId != 0 || keyCode != 0) {
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
      for (int i = 0; i < text.length(); i++)
         if (completionManager_.previewKeyPress(text.charAt(i)))
            return true;
      return false;
   }

   private CompletionManager completionManager_;
}
