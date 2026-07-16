/*
 * VimrcLoader.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Loads Vim key mappings from the user's vimrc file (~/.rstudio-vimrc if it
 * exists, otherwise ~/.vimrc) and applies them to Ace's Vim keybinding
 * emulation. Mappings are stored in state shared by all editor instances, so
 * the vimrc only needs to be applied once per client session.
 */
@Singleton
public class VimrcLoader
{
   @Inject
   public VimrcLoader(UserPrefs prefs, FilesServerOperations server)
   {
      prefs_ = prefs;
      server_ = server;

      // load the vimrc when the preference is enabled mid-session
      prefs_.vimLoadVimrc().addValueChangeHandler(event ->
      {
         if (!event.getValue())
            return;

         AceEditor editor = AceEditor.getLastFocusedEditor();
         if (editor != null && editor.isVimModeOn())
            ensureLoaded(editor.getWidget().getEditor());
      });
   }

   /**
    * Load and apply the user's vimrc if the associated preference is enabled.
    * The supplied editor must have the Vim keyboard handler attached.
    */
   public void ensureLoaded(AceEditorNative editor)
   {
      if (requested_ || !prefs_.vimLoadVimrc().getValue())
         return;

      requested_ = true;
      loadVimrc(editor, 0);
   }

   private void loadVimrc(AceEditorNative editor, int index)
   {
      if (index >= VIMRC_PATHS.length)
         return;

      String path = VIMRC_PATHS[index];
      server_.getFileContents(path, "UTF-8", new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String contents)
         {
            applyVimrc(editor, path, contents);
         }

         @Override
         public void onError(ServerError error)
         {
            // the file most likely doesn't exist; try the next candidate
            loadVimrc(editor, index + 1);
         }
      });
   }

   private void applyVimrc(AceEditorNative editor, String path, String contents)
   {
      int applied = 0;

      for (String rawLine : contents.split("\n"))
      {
         String line = rawLine.trim();
         if (line.isEmpty() || line.startsWith("\""))
            continue;

         String prepared = prepareLine(line);
         if (prepared == null)
         {
            Debug.log("VimrcLoader: ignoring unsupported line '" + line + "'");
            continue;
         }

         if (applyLine(editor, prepared))
            applied++;
         else
            Debug.log("VimrcLoader: failed to apply line '" + line + "'");
      }

      if (applied > 0)
         Debug.log("VimrcLoader: applied " + applied + " command(s) from " + path);
   }

   /**
    * Prepare a vimrc line for the Vim emulation, or return null if the line
    * isn't supported. Only mapping and 'set' commands are allowed through;
    * this keeps a vimrc from triggering arbitrary ex commands (e.g. ':qall')
    * as a side effect of being loaded.
    */
   private String prepareLine(String line)
   {
      String[] tokens = line.split("\\s+");

      String command = tokens[0];
      if (!SUPPORTED_COMMANDS.contains(command))
         return null;

      // process any special arguments following the command
      String rest = line.substring(command.length()).trim();
      while (rest.startsWith("<"))
      {
         int end = rest.indexOf('>');
         if (end == -1)
            break;

         String arg = rest.substring(0, end + 1).toLowerCase();
         if (arg.equals("<expr>") || arg.equals("<buffer>"))
         {
            // these change mapping semantics in ways the emulation
            // doesn't support, so skip the line entirely
            return null;
         }
         else if (SKIPPABLE_ARGUMENTS.contains(arg))
         {
            rest = rest.substring(end + 1).trim();
         }
         else
         {
            // not a special argument; likely the start of the mapping
            // itself (e.g. '<C-a>')
            break;
         }
      }

      return rest.isEmpty() ? command : command + " " + rest;
   }

   private static final native boolean applyLine(AceEditorNative editor, String line)
   /*-{
      try
      {
         var cm = editor.state.cm;
         if (cm == null)
            return false;

         var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
         Vim.handleEx(cm, line);
         return true;
      }
      catch (e)
      {
         return false;
      }
   }-*/;

   private static final String[] VIMRC_PATHS = new String[] {
      "~/.rstudio-vimrc",
      "~/.vimrc"
   };

   private static final Set<String> SUPPORTED_COMMANDS = new HashSet<>(Arrays.asList(
      "map",
      "nmap",      "nm",
      "vmap",      "vm",
      "imap",      "im",
      "omap",      "om",
      "noremap",   "no",
      "nnoremap",  "nn",
      "vnoremap",  "vn",
      "inoremap",  "ino",
      "onoremap",  "ono",
      "unmap",
      "mapclear",  "mapc",
      "nmapclear", "nmapc",
      "vmapclear", "vmapc",
      "imapclear", "imapc",
      "omapclear", "omapc",
      "set",       "se",
      "setlocal",  "setl",
      "setglobal", "setg"
   ));

   private static final Set<String> SKIPPABLE_ARGUMENTS = new HashSet<>(Arrays.asList(
      "<silent>", "<unique>", "<nowait>", "<special>", "<script>"
   ));

   private final UserPrefs prefs_;
   private final FilesServerOperations server_;

   private boolean requested_ = false;
}
