/*
 * GlobalProgressDelayer.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

public class GlobalProgressDelayer
{
   public GlobalProgressDelayer(GlobalDisplay globalDisplay,
                                final int delayMillis,
                                String progressMessage)
   {
      this(globalDisplay, delayMillis, progressMessage, null);
   }

   public GlobalProgressDelayer(GlobalDisplay globalDisplay,
                                final int delayMillis,
                                String progressMessage,
                                Operation onCancel)
   {
      globalDisplay_ = globalDisplay;
      progressMessage_ = progressMessage;
      onCancel_ = onCancel;

      if (delayMillis > 0)
      {
         timer_ = new Timer() {
            @Override
            public void run() {
               ensureIndicator();
            }
         };
         timer_.schedule(delayMillis);
      }
      else {
         ensureIndicator();
      }
   }

   // NOTE: auto-creates the indicator if it doesn't already exist
   public ProgressIndicator getIndicator()
   {
      ensureIndicator();
      return indicator_;
   }

   public void setMessage(String progressMessage)
   {
      if (indicator_ != null)
         indicator_.onProgress(progressMessage, onCancel_);
      else
         progressMessage_ = progressMessage;
   }

   public void dismiss()
   {
      if (timer_ != null )
      {
         timer_.cancel();
         timer_ = null;
      }
      
      if (indicator_ != null)
      {
         indicator_.onCompleted();
         indicator_ = null;
      }
   }

   private void ensureIndicator()
   {
      if (indicator_ == null)
      {
         indicator_ = globalDisplay_.getProgressIndicator("Error");
         indicator_.onProgress(progressMessage_, onCancel_);
      }
   }

   protected final GlobalDisplay globalDisplay_;
   private String progressMessage_;
   private Timer timer_ = null;
   private ProgressIndicator indicator_ = null;
   private Operation onCancel_ = null;
}
