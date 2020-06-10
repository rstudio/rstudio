/*
 * MonitoringMenuItem.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

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
   public MonitoringMenuItem(ToolbarButton refreshButton,
                             HasValueChangeHandlers<Boolean> observable,
                             boolean autoRefreshEnabled,
                             boolean monitoredValue)
   {
      observable_ = observable;
      autoRefreshEnabled_ = autoRefreshEnabled;
      monitoredValue_ = monitoredValue;
      
      refreshButton_ = refreshButton;
      autoRefreshImage_ = new ImageResource2x(ThemeResources.INSTANCE.refreshWorkspaceMonitored2x());
      manualRefreshImage_ = new ImageResource2x(ThemeResources.INSTANCE.refreshWorkspaceUnmonitored2x());
      
      observable.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            autoRefreshEnabled_ = event.getValue();
            updateRefreshButton();
            onStateChanged();
         }
      });
      
      updateRefreshButton();
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
      return monitoredValue_ == autoRefreshEnabled_;
   }
   
   private void updateRefreshButton()
   {
      refreshButton_.setLeftImage(autoRefreshEnabled_ ? autoRefreshImage_ : manualRefreshImage_);
   }
   
   private boolean autoRefreshEnabled_;
   
   protected final HasValueChangeHandlers<Boolean> observable_;
   protected final boolean monitoredValue_;
 
   private final ToolbarButton refreshButton_;
   private final ImageResource2x autoRefreshImage_;
   private final ImageResource2x manualRefreshImage_;
}

