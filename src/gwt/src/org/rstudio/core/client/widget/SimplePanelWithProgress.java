/*
 * SimplePanelWithProgress.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class SimplePanelWithProgress extends SimplePanel
                                  implements ProvidesResize,
                                             RequiresResize
{
   public SimplePanelWithProgress()
   { 
   }
   
   @Override
   public void setWidget(Widget widget)
   {
      if (loadProgressPanel_.equals(getWidget()))
         loadProgressPanel_.endProgressOperation();
      super.setWidget(widget);
      
   }
   
   public void showProgress(int delayMs)
   {
      setWidget(loadProgressPanel_);
      loadProgressPanel_.beginProgressOperation(delayMs);
   }

   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }

   private ProgressPanel loadProgressPanel_ = new ProgressPanel();
}
