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

import java.util.List;

import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.palette.ui.CommandPaletteEntry;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;

public class RAddinPaletteSource implements CommandPaletteEntrySource<String>
{
   public public RAddinPaletteSource(RAddins addins, ShortcutManager shortcuts)
   {
      addins_ = addins;
      executor_ = new AddinExecutor();
      map_ = shortcuts.getKeyMap(KeyMapType.ADDIN);
   }
   
   @Override
   public List<String> getElements()
   {
      return JsUtil.toList(addins_.keys());
   }

   @Override
   public CommandPaletteEntry renderElementEntry(String id)
   {
      RAddin addin = addins_.get(id);

      // Look up the key binding for this addin
      List<KeySequence> keys = map.getBindings(rAddin.getId());
      CommandPaletteEntry entry = new RAddinCommandPaletteEntry(rAddin, executor, keys);
      if (StringUtil.isNullOrEmpty(entry.getLabel()))
      {
         // Ignore addin commands which have no label
         continue;
      }
   }

   private final RAddins addins_;
   private final AddinExecutor executor_;
   private final KeyMap map_;
}
