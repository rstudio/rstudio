/*
 * MonitoringMenuItem.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

// A checkable menu item whose state is bound to some separate
// value. Often constructed in pairs for on-off checkable items.
//
// NOTE: Implementations should update the state of the observable
// object in 'onInvoked()'.
public abstract class MonitoringMenuItem extends CheckableMenuItem
{
   public MonitoringMenuItem(HasValueChangeHandlers<Boolean> observable,
                             boolean initialValue,
                             boolean monitoredValue)
   {
      observable_ = observable;
      currentValue_ = initialValue;
      monitoredValue_ = monitoredValue;
      
      observable.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            currentValue_ = event.getValue();
            onStateChanged();
         }
      });
      
      onStateChanged();
   }

   @Override
   public String getLabel()
   {
      return monitoredValue_
            ? "Refresh Automatically"
            : "Manual Refresh Only";
   }

   @Override
   public boolean isChecked()
   {
      return monitoredValue_ == currentValue_;
   }
   
   private boolean currentValue_;
   
   protected final boolean monitoredValue_;
   protected final HasValueChangeHandlers<Boolean> observable_;
}

