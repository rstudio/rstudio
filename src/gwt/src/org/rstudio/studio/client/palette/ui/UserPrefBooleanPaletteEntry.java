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

import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.BooleanValue;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefBooleanPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefBooleanPaletteEntry(BooleanValue val, UserPrefPaletteItem item)
   {
      super(val, item);
      prefItem_ = item;
      boolean initial = val.getGlobalValue();
      
      // Create the checkbox with the initial value
      checkbox_ = new CheckBox();
      checkbox_.setValue(initial);
      Style style = checkbox_.getElement().getStyle();
      syncLabel();
      
      // Set a constant width for the checkbox so that toggling on/off doesn't
      // change its width
      style.setWidth(45, Unit.PX);
      style.setDisplay(Display.INLINE_BLOCK);
      style.setTextAlign(TextAlign.LEFT);
      style.setCursor(Cursor.POINTER);

      // Add handler to the toggle control to sync pref with toggle state
      checkbox_.addValueChangeHandler((newVal) ->
      {
         // Compute new value
         BooleanValue pref = (BooleanValue)pref_;
         pref.setGlobalValue(newVal.getValue());

         // Sync the label text to "on" or "off"
         syncLabel();

         // Save new value
         prefItem_.nudgeWriter();
      });
      initialize();

      // Establish link between the toggle switch and the name element for
      // accessibility purposes (must be done post-init so that name_ has an ID)
      Roles.getCheckboxRole().setAriaLabelledbyProperty(checkbox_.getElement(), 
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
      checkbox_.setValue(!checkbox_.getValue(), true /* fire events */);
   }

   @Override
   public Widget getInvoker()
   {
      return checkbox_;
   }
   
   private void syncLabel()
   {
	   checkbox_.setText(checkbox_.getValue() ? "On" : "Off");
   }
   
   private final CheckBox checkbox_;
   private final UserPrefPaletteItem prefItem_;
}
