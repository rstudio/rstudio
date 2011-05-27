/*
 * IntervalTracker.java
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
package org.rstudio.core.client;

public class IntervalTracker
{
   public IntervalTracker(long intervalMillis, boolean startElapsed)
   {
      threshold_ = intervalMillis;
      if (!startElapsed)
         reset();
   }

   public void reset()
   {
      lastTime_ = System.currentTimeMillis();
   }

   public boolean hasElapsed()
   {
      return lastTime_ == null
             || System.currentTimeMillis() - lastTime_ >= threshold_;
   }

   private Long lastTime_;
   private final long threshold_;
}
