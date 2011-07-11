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
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Projects implements OpenProjectFileHandler
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(Session session,
                   FileDialogs fileDialogs,
                   RemoteFileSystemContext fsContext,
                   ApplicationQuit applicationQuit,
                   ProjectsServerOperations server,
                   EventBus eventBus,
                   Binder binder,
                   Commands commands)
   {
      session_ = session;
      applicationQuit_ = applicationQuit;
      server_ = server;
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      
      binder.bind(commands, this);
      
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
   }
   
   @Handler
   public void onNewProject()
   {
      if (!projectsEnabled())
         return;
      
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Switch Projects",
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            // choose project folder
            fileDialogs_.saveFile(
               "New Project", 
               fsContext_, 
               FileSystemItem.home(),
               ".Rproj",
               true,
               new ProgressOperationWithInput<FileSystemItem>() 
               {
                  @Override
                  public void execute(final FileSystemItem input,
                                      ProgressIndicator indicator)
                  {  
                     if (input == null)
                     {
                        indicator.onCompleted();
                        return;
                     }
                     
                     // create the project
                     indicator.onProgress("Creating project...");
                     server_.createProject(
                        input.getPath(),
                        new VoidServerRequestCallback(indicator) 
                        {
                           @Override 
                           public void onSuccess()
                           {
                              applicationQuit_.performQuit(saveChanges,
                                                           input.getPath());
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
      if (!projectsEnabled())
         return;
      
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Switch Projects",
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            // choose project file
            fileDialogs_.openFile(
               "Open Project", 
               fsContext_, 
               FileSystemItem.home(),
               "R Projects (*.Rproj)",
               new ProgressOperationWithInput<FileSystemItem>() 
               {
                  @Override
                  public void execute(final FileSystemItem input,
                                      ProgressIndicator indicator)
                  {
                     indicator.onCompleted();
                     
                     if (input == null)
                        return;
                     
                     // perform quit
                     applicationQuit_.performQuit(saveChanges, input.getPath());
                  }
                  
               });
            
         }
      }); 
   }

   @Override
   public void onOpenProjectFile(OpenProjectFileEvent event)
   {
  
   }

   private boolean projectsEnabled()
   {
      return session_.getSessionInfo().isProjectsEnabled();
   }
   
   private final Session session_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations server_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fsContext_;

  
}
