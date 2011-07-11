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
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Projects implements OpenProjectFileHandler,
                                 SwitchToProjectHandler,
                                 OpenProjectErrorHandler
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(GlobalDisplay globalDisplay,
                   final Session session,
                   Provider<ProjectMRUList> pMRUList,
                   FileDialogs fileDialogs,
                   RemoteFileSystemContext fsContext,
                   ApplicationQuit applicationQuit,
                   ProjectsServerOperations server,
                   EventBus eventBus,
                   Binder binder,
                   final Commands commands)
   {
      globalDisplay_ = globalDisplay;
      pMRUList_ = pMRUList;
      applicationQuit_ = applicationQuit;
      server_ = server;
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      
      binder.bind(commands, this);
      
      eventBus.addHandler(OpenProjectErrorEvent.TYPE, this);
      eventBus.addHandler(SwitchToProjectEvent.TYPE, this);
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
      
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            SessionInfo sessionInfo = session.getSessionInfo();
            
            // ensure mru is initialized
            ProjectMRUList mruList = pMRUList_.get();
            
            if(sessionInfo.isProjectsEnabled())
            {
               String activeProjectFile = sessionInfo.getActiveProjectFile();
               boolean hasProject = activeProjectFile != null;
          
               commands.closeProject().setEnabled(hasProject);
              
               // maintain mru
               if (hasProject)
                  mruList.add(activeProjectFile);
            }
            else
            {
               commands.newProject().remove();
               commands.openProject().remove();
               commands.projectMru0().remove();
               commands.projectMru1().remove();
               commands.projectMru2().remove();
               commands.projectMru3().remove();
               commands.projectMru4().remove();
               commands.projectMru5().remove();
               commands.projectMru6().remove();
               commands.projectMru7().remove();
               commands.projectMru8().remove();
               commands.projectMru9().remove();
               commands.clearRecentProjects().remove();
               commands.closeProject().remove();
            }
         }
      });
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
   
   
   @Handler
   public void onCloseProject()
   {
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Close Project",
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            applicationQuit_.performQuit(saveChanges, NONE);
         }});
   }
   
   

   @Override
   public void onOpenProjectFile(OpenProjectFileEvent event)
   {
  
   }
   
   @Override
   public void onSwitchToProject(final SwitchToProjectEvent event)
   {
      applicationQuit_.prepareForQuit("Switch Projects",
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            applicationQuit_.performQuit(saveChanges, event.getProject());
         }});
   }
   
   @Override
   public void onOpenProjectError(OpenProjectErrorEvent event)
   {
      // show error dialog
      String msg = "Project '" + event.getProject() + "' " +
                   "could not be opened: " + event.getMessage();
      globalDisplay_.showErrorMessage("Error Opening Project", msg);
       
      // remove from mru list
      pMRUList_.get().remove(event.getProject());
   }
 

   private final Provider<ProjectMRUList> pMRUList_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations server_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fsContext_;
   private final GlobalDisplay globalDisplay_;
   
   private static final String NONE = "none";

   
  
}
