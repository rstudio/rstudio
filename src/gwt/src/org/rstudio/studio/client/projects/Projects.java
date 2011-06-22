package org.rstudio.studio.client.projects;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileHandler;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Projects implements OpenProjectFileHandler
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(GlobalDisplay globalDisplay,
                   EventBus eventBus,
                   Binder binder,
                   Commands commands)
   {
      globalDisplay_ = globalDisplay;
      
      binder.bind(commands, this);
      
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
   }
   
   @Handler
   public void onNewProject()
   {
      globalDisplay_.showMessage(MessageDialog.INFO, 
                                 "RStudio", 
                                 "New Project: Not Implemented");
      
   }
   
   @Handler
   public void onOpenProject()
   {
      globalDisplay_.showMessage(MessageDialog.INFO, 
                                 "RStudio", 
                                 "Open Project: Not Implemented");
   }

   private final GlobalDisplay globalDisplay_;

   @Override
   public void onOpenProjectFile(OpenProjectFileEvent event)
   {
  
   }
}
