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
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Projects implements OpenProjectFileHandler
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(GlobalDisplay globalDisplay,
                   FileDialogs fileDialogs,
                   RemoteFileSystemContext fsContext,
                   WorkbenchContext workbenchContext,
                   ApplicationQuit applicationQuit,
                   ProjectsServerOperations server,
                   EventBus eventBus,
                   Binder binder,
                   Commands commands)
   {
      globalDisplay_ = globalDisplay;
      applicationQuit_ = applicationQuit;
      server_ = server;
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      workbenchContext_ = workbenchContext;
      
      binder.bind(commands, this);
      
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
   }
   
   @Handler
   public void onNewProject()
   {
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Switch Projects",
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            // choose project folder
            fileDialogs_.chooseFolder(
               "New Project Directory", 
               fsContext_, 
               workbenchContext_.getCurrentWorkingDir(),
               new ProgressOperationWithInput<FileSystemItem>() {

                  @Override
                  public void execute(final FileSystemItem input,
                                      ProgressIndicator indicator)
                  {
                     // create the project
                     server_.createProject(
                        input.getPath(),
                        new VoidServerRequestCallback() 
                        {
                           @Override
                           public void onSuccess()
                           {
                              applicationQuit_.performQuit(
                                saveChanges,
                                input.completePath(input.getStem() + ".Rproj"));
                           }
                          
                           
                        });
                     
                  }
                  
               });
            
         }
      }); 
   }
   
   @Handler
   public void onOpenProject()
   {
      
   }

   @Override
   public void onOpenProjectFile(OpenProjectFileEvent event)
   {
  
   }
   
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations server_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fsContext_;
   private final WorkbenchContext workbenchContext_;

  
}
