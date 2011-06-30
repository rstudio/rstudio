/*
 * GlobalProgressDelayer.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class GlobalProgressDelayer
{
   public GlobalProgressDelayer(final GlobalDisplay globalDisplay,
                                final String progressMessage)
   {
      final int DELAY_MILLIS = 250;

      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            dismiss_ = globalDisplay.showProgress(progressMessage);
         }
      };
      timer_.schedule(DELAY_MILLIS);
   }

   public void dismiss()
   {
      timer_.cancel();
      if (dismiss_ != null)
         dismiss_.execute();
   }

   private final Timer timer_;
   private Command dismiss_;
}
