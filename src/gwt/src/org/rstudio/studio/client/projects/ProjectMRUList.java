/*
 * ProjectMRUList.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.workbench.MRUList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProjectMRUList extends MRUList
{
   @Inject 
   public ProjectMRUList(Commands commands, 
                         WorkbenchListManager listManager,
                         final EventBus eventBus)
   {
      super(listManager.getProjectMruList(),
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
   
   @Override
   protected String transformMruEntryPath(String entryPath)
   {
      return FileSystemItem.createFile(entryPath).getParentPathString();
   }

}
