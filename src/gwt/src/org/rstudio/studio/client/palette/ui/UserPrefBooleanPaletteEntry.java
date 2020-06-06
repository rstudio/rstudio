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
package org.rstudio.studio.client.palette.ui;

import org.rstudio.core.client.widget.Toggle;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.BooleanValue;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefBooleanPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefBooleanPaletteEntry(BooleanValue val, UserPrefPaletteItem item)
   {
      super(val, item);
      prefItem_ = item;
      boolean initial = false;

      initial = val.getGlobalValue();

      toggle_ = new Toggle("", false);
      toggle_.setState(initial ? 
         Toggle.State.ON : Toggle.State.OFF, false);

      // Add handler to the toggle control to sync pref with toggle state
      toggle_.addValueChangeHandler((state) ->
      {
         // Compute new value
         boolean newValue = state.getValue() == Toggle.State.ON;
         BooleanValue pref = (BooleanValue)pref_;
         pref.setGlobalValue(newValue);

         // Save new value
         prefItem_.nudgeWriter();
      });
      initialize();

      // Establish link between the toggle switch and the name element for
      // accessibility purposes (must be done post-init so that name_ has an ID)
      Roles.getCheckboxRole().setAriaLabelledbyProperty(toggle_.getElement(), 
            Id.of(name_.getElement()));
   }

   @Override
   public void invoke(InvocationSource source)
   {
      if (source == InvocationSource.Mouse)
      {
         // Ignore invoke via click as it's much too easy to trip the toggle
         // switch accidentally with the mouse
         return;
      }

      // Set new value (will trigger an event that causes the underlying pref to
      // set)
      boolean newValue = toggle_.getState() != Toggle.State.ON;
      toggle_.setState(newValue ? Toggle.State.ON : Toggle.State.OFF,
         !RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue());
   }

   @Override
   public Widget getInvoker()
   {
      return toggle_;
   }
   
   private final Toggle toggle_;
   private final UserPrefPaletteItem prefItem_;
}
