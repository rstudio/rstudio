/*
 * SimplePanelWithProgress.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.widget.images.ProgressImages;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class SimplePanelWithProgress extends SimplePanel
                                  implements ProvidesResize,
                                             RequiresResize
{
   public ProgressPanel createProgressPanel(Widget image, int offset)
   {
      return new ProgressPanel(image, offset);
   }
   
   public SimplePanelWithProgress()
   { 
      this(ProgressImages.createLarge(), 100);
   }
   
   public SimplePanelWithProgress(Widget progressImage)
   { 
      this(progressImage, 100);
   }
   
   public SimplePanelWithProgress(Widget progressImage, int verticalOffset)
   {
      loadProgressPanel_ = createProgressPanel(progressImage, verticalOffset);
   }
   
   @Override
   public void setWidget(Widget widget)
   {
      if (isProgressShowing())
         loadProgressPanel_.endProgressOperation();
      super.setWidget(widget);
      
   }

   public void showProgress(int delayMs)
   {
      showProgress(delayMs, null);
   }
   
   public void showProgress(int delayMs, String message)
   {
      if (!isProgressShowing())
      {
         setWidget(loadProgressPanel_);
         loadProgressPanel_.beginProgressOperation(delayMs, message);
      }
   }
   
   public boolean isProgressShowing()
   {
      return loadProgressPanel_.equals(getWidget());
   }
   
   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }
   
   private ProgressPanel loadProgressPanel_ = new ProgressPanel();
}
