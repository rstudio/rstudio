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

      // when the preference is enabled mid-session, load the vimrc using the
      // focused Vim editor; when disabled, reset so that a later re-enable
      // reloads the file (e.g. after the user has edited it)
      prefs_.vimLoadVimrc().addValueChangeHandler(event ->
      {
         if (!event.getValue())
         {
            if (state_ == State.DONE)
               state_ = State.IDLE;
            return;
         }

         AceEditor editor = AceEditor.getLastFocusedEditor();
         if (editor != null && editor.isVimModeOn())
            ensureLoaded(editor.getWidget().getEditor());
      });
   }

   /**
    * Load and apply the user's vimrc if the associated preference is enabled.
    * The vimrc is applied at most once per session, no matter how often this
    * is called; disabling and re-enabling the preference resets that, so the
    * file can be reloaded without restarting. The supplied editor must have
    * the Vim keyboard handler attached.
    */
   public void ensureLoaded(AceEditorNative editor)
   {
      if (state_ != State.IDLE || !prefs_.vimLoadVimrc().getValue())
         return;

      state_ = State.LOADING;
      loadVimrc(editor, 0);
   }

   private void loadVimrc(AceEditorNative editor, int index)
   {
      if (index >= VIMRC_PATHS.length)
      {
         // no vimrc could be read; treat as done for this session rather
         // than re-trying on every editor initialization
         state_ = State.DONE;
         return;
      }

      String path = VIMRC_PATHS[index];
      server_.getFileContents(path, "UTF-8", new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String contents)
         {
            if (!prefs_.vimLoadVimrc().getValue())
            {
               // the preference was disabled while the request was in flight
               state_ = State.IDLE;
               return;
            }

            if (!hasVimState(editor))
            {
               // the editor that triggered the load was detached while the
               // request was in flight; let the next Vim editor retry
               state_ = State.IDLE;
               return;
            }

            state_ = State.DONE;
            applyVimrc(editor, path, contents);
         }

         @Override
         public void onError(ServerError error)
         {
            // typically the file just doesn't exist, but log the failure
            // since the server doesn't distinguish a missing file from e.g.
            // a permission or decoding error
            Debug.log("VimrcLoader: couldn't read " + path + " (" + error.getMessage() + ")");
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

         String error = applyLine(editor, prepared);
         if (error == null)
            applied++;
         else
            Debug.log("VimrcLoader: failed to apply line '" + line + "' (" + error + ")");
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
   static String prepareLine(String line)
   {
      line = line.trim();

      String command = line.split("\\s+")[0];
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

   private static final native boolean hasVimState(AceEditorNative editor)
   /*-{
      return editor.state != null && editor.state.cm != null;
   }-*/;

   // returns null on success, or an error message on failure
   private static final native String applyLine(AceEditorNative editor, String line)
   /*-{
      try
      {
         var Vim = $wnd.require("ace/keyboard/vim").CodeMirror.Vim;
         Vim.handleEx(editor.state.cm, line);
         return null;
      }
      catch (e)
      {
         return "" + e;
      }
   }-*/;

   private enum State { IDLE, LOADING, DONE }

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

   private State state_ = State.IDLE;
}
