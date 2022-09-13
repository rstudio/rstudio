/*
 * CommandPaletteLauncher.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.palette;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.widget.ModalPopupPanel;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.palette.events.PaletteItemExecutedEvent;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteMruEntry;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.Source;

import java.util.ArrayList;
import java.util.List;

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
                                 Provider<WorkbenchListManager> pWorkbenchLists,
                                 EventBus events,
                                 GlobalDisplay display,
                                 Binder binder)
   {
      binder.bind(commands, this);
      addins_ = addins;
      commands_ = commands;
      pSource_ = pSource;
      pPrefs_ = pPrefs;
      display_ = display;
      pWorkbenchLists_ = pWorkbenchLists;
      state_ = State.Hidden;

      // Listen for item executions; when they occur, update the MRU accordingly
      events.addHandler(PaletteItemExecutedEvent.TYPE, (evt) ->
      {
         // Update local copy of MRU list immediately; this update will arrive
         // from the server eventually, but the lag is obvious if you attempt
         // to use the Command Palette as a quick "run last command again" tool
         if (mru_ == null)
         {
            mru_ = new ArrayList<>();
         }
         mru_.removeIf(entry -> entry.equals(evt.getMruEntry()));
         mru_.add(0, evt.getMruEntry());

         // Update copy on server
         pWorkbenchLists_.get().getCommandPaletteMruList().prepend(
            evt.getMruEntry().toString());
      });
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

   @Handler
   public void onClearCommandPaletteMru()
   {
      pWorkbenchLists_.get().getCommandPaletteMruList().clear();
      mru_ = null;
      display_.showMessage(GlobalDisplay.MSG_INFO, constants_.cmdPaletteClearedCaption(),
              constants_.cmdPaletteClearedMessage());
   }
   
   /**
    * Creates the popup panel that hosts the palette. Since this panel is
    * relatively heavyweight (it can hold a large number of commands), we create
    * it anew each time the palette is shown.
    */
   private void createPanel()
   {
      // Extra sources (currently only the source tab)
      List<CommandPaletteEntryProvider> providers = new ArrayList<>();
      
      // Create sources
      providers.add(new AppCommandPaletteSource(ShortcutManager.INSTANCE, commands_));
      CommandPaletteEntryProvider provider = pSource_.get().getPaletteEntryProvider();
      if (provider != null)
      {
         providers.add(provider);
      }
      providers.add(new RAddinPaletteSource(addins_.getRAddins(), ShortcutManager.INSTANCE));
      providers.add(new UserPrefPaletteSource(pPrefs_.get()));

      // Populate the MRU on first show if enabled
      if (mru_ == null && pPrefs_.get().commandPaletteMru().getValue())
      {
         pWorkbenchLists_.get().getCommandPaletteMruList().addListChangedHandler((evt) ->
         {
            ArrayList<String> mru = evt.getList();

            // After the first time the palette is shown, the MRU is updated by this event handler.
            mru_ = new ArrayList<>();
            for (String entry: mru)
            {
               CommandPaletteMruEntry mruEntry = CommandPaletteMruEntry.fromString(entry);
               if (mruEntry != null)
               {
                  mru_.add(mruEntry);
               }
            }
         });
      }

      // Create the command palette widget
      palette_ = new CommandPalette(providers,
         pPrefs_.get().commandPaletteMru().getValue() ? mru_ : null, this);

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
      Roles.getDialogRole().setAriaLabelProperty(ele, constants_.searchCmdsAriaLabelProperty());

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
   private ArrayList<CommandPaletteMruEntry> mru_;

   private final Commands commands_;
   private final AddinsCommandManager addins_;
   private final GlobalDisplay display_;
   private final Provider<Source> pSource_;
   private final Provider<UserPrefs> pPrefs_;
   private final Provider<WorkbenchListManager> pWorkbenchLists_;

   private static final PaletteConstants constants_ = GWT.create(PaletteConstants.class);
}
