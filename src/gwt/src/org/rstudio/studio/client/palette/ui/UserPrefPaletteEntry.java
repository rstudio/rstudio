/*
 * UserPrefPaletteEntry.java
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
package org.rstudio.studio.client.palette.ui;

import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;

public abstract class UserPrefPaletteEntry extends CommandPaletteEntry
{
   public UserPrefPaletteEntry(PrefValue<?> val)
   {
      super();
      pref_ = val;
   }

   @Override
   public String getLabel()
   {
      return pref_.getTitle();
   }

   @Override
   public String getId()
   {
      return pref_.getId();
   }

   @Override
   public String getContext()
   {
      return new String("Setting");
   }
   
   @Override
   public String getScope()
   {
      return "userpref";
   }

   @Override
   public boolean enabled()
   {
      // User preferences are always enabled
      return true;
   }
   
   @Override
   public boolean dismissOnInvoke()
   {
      return false;
   }
   
   @Override
   public void invoke()
   {
      PREF_WRITER.nudge();
   }
   
   // Use a single static pref writer so that we batch updates to preferences,
   // and write pref updates about 2s after the user stops changing them. It's
   // very easy to update toggle prefs (and others) several times in quick
   // succession, and we don't want to issue a flurry of pref write commands.
   private final static DebouncedCommand PREF_WRITER = new DebouncedCommand(2000)
   {
      @Override
      protected void execute()
      {
         // If we're already in the middle of an update, delay for another 2s
         if (updating_)
         {
            nudge();
            return;
         }

         // Commit the update to the server
         updating_ = true;
         RStudioGinjector.INSTANCE.getUserPrefs().writeUserPrefs((Boolean success) ->
         {
            updating_ = false;
         });
      }

      private boolean updating_ = false;
   };

   protected final PrefValue<?> pref_;
}
