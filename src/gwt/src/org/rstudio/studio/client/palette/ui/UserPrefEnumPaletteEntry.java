/*
 * UserPrefEnumPaletteEntry.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.palette.UserPrefPaletteItem;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

public class UserPrefEnumPaletteEntry extends UserPrefPaletteEntry
{
   public UserPrefEnumPaletteEntry(EnumValue val, UserPrefPaletteItem item)
   {
      super(val, item);
      val_ = val;
      
      // Create marginally more user friendly names for option values by
      // removing common separators and adding some casing
      List<String> options = new ArrayList<String>();
      for (String value: val.getAllowedValues())
      {
         String option = value.replace("-", " ");
         option = option.replace("_", " ");
         option = StringUtil.capitalizeAllWords(option);
         options.add(option);
      }
      
      selector_ = new SelectWidget("", options.toArray(new String[0]),
            val.getAllowedValues(), false /* is multiple */);
            
      // Show the currently selected value
      selector_.setValue(val.getGlobalValue());
      
      // Adjust style for display
      selector_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      selector_.getElement().getStyle().setMarginRight(5, Unit.PX);
      
      selector_.addChangeHandler((evt) ->
      {
         // Change the preference to the new value
         val_.setGlobalValue(selector_.getValue());

         // Save new state
         item_.nudgeWriter();
      });

      initialize();
   }

   @Override
   public Widget getInvoker()
   {
      return selector_;
   }
   
   @Override
   public void invoke()
   {
      DomUtils.setActive(selector_.getElement());
   }
   
   private SelectWidget selector_;
   private EnumValue val_;
}
