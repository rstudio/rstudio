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
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.ui.UserPrefBooleanPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefEnumPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefIntegerPaletteEntry;
import org.rstudio.studio.client.palette.ui.UserPrefPaletteEntry;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.BooleanValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.IntValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;

import com.google.gwt.user.client.ui.Widget;

public class UserPrefPaletteItem implements CommandPaletteItem
{
   public UserPrefPaletteItem(PrefValue<?> val)
   {
      val_ = val;
   }

   @Override
   public Widget asWidget()
   {
      if (widget_ == null)
      {
         if (val_ instanceof BooleanValue)
         {
            widget_ = new UserPrefBooleanPaletteEntry((BooleanValue)val_, this);
         }
         else if (val_ instanceof EnumValue)
         {
            widget_ = new UserPrefEnumPaletteEntry((EnumValue)val_, this);
         }
         else if (val_ instanceof IntValue)
         {
            widget_ = new UserPrefIntegerPaletteEntry((IntValue)val_, this);
         }
      }
      
      return widget_;
   }

   @Override
   public void invoke()
   {
      widget_.invoke();
      nudgeWriter();
   }

   @Override
   public boolean matchesSearch(String[] keywords)
   {
      String hay = val_.getTitle().toLowerCase();
      for (String needle: keywords)
      {
         if (!hay.contains(needle))
         {
            return false;
         }
      }
      return true;
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
   private UserPrefPaletteEntry widget_;
}
