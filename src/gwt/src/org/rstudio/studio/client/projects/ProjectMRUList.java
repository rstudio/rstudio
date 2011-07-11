package org.rstudio.studio.client.projects;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.workbench.MRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProjectMRUList extends MRUList
{
   @Inject 
   public ProjectMRUList(Commands commands, 
                         Session session,
                         final EventBus eventBus)
   {
      super(commands, 
            session, 
            "project-mru",
            new AppCommand[] {
                  commands.projectMru0(),
                  commands.projectMru1(),
                  commands.projectMru2(),
                  commands.projectMru3(),
                  commands.projectMru4(),
                  commands.projectMru5(),
                  commands.projectMru6(),
                  commands.projectMru7(),
                  commands.projectMru8(),
                  commands.projectMru9()
            },
            commands.clearRecentProjects(),
            false,
            new OperationWithInput<String>() 
            {
               @Override
               public void execute(String file)
               {
                  eventBus.fireEvent(new SwitchToProjectEvent(file));
               }
            });
   }

}
