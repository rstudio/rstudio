/*
 * Vim.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

public class Vim
{
   public Vim(AceEditor editor)
   {
      editor_ = editor.getWidget().getEditor();
      vim_ = VimAPI.get();
   }
   
   public boolean isActive() { return isActive(editor_); }
   private final native boolean isActive(AceEditorNative editor) /*-{
      return editor.$vimModeHandler != null;
   }-*/;
   
   public void exitVisualMode() { exitVisualMode(editor_, vim_); }
   private final native void exitVisualMode(AceEditorNative editor, VimAPI vim) /*-{
      var vimState = editor.state.cm.state.vim;
      if (vimState.visualMode)
         vim.exitVisualMode(editor.state.cm);
   }-*/;
   
   public void exitInsertMode() { exitInsertMode(editor_, vim_); }
   private final native void exitInsertMode(AceEditorNative editor, VimAPI vim) /*-{
      var vimState = editor.state.cm.state.vim;
      if (vimState.insertMode)
         vim.exitInsertMode(editor.state.cm);
   }-*/;
   
   private final AceEditorNative editor_;
   private final VimAPI vim_;
}
