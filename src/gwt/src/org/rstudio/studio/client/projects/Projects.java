package org.rstudio.studio.client.projects;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Projects
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(GlobalDisplay globalDisplay,
                   Binder binder,
                   Commands commands)
   {
      binder.bind(commands, this);
      
      globalDisplay_ = globalDisplay;
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
}
