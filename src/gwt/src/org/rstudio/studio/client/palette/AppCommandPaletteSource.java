/*
 * AppCommandPaletteSource.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.commands.Commands;

public class AppCommandPaletteSource implements CommandPaletteEntryProvider
{
   public AppCommandPaletteSource(ShortcutManager shortcuts, Commands commands)
   {
      commands_ = commands;
      map_ = shortcuts.getKeyMap(KeyMapType.APPLICATION);
   }

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      List<CommandPaletteItem> items = new ArrayList<CommandPaletteItem>();
      List<String> sorted = new ArrayList<String>();
      Set<String> unsorted = new HashSet<String>();
      unsorted.addAll(commands_.getCommands().keySet());

      // Front-load the first page of results with some useful commands; we don't attempt to sort
      // the whole list by popularity, but we want the first page of results to be familiar,
      // high-usage commands.
      sorted.add(commands_.newSourceDoc().getId());
      sorted.add(commands_.newRMarkdownDoc().getId());
      sorted.add(commands_.newRNotebook().getId());
      sorted.add(commands_.newRShinyApp().getId());
      sorted.add(commands_.newTerminal().getId());
      sorted.add(commands_.newProject().getId());
      sorted.add(commands_.openSourceDoc().getId());
      sorted.add(commands_.openProject().getId());
      sorted.add(commands_.goToFileFunction().getId());
      sorted.add(commands_.sourceActiveDocument().getId());
      sorted.add(commands_.knitDocument().getId());
      sorted.add(commands_.importDatasetFromCsvUsingReadr().getId());
      sorted.add(commands_.clearWorkspace().getId());
      sorted.add(commands_.restartR().getId());
      sorted.add(commands_.installPackage().getId());
      sorted.add(commands_.helpHome().getId());
      sorted.add(commands_.showOptions().getId());

      // Remove the front-loaded commands from the unsorted list
      for (String id: sorted)
      {
         unsorted.remove(id);
      }

      // Add the remainder of the unsorted commands to the sorted list
      // (in no particular order)
      sorted.addAll(unsorted);

      // Add each command from the sorted list to the palette
      for (String id: sorted)
      {
         if ((id.contains("Mru") || id.startsWith("mru") || id.contains("Dummy")) &&
              !id.contains("Palette"))
         {
            // MRU entries and dummy commands should not appear in the palette
            // (unless they pertain to the palette itself)
            continue;
         }

         // Retrieve the command in question
         AppCommand command = commands_.getCommandById(id);
         if (command == null)
         {
            Debug.logWarning("ID '" + id + "' has no matching command.");
            continue;
         }
         
         // Ensure the command is visible. It'd be nice to show all commands in
         // the palette for the purposes of examining key bindings, discovery,
         // etc., but invisible commands are generally meaningless in the 
         // current context.
         if (!command.isVisible())
         {
            continue;
         }

         // Look up the key binding for this command
         List<KeySequence> keys = map_.getBindings(command.getId());
         
         // Create an application command entry
         items.add(new AppCommandPaletteItem(command, keys));
      }

      return items;
   }

   @Override
   public CommandPaletteItem getCommandPaletteItem(String id)
   {
      if (StringUtil.isNullOrEmpty(id))
      {
         return null;
      }

      AppCommand command = commands_.getCommandById(id);
      if (command == null)
      {
         Debug.logWarning("Unknown command ID requested by command palette: '" + id + "'");
         return null;
      }

      return new AppCommandPaletteItem(command, map_.getBindings(id));
   }

   @Override
   public String getProviderScope()
   {
      return CommandPalette.SCOPE_APP_COMMAND;
   }

   private final KeyMap map_;
   private final Commands commands_;
}
