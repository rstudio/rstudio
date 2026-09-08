/*
 * Vim.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
   }

   public boolean isActive() { return isActive(editor_); }
   private final native boolean isActive(AceEditorNative editor) /*-{
      return editor.$vimModeHandler != null;
   }-*/;

   // NOTE: VimAPI is resolved at call time, not construction time -- the vim
   // keybindings load lazily, and the exit* methods only run when vim mode is
   // active on the editor, which implies the keybindings have loaded
   public void exitVisualMode() { exitVisualMode(editor_, VimAPI.get()); }
   private final native void exitVisualMode(AceEditorNative editor, VimAPI vim) /*-{
      var vimState = editor.state.cm.state.vim;
      if (vimState.visualMode)
         vim.exitVisualMode(editor.state.cm);
   }-*/;

   public void exitInsertMode() { exitInsertMode(editor_, VimAPI.get()); }
   private final native void exitInsertMode(AceEditorNative editor, VimAPI vim) /*-{
      var vimState = editor.state.cm.state.vim;
      if (vimState.insertMode)
         vim.exitInsertMode(editor.state.cm);
   }-*/;

   private final AceEditorNative editor_;
}
