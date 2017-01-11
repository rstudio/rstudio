/*
 * ProgressPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.widget.images.ProgressImages;

public class ProgressPanel extends Composite
{
   public ProgressPanel()
   {
      this(ProgressImages.createLarge());
   }
   
   public ProgressPanel(Widget progressImage)
   {
      this(progressImage, 100);
   }
   
   public ProgressPanel(Widget progressImage, int verticalOffset)
   { 
      progressImage_ = progressImage;
      HorizontalCenterPanel progressPanel = new HorizontalCenterPanel(
                                                            progressImage_, 
                                                            verticalOffset);
      progressImage_.setVisible(false);
      progressPanel.setSize("100%", "100%");
      initWidget(progressPanel);
   }
   
   public void beginProgressOperation(int delayMs)
   {
      clearTimer();
      progressImage_.setVisible(false);

      timer_ = new Timer() {
         public void run() {
            if (timer_ != this)
               return; // This should never happen, but, just in case

            progressImage_.setVisible(true);
         }
      };
      timer_.schedule(delayMs);
   }

   public void endProgressOperation()
   {
      clearTimer();
      progressImage_.setVisible(false);
   }

   private void clearTimer()
   {
      if (timer_ != null)
      {
         timer_.cancel();
         timer_ = null;
      }
   }

   private final Widget progressImage_ ;
   private Timer timer_;
}
