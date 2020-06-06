/*
 * DebouncedCommand.java
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
package org.rstudio.core.client;

import com.google.gwt.user.client.Timer;

public abstract class DebouncedCommand
{
   public DebouncedCommand(int delayMs)
   {
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            execute();
         }
      };
      
      delayMs_ = delayMs;
      suspended_ = false;
   }
   
   public void nudge()
   {
      if (!suspended_)
         timer_.schedule(delayMs_);
   }
   
   public void suspend()
   {
      timer_.cancel();
      suspended_ = true;
   }
   
   public void resume()
   {
      suspended_ = false;
   }
   
   public boolean isRunning()
   {
      return timer_.isRunning();
   }
   
   protected abstract void execute();
   
   private final Timer timer_;
   private final int delayMs_;
   private boolean suspended_;
}
