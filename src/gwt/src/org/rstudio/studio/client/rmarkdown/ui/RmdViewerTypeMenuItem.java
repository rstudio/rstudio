/*
 * RmdViewerTypeMenuItem.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.widget.CheckableMenuItem;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class RmdViewerTypeMenuItem extends CheckableMenuItem
{
   public RmdViewerTypeMenuItem(int viewerType, String label, UIPrefs uiPrefs)
   {
      viewerType_ = viewerType;
      label_ = label;
      uiPrefs_ = uiPrefs;
      uiPrefs.rmdViewerType().addValueChangeHandler(
            new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> arg0)
         {
            onStateChanged();
         }
      });
      onStateChanged();
   }

   @Override
   public boolean isChecked()
   {
      return uiPrefs_ != null && 
             uiPrefs_.rmdViewerType().getValue() == viewerType_;
   }

   @Override
   public void onInvoked()
   {
      uiPrefs_.rmdViewerType().setProjectValue(viewerType_, true);
      uiPrefs_.writeUIPrefs();
   }

   @Override
   public String getLabel()
   {
      return label_ == null ? "" : label_;
   }
   
   int viewerType_;
   UIPrefs uiPrefs_;
   String label_;
}
