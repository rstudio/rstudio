/*
 * Projects.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
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
                   FileDialogs fileDialogs,
                   EventBus eventBus,
                   Binder binder,
                   Commands commands)
   {
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      
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
      
   }

   @Override
   public void onOpenProjectFile(OpenProjectFileEvent event)
   {
  
   }
   
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;

  
}
