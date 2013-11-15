/*
 * WorkbenchContext.java
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WorkbenchContext
{

   @Inject
   public WorkbenchContext(Session session, EventBus eventBus)
   {
      session_ = session;
      
      // track current working dir
      currentWorkingDir_ = FileSystemItem.home();
      defaultFileDialogDir_ = FileSystemItem.home();
      eventBus.addHandler(WorkingDirChangedEvent.TYPE, 
                          new WorkingDirChangedHandler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            currentWorkingDir_ = FileSystemItem.createDir(event.getPath());
            defaultFileDialogDir_ = FileSystemItem.createDir(event.getPath());;
         }      
      }); 
      
      eventBus.addHandler(BusyEvent.TYPE, new BusyHandler() {
         @Override
         public void onBusy(BusyEvent event)
         {
            isServerBusy_ = event.isBusy();
         } 
      });
      
      eventBus.addHandler(RestartStatusEvent.TYPE, 
                          new RestartStatusEvent.Handler()
      {
         @Override
         public void onRestartStatus(RestartStatusEvent event)
         {
            if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
               isRestartInProgress_ = true;
            else if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
            {
               // clear the restart in progress event after a delay
               // (it basically just controls whether errors are displayed
               // from get_environment_state, list_packages, etc.). we've 
               // seen issues where the flag is cleared too soon -- this
               // is likely an underlying logic problem in sendPing, but
               // we don't want to make a change to that late in the v0.98
               // cycle so instead we just delay the setting of the flag
               // finding a better solution is tracked in bug #3651
               new Timer() {
                  @Override
                  public void run()
                  {
                     isRestartInProgress_ = false;
                  }
               }.schedule(500);
            }
         }
      });
   }
   
  
  
   public FileSystemItem getCurrentWorkingDir()
   {
      return currentWorkingDir_;
   }
   
   public FileSystemItem getDefaultFileDialogDir()
   {
      if (defaultFileDialogDir_ != null)
         return defaultFileDialogDir_;
      else
         return getCurrentWorkingDir();
   }
   
   public void setDefaultFileDialogDir(FileSystemItem dir)
   {
      defaultFileDialogDir_ = dir;
   }
   
   // NOTE: mirrors behavior of rEnvironmentDir in SessionMain.cpp
   public String getREnvironmentPath()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (sessionInfo != null)
      {
         FileSystemItem rEnvDir = null;

         if (getActiveProjectDir() != null)
         {
            rEnvDir = getActiveProjectDir();
         }
         if (sessionInfo.getMode().equals(SessionInfo.DESKTOP_MODE))
         {
            rEnvDir = currentWorkingDir_;
         }
         else
         {
            rEnvDir = FileSystemItem.createDir(
                                       sessionInfo.getInitialWorkingDir());
         }
         return rEnvDir.completePath(".RData");
      }
      else
      {
         return FileSystemItem.home().completePath(".RData");
      }
   }
   
   public String getActiveProjectFile()
   {
      return session_.getSessionInfo().getActiveProjectFile();
   }
   
   public FileSystemItem getActiveProjectDir()
   {
      if (activeProjectDir_ == null)
      {
         SessionInfo sessionInfo = session_.getSessionInfo();
         if (sessionInfo != null &&
             sessionInfo.getActiveProjectFile() != null)
         {
            activeProjectDir_ = FileSystemItem.createFile(
                           sessionInfo.getActiveProjectFile()).getParentPath();
         }
      }
      return activeProjectDir_;
   }
   
   public boolean isProjectActive()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      return sessionInfo != null && sessionInfo.getActiveProjectFile() != null;
   }
   
   public boolean isServerBusy()
   {
      return isServerBusy_;
   }
   
   public boolean isRestartInProgress()
   {
      return isRestartInProgress_;
   }
   
   private boolean isServerBusy_ = false;
   private boolean isRestartInProgress_ = false;
   private FileSystemItem currentWorkingDir_ = FileSystemItem.home();
   private FileSystemItem defaultFileDialogDir_ = FileSystemItem.home();
   private FileSystemItem activeProjectDir_ = null;
   private Session session_; 
   
}
