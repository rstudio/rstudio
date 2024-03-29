/*
 * UserPrefPaletteEntry.java
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
package org.rstudio.studio.client.palette.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.palette.PaletteConstants;
import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;

public abstract class UserPrefPaletteEntry extends CommandPaletteEntry
{
   public UserPrefPaletteEntry(PrefValue<?> val, UserPrefPaletteItem item)
   {
      super(item);
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
      return new String(constants_.settingText());
   }
   
   @Override
   public String getScope()
   {
      return CommandPalette.SCOPE_USER_PREFS;
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
   
   abstract public void invoke(InvocationSource source);

   protected final PrefValue<?> pref_;
   private static final PaletteConstants constants_ = GWT.create(PaletteConstants.class);
}
