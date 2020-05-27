/*
 * ProgressDelayer.java
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
package org.rstudio.studio.client.common;

import org.rstudio.core.client.widget.ProgressIndicator;

import com.google.gwt.user.client.Timer;

public class ProgressDelayer
{
   public ProgressDelayer(ProgressIndicator indicator, 
         final int delayMillis, final String progressMessage)
   {
      indicator_ = indicator;
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            indicator_.onProgress(progressMessage);
         }
      };
      timer_.schedule(delayMillis);
   }

   public void dismiss()
   {
      if (timer_ != null )
      {
         timer_.cancel();
      }
      
      if (indicator_ != null)
      {
         indicator_.clearProgress();
      }
   }

   private final Timer timer_;
   private final ProgressIndicator indicator_;
}
