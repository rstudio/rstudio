/*
 * UserPrefBooleanPaletteEntry.java
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
package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.widget.Toggle;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefDefinition;

import com.google.gwt.user.client.ui.Widget;

public class UserPrefBooleanPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefBooleanPaletteEntry(String id, UserPrefDefinition pref)
   {
      super(id, pref);
      toggle_ = new Toggle("", false);
      toggle_.setState(Toggle.State.OFF, false /* animate */);
      initialize();
   }

   @Override
   public void invoke()
   {
      toggle_.setState(toggle_.getState() == Toggle.State.ON ?
         Toggle.State.OFF : Toggle.State.ON, true /* animate */);
   }

   @Override
   public Widget getInvoker()
   {
      return toggle_;
   }
   
   private final Toggle toggle_;
}
