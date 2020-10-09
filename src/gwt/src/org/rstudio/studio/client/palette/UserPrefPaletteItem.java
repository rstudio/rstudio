/*
 * UserPrefPaletteItem.java
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

import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.palette.ui.UserPrefBooleanPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefEnumPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefIntegerPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefPaletteEntry;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.BooleanValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.IntValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;

public class UserPrefPaletteItem extends BasePaletteItem<UserPrefPaletteEntry>
{
   public UserPrefPaletteItem(PrefValue<?> val)
   {
      val_ = val;
   }

   @Override
   public UserPrefPaletteEntry createWidget()
   {
      if (val_ instanceof BooleanValue)
      {
         return new UserPrefBooleanPaletteEntry((BooleanValue)val_, this);
      }
      else if (val_ instanceof EnumValue)
      {
         return new UserPrefEnumPaletteEntry((EnumValue)val_, this);
      }
      else if (val_ instanceof IntValue)
      {
         return new UserPrefIntegerPaletteEntry((IntValue)val_, this);
      }
      
      return null;
   }

   @Override
   public void invoke(InvocationSource source)
   {
      widget_.invoke(source);
      nudgeWriter();
   }

   @Override
   public boolean matchesSearch(String[] keywords)
   {
      return super.labelMatchesSearch("setting " + val_.getTitle(), keywords);
   }

   @Override
   public void setSearchHighlight(String[] keywords)
   {
      widget_.setSearchHighlight(keywords);
   }

   @Override
   public boolean dismissOnInvoke()
   {
      return false;
   }

   @Override
   public void setSelected(boolean selected)
   {
      widget_.setSelected(selected);
   }
   
   public void nudgeWriter()
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

   private final PrefValue<?> val_;
}
