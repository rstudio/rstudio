package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;

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
      eventBus.addHandler(WorkingDirChangedEvent.TYPE, 
                          new WorkingDirChangedHandler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            currentWorkingDir_ = FileSystemItem.createDir(event.getPath());
         }      
      }); 
   }
   
  
   public FileSystemItem getCurrentWorkingDir()
   {
      return currentWorkingDir_;
   }
   
   // mirrors behavior of rEnvironmentDir in SessionMain.cpp
   public String getREnvironmentPath()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (sessionInfo != null)
      {
         FileSystemItem rEnvDir = null;
         if (sessionInfo.getMode().equals(SessionInfo.DESKTOP_MODE))
            rEnvDir = currentWorkingDir_;
         else
            rEnvDir = FileSystemItem.createDir(
                                       sessionInfo.getInitialWorkingDir());
         return rEnvDir.completePath(".RData");
      }
      else
      {
         return FileSystemItem.home().completePath(".RData");
      }
   }
   
   
   FileSystemItem currentWorkingDir_ = FileSystemItem.home();
   Session session_;
}
