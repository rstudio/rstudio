/*
 * UserPrefMenuItem.java
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

package org.rstudio.core.client.widget;

import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

// A menu item that represents a given value for a given UIPref. Shows a check
// when the UIPref is set to the value. Invoking the menu item sets the UIPref
// to the value.
public class UserPrefMenuItem <T> extends CheckableMenuItem
{
   public UserPrefMenuItem(PrefValue<T> prefValue, T targetValue, 
                         String label, UserPrefs uiPrefs)
   {
      targetValue_ = targetValue;
      label_ = label;
      uiPrefs_ = uiPrefs;
      prefValue_ = prefValue;
      prefValue.addValueChangeHandler(
            new ValueChangeHandler<T>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<T> arg0)
         {
            onStateChanged();
         }
      });
      onStateChanged();
   }

   @Override
   public boolean isChecked()
   {
      return prefValue_ != null &&
             prefValue_.getValue() == targetValue_;
   }

   @Override
   @SuppressWarnings({ "unchecked" })
   public void onInvoked()
   {
      if (targetValue_ instanceof Boolean)
      {
         // for boolean values, the menu item acts like a toggle
         Boolean newValue = !(Boolean)prefValue_.getValue();
         prefValue_.setGlobalValue((T)newValue, true);
      }
      else
      {
         // for other value types the menu item always sets to the target value
         prefValue_.setGlobalValue(targetValue_, true);
      }
      uiPrefs_.writeUserPrefs();
   }

   @Override
   public String getLabel()
   {
      return label_ == null ? "" : label_;
   }
   
   T targetValue_;
   PrefValue<T> prefValue_;
   UserPrefs uiPrefs_;
   String label_;
}
