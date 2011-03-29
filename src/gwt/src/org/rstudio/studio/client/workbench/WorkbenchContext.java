package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WorkbenchContext 
{

   @Inject
   public WorkbenchContext(EventBus eventBus)
   {
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
   
  
   FileSystemItem getCurrentWorkingDir()
   {
      return currentWorkingDir_;
   }
   
   
   FileSystemItem currentWorkingDir_ = FileSystemItem.home();
}
