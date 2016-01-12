/*
 * ProgressIndicatorDelay.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

public class ProgressIndicatorDelay implements ProgressIndicator
{
   private ProgressIndicator progressIndicator_;
   private boolean showing_;
   
   public ProgressIndicatorDelay(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }
   
   @Override
   public void onProgress(final String status)
   {
      showing_ = true;
      new Timer()
      {
         @Override
         public void run()
         {
            if (showing_)
            {
               progressIndicator_.onProgress(status);
            }
         }
      }.schedule(1);
   }
   
   @Override
   public void onCompleted()
   {
      showing_ = false;
      new Timer()
      {
         @Override
         public void run()
         {
            progressIndicator_.onCompleted();
         }
      }.schedule(2000);
   }

   @Override
   public void onError(String message)
   {
      progressIndicator_.onError(message);
   }

   @Override
   public void clearProgress()
   {
      progressIndicator_.clearProgress();
   }
}
