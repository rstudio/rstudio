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

package org.rstudio.studio.client.palette;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.widget.ModalPopupPanel;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.Source;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class CommandPaletteLauncher implements CommandPalette.Host
{
   public interface Binder
          extends CommandBinder<Commands, CommandPaletteLauncher> {}
   
   private enum State
   {
      Showing,        // The panel is currently showing
      Hidden          // The panel is ready, but currently hidden
   }
   
   @Inject 
   public CommandPaletteLauncher(Commands commands,
         AddinsCommandManager addins,
         Provider<Source> pSource,
         Provider<UserPrefs> pPrefs,
         Binder binder)
   {
      binder.bind(commands, this);
      addins_ = addins;
      commands_ = commands;
      pSource_ = pSource;
      pPrefs_ = pPrefs;
      state_ = State.Hidden;
   }
   
   @Handler
   public void onShowCommandPalette()
   {
      // If the palette is already showing, treat this as a hide.
      if (state_ == State.Showing)
      {
         dismiss();
         return;
      }
      if (state_ == State.Hidden)
      {
         createPanel();
      }
   }
   
   /**
    * Creates the popup panel that hosts the palette. Since this panel is
    * relatively heavyweight (it can hold a large number of commands), we create
    * it anew each time the palette is shown.
    */
   private void createPanel()
   {
      // Extra sources (currently only the source tab)
      List<CommandPaletteEntrySource> sources = new ArrayList<CommandPaletteEntrySource>();
      
      // Create sources
      sources.add(new AppCommandPaletteSource(ShortcutManager.INSTANCE, commands_));
      sources.add(pSource_.get());
      sources.add(new RAddinPaletteSource(addins_.getRAddins(), ShortcutManager.INSTANCE));
      sources.add(new UserPrefPaletteSource(pPrefs_.get()));

      // Create the command palette widget
      palette_ = new CommandPalette(sources, this);
      
      panel_ = new ModalPopupPanel(
            true,  // Auto hide
            true,  // Modal
            false, // Glass (main window overlay)
            true   // Close on Esc
      );
      
      // Required for accessibility (see #6962)
      panel_.getElement().setTabIndex(-1);
      
      // Copy classes from the root RStudio container onto this panel. This is
      // necessary so that we can properly inherit theme colors.
      Element root = Document.get().getElementById("rstudio_container");
      if (root != null)
      {
         panel_.addStyleName(root.getClassName());
      }
      
      // Assign the appropriate ARIA role to this panel
      Element ele = panel_.getElement();
      Roles.getDialogRole().set(ele);
      Roles.getDialogRole().setAriaLabelProperty(ele, "Search commands and settings");

      // Find the center of the screen
      panel_.add(palette_);
      panel_.setPopupPositionAndShow((int x, int y) -> 
      {
         panel_.setPopupPosition(Window.getClientWidth() / 2 - 300, 50);
      });
      
      // Set z-index above panel splitters (otherwise they overlap the popup)
      panel_.getElement().getStyle().setZIndex(250);
      palette_.focus();

      state_ = State.Showing;
      
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
      state_ = State.Hidden;
   }
   
   private ModalPopupPanel panel_;
   private CommandPalette palette_;
   private State state_;

   private final Commands commands_;
   private final AddinsCommandManager addins_;
   private final Provider<Source> pSource_;
   private final Provider<UserPrefs> pPrefs_;
}
