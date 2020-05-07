/*
 * CommandPaletteLauncher.java
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

package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CommandPaletteLauncher implements CommandPalette.Host
{
   public interface Binder
          extends CommandBinder<Commands, CommandPaletteLauncher> {}
   
   private enum State
   {
      Uninitialized,  // The panel has never been shown
      Initializing,   // The panel is getting ready to show for the first time
      Initialized     // The panel has been shown at least once and is fully initialized
   }
   
   @Inject 
   public CommandPaletteLauncher(Commands commands,
         AddinsCommandManager addins,
         Binder binder)
   {
      binder.bind(commands, this);
      addins_ = addins;
      commands_ = commands;
      state_ = State.Uninitialized;
   }
   
   @Handler
   public void onShowCommandPalette()
   {
      // Create the command palette widget
      palette_ = new CommandPalette(commands_, addins_.getRAddins(), 
            ShortcutManager.INSTANCE, this);
      
      if (state_ == State.Initialized)
      {
         // Already initialized; just show the panel
         createPanel();
      }
      else if (state_ == State.Uninitialized)
      {
         // Not yet initialized. The first load happens behind the browser event
         // loop so that the CSS resources can be injected. We could fix this by
         // eagerly injecting these when RStudio starts, but this way we don't
         // pay any boot time penalty.
         state_ = State.Initializing;
         Scheduler.get().scheduleDeferred(() ->
         {
            state_ = State.Initialized;
            createPanel();
         });
      }
   }
   
   /**
    * Creates the popup panel that hosts the palette. Since this panel is
    * relatively heavyweight (it can hold a large number of commands), we create
    * it anew each time the palette is shown.
    */
   private void createPanel()
   {
      panel_ = new PopupPanel(true, true);
      
      // Copy classes from the root RStudio container onto this panel. This is
      // necessary so that we can properly inherit theme colors.
      Element root = Document.get().getElementById("rstudio_container");
      if (root != null)
      {
         panel_.addStyleName(root.getClassName());
      }

      panel_.add(palette_);
      panel_.show();
      panel_.center();
      
      // Set z-index above panel splitters (otherwise they overlap the popup)
      panel_.getElement().getStyle().setZIndex(250);

      palette_.focus();
      
      // Free our reference to the panel when it closes
      panel_.addCloseHandler((evt) -> 
      {
         cleanup();
      });
   }

   @Override
   public void dismiss()
   {
      panel_.hide();
      cleanup();
   }
   
   /**
    * Free references to the palette and panel 
    */
   private void cleanup()
   {
      palette_ = null;
      panel_ = null;
   }
   
   private PopupPanel panel_;
   private CommandPalette palette_;
   private final Commands commands_;
   private final AddinsCommandManager addins_;
   private State state_;
}