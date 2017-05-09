/*
 * AceEditorMixins.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.JavaScriptEventHistory;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

import com.google.inject.Inject;

public class AceEditorMixins
{
   public AceEditorMixins()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(JavaScriptEventHistory history)
   {
      history_ = history;
   }
   
   public static void initializeMixins()
   {
      if (INITIALIZED)
         return;
      INITIALIZED = true;
      initializeMixinsImpl();
   }
   
   private static final native void initializeMixinsImpl()
   /*-{
      var Editor = $wnd.require("ace/editor").Editor;
      var RStudioEditor = $wnd.require("rstudio/loader").RStudioEditor;
      
      (function() {
         
         this.onPaste = function(text)
         {
            @org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorMixins::onPaste(Lorg/rstudio/studio/client/workbench/views/source/editors/text/ace/AceEditorNative;Ljava/lang/String;)(this, text);
         };
         
      }).call(RStudioEditor.prototype);
   }-*/;
   
   private static final void onPaste(AceEditorNative editor, String text)
   {
      Debug.logToRConsole("On paste!");
      
      // TODO: detect whether this paste event is issued in response to a middle click
      // -- in such cases, we want to paste using the global mouse selection
      
      // normalize line endings (Ace expects only '\n' line endings based
      // on how we initialize it, and '\r\n' line endings cause issues)
      text = text.replaceAll("\r\n|\r", "\n");
      
      // invoke paste handler
      invokePasteHandler(editor, text);
   }
   
   private static final native void invokePasteHandler(AceEditorNative editor, String text)
   /*-{
      var Editor = $wnd.require("ace/editor").Editor;
      Editor.onPaste.call(editor, text);
   }-*/;
   
   // Injected ----
   private JavaScriptEventHistory history_;
   
   // Static Members ----
   private static boolean INITIALIZED = false;
}
