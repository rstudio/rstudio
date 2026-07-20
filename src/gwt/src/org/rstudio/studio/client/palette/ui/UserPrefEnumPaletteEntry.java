/*
 * UserPrefEnumPaletteEntry.java
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


import java.util.Set;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefEnumPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefEnumPaletteEntry(EnumValue val, UserPrefPaletteItem item, Set<String> excludedValues)
   {
      super(val, item);
      val_ = val;
      prefItem_ = item;

      selector_ = new ListBox();
      selector_.setVisibleItemCount(1);

      // Create marginally more user friendly names for option values by
      // removing common separators and adding some casing. Values excluded
      // because they are unavailable in this build are skipped -- except the
      // current value, which is kept selectable so a stale/unavailable
      // selection is shown and can be changed away from (otherwise it would
      // appear as the first option and could not be cleared).
      String currentValue = val_.getGlobalValue();
      String[] values = val.getAllowedValues();
      for (String value: values)
      {
         if (excludedValues.contains(value) && !StringUtil.equals(value, currentValue))
            continue;
         String option = value.replace("-", " ");
         option = option.replace("_", " ");
         option = StringUtil.capitalizeAllWords(option);
         selector_.addItem(option, value);
      }

      // Show the currently selected value (indices reflect the filtered list)
      for (int i = 0; i < selector_.getItemCount(); i++)
      {
         if (StringUtil.equals(selector_.getValue(i), currentValue))
         {
            selector_.setSelectedIndex(i);
            break;
         }
      }
      
      // Adjust style for display
      selector_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      selector_.getElement().getStyle().setMarginRight(5, Unit.PX);
      
      selector_.addChangeHandler((evt) ->
      {
         // Change the preference to the new value
         val_.setGlobalValue(selector_.getSelectedValue());

         // Save new state
         prefItem_.nudgeWriter();
      });

      initialize();

      // Establish link between the selector and the name element for
      // accessibility purposes
      Roles.getOptionRole().setAriaLabelledbyProperty(selector_.getElement(),
            Id.of(name_.getElement()));

   }

   @Override
   public Widget getInvoker()
   {
      return selector_;
   }
   
   @Override
   public void invoke(InvocationSource source)
   {
      DomUtils.setActive(selector_.getElement());
   }
   
   private ListBox selector_;
   private EnumValue val_;
   private final UserPrefPaletteItem prefItem_;
}
