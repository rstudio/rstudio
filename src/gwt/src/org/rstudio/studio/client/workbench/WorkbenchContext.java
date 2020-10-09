/*
 * WorkbenchContext.java
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
package org.rstudio.studio.client.workbench;

import java.util.List;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RVersionsChangedEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class WorkbenchContext
{

   @Inject
   public WorkbenchContext(Session session, EventBus eventBus,
         Provider<GitState> pGitState, Provider<UserPrefs> pUserPrefs)
   {
      session_ = session;
      pGitState_ = pGitState;
      pUserPrefs_ = pUserPrefs;

      // track current working dir
      currentWorkingDir_ = FileSystemItem.home();
      defaultFileDialogDir_ = FileSystemItem.home();
      eventBus.addHandler(WorkingDirChangedEvent.TYPE,
                          new WorkingDirChangedEvent.Handler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            currentWorkingDir_ = FileSystemItem.createDir(event.getPath());
            defaultFileDialogDir_ = FileSystemItem.createDir(event.getPath());
         }
      });

      eventBus.addHandler(BusyEvent.TYPE, busyEvent ->
      {
         isServerBusy_ = busyEvent.isBusy();

         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().setBusy(isServerBusy_);
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


      // track R version info
      eventBus.addHandler(RVersionsChangedEvent.TYPE,
                          new RVersionsChangedEvent.Handler()
      {
         @Override
         public void onRVersionsChanged(RVersionsChangedEvent event)
         {
            rVersionsInfo_ = event.getRVersionsInfo();
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

   public RVersionsInfo getRVersionsInfo()
   {
      if (rVersionsInfo_ != null)
         return rVersionsInfo_;
      else
         return session_.getSessionInfo().getRVersionsInfo();
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
         FileSystemItem rEnvDir;

         if (getActiveProjectDir() != null)
         {
            rEnvDir = FileSystemItem.createDir(
                                        sessionInfo.getProjectUserDataDir());
         }
         else if (sessionInfo.getMode() == SessionInfo.DESKTOP_MODE)
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

   public FileSystemItem getDefaultWorkingDir()
   {
      if (defaultWorkingDir_ == null)
      {
         SessionInfo sessionInfo = session_.getSessionInfo();
         defaultWorkingDir_ = FileSystemItem.createDir(
                                    sessionInfo.getDefaultWorkingDir());
      }
      return defaultWorkingDir_;
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

   public boolean isBuildInProgress()
   {
      return isBuildInProgress_;
   }

   public void setBuildInProgress(boolean inProgress)
   {
      isBuildInProgress_ = inProgress;
   }

   public String createWindowTitle()
   {
      FileSystemItem projDir = getActiveProjectDir();
      if (projDir != null)
      {
         String title;
         if (pUserPrefs_.get().fullProjectPathInWindowTitle().getValue())
            title = projDir.getPath();
         else
            title = projDir.getName();
         BranchesInfo branchInfo = pGitState_.get().getBranchInfo();
         if (branchInfo != null)
         {
            String branch = branchInfo.getActiveBranch();
            if (branch != null)
               title = title + " - " + branch;
         }
         return title;
      }
      return null;
   }

   public String getUserIdentity()
   {
      return session_.getSessionInfo().getUserIdentity();
   }
   
   public List<String> getDroppedUrls()
   {
      return droppedUrls_;
   }
   
   public void setDroppedUrls(List<String> droppedUrls)
   {
      droppedUrls_ = droppedUrls;
   }

   private boolean isServerBusy_ = false;
   private boolean isRestartInProgress_ = false;
   private boolean isBuildInProgress_ = false;
   private List<String> droppedUrls_ = null;
   private FileSystemItem currentWorkingDir_ = FileSystemItem.home();
   private FileSystemItem defaultFileDialogDir_ = FileSystemItem.home();
   private FileSystemItem defaultWorkingDir_ = null;
   private FileSystemItem activeProjectDir_ = null;
   private final Session session_;
   private final Provider<GitState> pGitState_;
   private final Provider<UserPrefs> pUserPrefs_;
   private RVersionsInfo rVersionsInfo_ = null;

}
