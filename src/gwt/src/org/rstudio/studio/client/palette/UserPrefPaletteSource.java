/*
 * UserPrefPaletteSource.java
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.palette.model.CommandPaletteEntryProvider;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.ui.CommandPalette;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

/**
 * A command palette entry source which serves as a factory for user preference
 * values.
 */
public class UserPrefPaletteSource implements CommandPaletteEntryProvider
{
   public UserPrefPaletteSource(UserPrefs prefs, SessionInfo sessionInfo)
   {
      prefs_ = prefs;
      sessionInfo_ = sessionInfo;
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
         if (isPrefUnavailable(val.getId()))
         {
            // Ignore preferences for AI features not available in this build
            continue;
         }
         items.add(new UserPrefPaletteItem(val, excludedValues(val.getId())));
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

      if (isPrefUnavailable(id))
      {
         return null;
      }

      PrefValue<?> val = prefs_.getPrefValue(id);
      if (val == null)
      {
         Debug.logWarning("Unknown preference requested by command palette: '" + id + "'");
         return null;
      }

      return new UserPrefPaletteItem(val, excludedValues(id));
   }

   /**
    * Preferences for AI features that should not appear in the palette when the
    * corresponding feature is unavailable in this build (e.g. built with
    * RSTUDIO_ENABLE_POSIT_ASSISTANT=OFF) or disabled by the administrator.
    */
   private boolean isPrefUnavailable(String id)
   {
      switch (id)
      {
         // Posit Assistant-specific preferences (chat is Posit-only).
         case UserPrefsAccessor.ASSISTANT_TOOLBAR_BUTTON_VISIBLE:
         case UserPrefsAccessor.ASSISTANT_SHOW_MESSAGES:
         case UserPrefsAccessor.POSIT_ASSISTANT_TEST_MANIFEST:
         case UserPrefsAccessor.POSIT_ASSISTANT_UPDATE_CHECK_INTERVAL_HOURS:
         case UserPrefsAccessor.CHAT_PROVIDER:
            return !sessionInfo_.getPositAssistantEnabled();
         // The assistant selector and the system-CA option apply to any AI
         // agent (Copilot or Posit Assistant), so hide them only when no AI
         // assistant is available. The assistant selector additionally filters
         // out unavailable providers via excludedValues().
         case UserPrefsAccessor.ASSISTANT:
         case UserPrefsAccessor.ASSISTANT_USE_SYSTEM_CA:
            return !sessionInfo_.getCopilotEnabled() &&
                   !sessionInfo_.getPositAssistantEnabled();
         default:
            return false;
      }
   }

   /**
    * Enum preference values to hide because the provider they select is
    * unavailable. Only the "assistant" selector filters providers today; every
    * other preference returns an empty set (no filtering).
    */
   private Set<String> excludedValues(String id)
   {
      Set<String> excluded = new HashSet<>();
      if (id.equals(UserPrefsAccessor.ASSISTANT))
      {
         if (!sessionInfo_.getPositAssistantEnabled())
            excluded.add(UserPrefsAccessor.ASSISTANT_POSIT);
         if (!sessionInfo_.getCopilotEnabled())
            excluded.add(UserPrefsAccessor.ASSISTANT_COPILOT);
      }
      return excluded;
   }

   @Override
   public String getProviderScope()
   {
      return CommandPalette.SCOPE_USER_PREFS;
   }

   private final UserPrefs prefs_;
   private final SessionInfo sessionInfo_;
}
