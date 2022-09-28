/*
 * RAddinPaletteSource.java
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

package org.rstudio.studio.client.palette;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;

/**
 * A command palette entry source which serves as a factory for R addin
 * commands.
 */
public class RAddinPaletteSource implements CommandPaletteEntryProvider
{
   public RAddinPaletteSource(RAddins addins, ShortcutManager shortcuts)
   {
      addins_ = addins;
      executor_ = new AddinExecutor();
      map_ = shortcuts.getKeyMap(KeyMapType.ADDIN);
   }
   
   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      List<CommandPaletteItem> items = new ArrayList<>();
      for (String id: JsUtil.asIterable(addins_.keys()))
      {
         RAddin addin = addins_.get(id);
         List<KeySequence> keys = map_.getBindings(addin.getId());
         if (StringUtil.isNullOrEmpty(addin.getName()))
            continue;
         
         CommandPaletteItem item = new RAddinPaletteItem(addin, executor_, keys);
         items.add(item);
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

      RAddin addin = addins_.get(id);
      if (addin == null)
      {
         Debug.logWarning("R addin requested by the command palette but not found: '" + id + "'");
         return null;
      }
      return new RAddinPaletteItem(addin, executor_, map_.getBindings(id));
   }

   @Override
   public String getProviderScope()
   {
      return CommandPalette.SCOPE_R_ADDIN;
   }

   private final RAddins addins_;
   private final AddinExecutor executor_;
   private final KeyMap map_;
}
