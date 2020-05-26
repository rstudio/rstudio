/*
 * TimedProgressIndicator.java
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;

import com.google.gwt.user.client.Timer;

/**
 * Wraps a progress indicator with a timer to allow messages to persist after
 * the operation is complete.
 */
public class TimedProgressIndicator
             implements ProgressIndicator
{
   public TimedProgressIndicator(ProgressIndicator indicator)
   {
      indicator_ = indicator;
      completed_ = false;
   }
   
   /**
    * Displays a message on the progress indicator for at least the given length
    * of time, even after the progress operation is complete.
    * 
    * @param message The message to display.
    * @param minDisplayTime The minimum length of time to display the message.
    */
   public void onTimedProgress(String message, int minDisplayTime)
   {
      onProgress(message);
      if (timer_ == null)
      {
         timer_ = new Timer()
         {
            @Override
            public void run()
            {
               // if the operation completed while the timer was running, finish
               // now
               if (completed_)
                  onCompleted();
            }
         };
      }
      timer_.schedule(minDisplayTime);
   }

   @Override
   public void onProgress(String message)
   {
      indicator_.onProgress(message);
   }

   @Override
   public void onProgress(String message, Operation onCancel)
   {
      indicator_.onProgress(message, onCancel);
   }

   @Override
   public void onCompleted()
   {
      if (timer_ != null && timer_.isRunning())
      {
         completed_ = true;
         return;
      }
      indicator_.onCompleted();
   }

   @Override
   public void onError(String message)
   {
      indicator_.onError(message);
   }

   @Override
   public void clearProgress()
   {
      indicator_.clearProgress();
   }
   
   private Timer timer_;
   private final ProgressIndicator indicator_;
   private boolean completed_ = false;
}
