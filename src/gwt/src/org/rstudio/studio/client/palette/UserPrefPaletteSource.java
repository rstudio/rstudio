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

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.palette.ui.CommandPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefBooleanPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefEnumPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefIntegerPaletteEntry;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.BooleanValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.IntValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

/**
 * A command palette entry source which serves as a factory for user preference
 * values.
 */
public class UserPrefPaletteSource implements CommandPaletteEntrySource<PrefValue<?>>
{
   public UserPrefPaletteSource(UserPrefs prefs)
   {
      prefs_ = prefs;
   }

   @Override
   public List<PrefValue<?>> getPaletteCommands()
   {
      return prefs_.allPrefs();
   }

   @Override
   public CommandPaletteEntry renderPaletteCommand(PrefValue<?> val)
   {
      if (StringUtil.isNullOrEmpty(val.getTitle()))
      {
         // Ignore preferences with no title (the title is the only
         // reasonable thing we can display)
         return null;
      }

      if (val instanceof BooleanValue)
      {
         return new UserPrefBooleanPaletteEntry((BooleanValue)val);
      }
      else if (val instanceof EnumValue)
      {
         return new UserPrefEnumPaletteEntry((EnumValue)val);
      }
      else if (val instanceof IntValue)
      {
         return  new UserPrefIntegerPaletteEntry((IntValue)val);
      }
      return null;
   }

   private final UserPrefs prefs_;
}
