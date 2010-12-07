/*
 * ProgressPanel.java
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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.widget.images.ProgressImages;

public class ProgressPanel extends Composite
{
   public ProgressPanel()
   { 
      progressImage_ = ProgressImages.createLarge();
      HorizontalCenterPanel progressPanel = new HorizontalCenterPanel(
                                                            progressImage_, 
                                                            100);
      progressImage_.setVisible(false);
      progressPanel.setSize("100%", "100%");
      initWidget(progressPanel);
   }
   
   public void beginProgressOperation(int delayMs)
   {
      progressImage_.setVisible(false);
      progressOperationPending_ = true ;
      
      Timer timer = new Timer() {
         public void run() {
            if (progressOperationPending_)
               progressImage_.setVisible(true);  
         }
      };
      timer.schedule(delayMs);
   }
   
   public void endProgressOperation()
   {
      progressOperationPending_ = false ;
      progressImage_.setVisible(false);
   }
   
   private Image progressImage_ ;
   private boolean progressOperationPending_ = false ;
}
