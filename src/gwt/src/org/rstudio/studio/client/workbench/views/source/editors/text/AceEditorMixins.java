/*
 * AceEditorMixins.java
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.JavaScriptEventHistory;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.JavaScriptEventHistory.EventData;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.AceSelectionChangedEvent;

import com.google.inject.Inject;

public class AceEditorMixins
{
   public AceEditorMixins(AceEditor editor)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      editor_ = editor.getWidget().getEditor();
      
      initializeMixins(editor_);
      
      // on Linux, record whenever the user selects something in Ace
      if (BrowseCap.isLinuxDesktop())
      {
         editor.addSelectionChangedHandler(new AceSelectionChangedEvent.Handler()
         {
            @Override
            public void onSelectionChanged(AceSelectionChangedEvent event)
            {
               String selection = editor_.getSelectedText();
               if (!StringUtil.isNullOrEmpty(selection))
                  Desktop.getFrame().setGlobalMouseSelection(selection);
            }
         });
      }
   }
   
   @Inject
   private void initialize(JavaScriptEventHistory history)
   {
      history_ = history;
   }
   
   private final native void initializeMixins(AceEditorNative editor)
   /*-{
      // store reference to mixins object
      var self = this;
      
      // override the 'onPaste()' method provided by the Editor prototype
      editor.$onPaste = editor.onPaste;
      
      editor.onPaste = $entry(function(text) {
         
         // call mixins method
         self.@org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorMixins::onPaste(Lorg/rstudio/studio/client/workbench/views/source/editors/text/ace/AceEditorNative;Ljava/lang/String;)(this, text);
      });
      
   }-*/;
   
   private final void onPaste(AceEditorNative editor, String text)
   {
      // detect whether this paste event is issued in response to a middle click
      // -- in such cases, we want to paste using the global mouse selection
      boolean useGlobalMouseSelection =
            BrowseCap.isLinuxDesktop() &&
            isPasteTriggeredByMiddleClick();
      
      // command to be executed once required text has been made available
      final CommandWithArg<String> onReadyToPaste = new CommandWithArg<String>()
      {
         @Override
         public void execute(String code)
         {
            // normalize line endings (Ace expects only '\n' line endings based
            // on how we initialize it, and '\r\n' line endings cause issues)
            code = code.replaceAll("\r\n|\r", "\n");

            // invoke paste handler
            invokePasteHandler(editor, code);
         }
      };
      
      if (useGlobalMouseSelection)
      {
         Desktop.getFrame().getGlobalMouseSelection(selection ->
               onReadyToPaste.execute(selection));
      }
      else
      {
         onReadyToPaste.execute(text);
      }
   }
   
   private final boolean isPasteTriggeredByMiddleClick()
   {
      // find the first key or mouse event in the JS event history
      EventData event = history_.findEvent(new JavaScriptEventHistory.Predicate()
      {
         @Override
         public boolean accept(EventData event)
         {
            String type = event.getType();
            for (String candidate : CANDIDATES)
               if (type.contentEquals(candidate))
                  return true;
            return false;
         }
         
         private final String[] CANDIDATES = new String[] {
               "mousedown", "click", "mouseup",
               "keydown", "keypress", "keyup"
         };
      });
      
      // shouldn't happen, but guard against failure to find any
      // associated event
      if (event == null)
         return false;
      
      // check to see if the middle mouse button was associated with
      // this event
      int button = event.getButton();
      if (button != EventData.BUTTON_MIDDLE)
         return false;
      
      // ensure that this was a click-related mouse event
      // (be permissive as to what mouse even occurred)
      String type = event.getType();
      return
            type == "mousedown" ||
            type == "click" ||
            type == "mouseup";
   }
   
   private static final native void invokePasteHandler(AceEditorNative editor, String text)
   /*-{
      editor.$onPaste(text);
   }-*/;
   
   private final AceEditorNative editor_;
   
   // Injected ----
   private static JavaScriptEventHistory history_;
}
