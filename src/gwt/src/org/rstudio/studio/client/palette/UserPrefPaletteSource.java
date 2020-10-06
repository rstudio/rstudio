/*
 * UserPrefPaletteSource.java
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
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

/**
 * A command palette entry source which serves as a factory for user preference
 * values.
 */
public class UserPrefPaletteSource implements CommandPaletteEntrySource
{
   public UserPrefPaletteSource(UserPrefs prefs)
   {
      prefs_ = prefs;
   }

   @Override
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      List<CommandPaletteItem> items = new ArrayList<CommandPaletteItem>();
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

   private final UserPrefs prefs_;
}
