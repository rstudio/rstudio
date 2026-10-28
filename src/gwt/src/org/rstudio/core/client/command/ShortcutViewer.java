/*
 * ShortcutViewer.java
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

package org.rstudio.core.client.command;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.widget.CommandShortcutReference;
import org.rstudio.core.client.widget.KeySymbolLegend;
import org.rstudio.core.client.widget.ShortcutReferenceDialog;
import org.rstudio.core.client.widget.VimKeyInfo;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShortcutViewer
{
   public interface Binder extends CommandBinder<Commands, ShortcutViewer> {}

   @Inject
   public ShortcutViewer(Binder binder, Commands commands)
   {
      binder.bind(commands, this);
   }

   @Handler
   public void onHelpKeyboardShortcuts()
   {
      // shortcuts are shown with key symbols where there's a Command key
      new ShortcutReferenceDialog(
            constants_.shortcutHeaderText(),
            BrowseCap.hasMetaKey() ? new KeySymbolLegend() : null,
            new CommandShortcutReference()).showModal();
   }

   public void showVimKeyboardShortcuts()
   {
      // Runs from the Vim :help command, which returns focus to the editor
      // afterwards; wait for that so the dialog takes focus last and
      // restores it to the editor on close
      Scheduler.get().scheduleDeferred(() ->
         new ShortcutReferenceDialog(
               constants_.vimKeyboardShortcutsText(),
               new VimKeyInfo()).showModal());
   }

   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
