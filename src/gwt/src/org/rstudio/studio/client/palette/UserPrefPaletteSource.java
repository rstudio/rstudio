/*
 * UserPrefPaletteSource.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

/**
 * A command palette entry source which serves as a factory for user preference
 * values.
 */
public class UserPrefPaletteSource implements CommandPaletteEntryProvider
{
   public UserPrefPaletteSource(UserPrefs prefs)
   {
      prefs_ = prefs;
   }

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      List<CommandPaletteItem> items = new ArrayList<>();
      for (PrefValue<?> val: prefs_.allPrefs())
      {
         if (StringUtil.isNullOrEmpty(val.getTitle()))
         {
            // Ignore preferences with no title (the title is the only
            // reasonable thing we can display)
            continue;
         }
         items.add(new UserPrefPaletteItem(val));
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

      PrefValue<?> val = prefs_.getPrefValue(id);
      if (val == null)
      {
         Debug.logWarning("Unknown preference requested by command palette: '" + id + "'");
         return null;
      }

      return new UserPrefPaletteItem(val);
   }

   @Override
   public String getProviderScope()
   {
      return CommandPalette.SCOPE_USER_PREFS;
   }

   private final UserPrefs prefs_;
}
