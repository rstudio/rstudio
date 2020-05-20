/*
 * ProcessCallback.java
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.ProcessResult;
import org.rstudio.studio.client.server.ServerError;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class ProcessCallback extends SimpleRequestCallback<ProcessResult>
{
   public ProcessCallback(String title)
   {
      this(title, null);
   }
   
   public ProcessCallback(String title, String progressMessage)
   {
      this(title, progressMessage, 0);
   }
   
   public ProcessCallback(String title, 
                          String progressMessage,
                          int progressPaddingMs)
   {
      super(title);
      title_ = title;
      progressPaddingMs_ = progressPaddingMs;
      
      if (progressMessage != null)
      {
         GlobalDisplay globalDisplay =
               RStudioGinjector.INSTANCE.getGlobalDisplay();
         dismissProgress_ = globalDisplay.showProgress(progressMessage);
      }
   }
   
   @Override
   public void onResponseReceived(ProcessResult response)
   {
      if (!StringUtil.isNullOrEmpty(response.getOutput()))
      {
         dismissProgress();
         
         new ConsoleProgressDialog(title_,
                                   response.getOutput(),
                                   response.getExitCode()).showModal();
      }
      else
      {
         delayedDismissProgress();
      }
   }
   
   @Override
   public void onError(ServerError error)
   {
      dismissProgress();
      
      super.onError(error);
   }
   
   private void delayedDismissProgress()
   {
      if (dismissProgress_ != null)
      {
         if (progressPaddingMs_ > 0)
         {
            new Timer() {
               @Override
               public void run()
               {
                  dismissProgress();
               }   
            }.schedule(progressPaddingMs_);
         }
         else
         {
            dismissProgress();
         }
      }
   }
   
   private void dismissProgress()
   {
      if (dismissProgress_ != null)
      {
         dismissProgress_.execute();
         dismissProgress_ = null;
      }
   }

   private final String title_;
   private final int progressPaddingMs_;
   private Command dismissProgress_ = null;
}
