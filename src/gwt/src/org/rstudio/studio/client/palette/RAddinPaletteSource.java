/*
 * RAddinPaletteSource.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;

/**
 * A command palette entry source which serves as a factory for R addin
 * commands.
 */
public class RAddinPaletteSource implements CommandPaletteEntrySource
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
      List<CommandPaletteItem> items = new ArrayList<CommandPaletteItem>();
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

   private final RAddins addins_;
   private final AddinExecutor executor_;
   private final KeyMap map_;
}
