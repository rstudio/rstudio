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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.ui.NewProjectDialog;
import org.rstudio.studio.client.projects.ui.NewProjectDialog.Result;
import org.rstudio.studio.client.projects.ui.ProjectOptionsDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.user.client.Command;
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
                   final Commands commands,
                   Provider<UIPrefs> pUIPrefs)
   {
      globalDisplay_ = globalDisplay;
      pMRUList_ = pMRUList;
      applicationQuit_ = applicationQuit;
      server_ = server;
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      session_ = session;
      pUIPrefs_ = pUIPrefs;
      
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
            
            // enable/disable commands
            String activeProjectFile = sessionInfo.getActiveProjectFile();
            boolean hasProject = activeProjectFile != null;
            commands.closeProject().setEnabled(hasProject);
            commands.projectOptions().setEnabled(hasProject);
              
            // maintain mru
            if (hasProject)
               mruList.add(activeProjectFile);
         }
      });
   }
   
   
   @Handler
   public void onNewProject()
   {
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Save Current Workspace",
                                      new ApplicationQuit.QuitContext() {
     
         @Override
         public void onReadyToQuit(final boolean saveChanges)
         {
            NewProjectDialog dlg = new NewProjectDialog(
              globalDisplay_,
              FileSystemItem.createDir(
                       pUIPrefs_.get().defaultProjectLocation().getValue()),
              new ProgressOperationWithInput<NewProjectDialog.Result>() {

               @Override
               public void execute(final Result newProject, 
                                   final ProgressIndicator indicator)
               {      
                  // create project command which can be invoked from 
                  // multiple contexts
                  final Command createProjCmd = new Command() {
                     @Override
                     public void execute()
                     {
                        // TODO Auto-generated method stub
                        indicator.onProgress("Creating project...");
                        
                        server_.createProject(
                           newProject.getProjectFile(),
                           new VoidServerRequestCallback(indicator) 
                           {
                              @Override 
                              public void onSuccess()
                              {
                                 applicationQuit_.performQuit(
                                                 saveChanges, 
                                                 newProject.getProjectFile());
                              } 
                           });
                     }
                     
                  };
                  
                  
                  // update default project location pref if necessary
                  if (newProject.getNewDefaultProjectLocation() != null)
                  {
                     indicator.onProgress("Saving default project location...");
                     
                     pUIPrefs_.get().defaultProjectLocation().setGlobalValue(
                                    newProject.getNewDefaultProjectLocation());
                     
                     // call the server -- in all cases continue on with
                     // creating the project (swallow errors updating the pref)
                     server_.setUiPrefs(
                          session_.getSessionInfo().getUiPrefs(), 
                          new ServerRequestCallback<Void>() {
                             @Override
                             public void onResponseReceived(Void response)
                             {
                                createProjCmd.execute();
                             }
                             
                             @Override
                             public void onError(ServerError error)
                             {
                                Debug.log(error.getUserMessage());
                                createProjCmd.execute();
                             }
                          });
                  }
                  else
                  {
                     createProjCmd.execute();
                  }  
               }
   
            });
            dlg.showModal();
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
               FileSystemItem.createDir(
                     pUIPrefs_.get().defaultProjectLocation().getValue()),
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
   
   @Handler
   public void onProjectOptions()
   {
      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator(
                  "Error Reading Options");
      indicator.onProgress("Reading options...");

      server_.readProjectConfig(new SimpleRequestCallback<RProjectConfig>() {

         @Override
         public void onResponseReceived(RProjectConfig config)
         {
            indicator.onCompleted();
            ProjectOptionsDialog dlg = new ProjectOptionsDialog(
               config,
               server_,
               new ProgressOperationWithInput<RProjectConfig>() {
                  @Override
                  public void execute(final RProjectConfig input,
                                      ProgressIndicator indicator)
                  {
                      indicator.onProgress("Saving options...");
                      server_.writeProjectConfig(
                            input, 
                            new VoidServerRequestCallback(indicator) {
                               @Override
                               public void onSuccess()
                               {
                                  // update prefs
                                  UIPrefs prefs = pUIPrefs_.get();
                                  prefs.useSpacesForTab().setProjectValue(
                                                 input.getUseSpacesForTab());
                                  prefs.numSpacesForTab().setProjectValue(
                                                 input.getNumSpacesForTab());
                                  prefs.defaultEncoding().setProjectValue(
                                                 input.getEncoding());
                                  
                               }
                            });
                  }
               });
            dlg.showModal();
        
         }});
   }

   @Override
   public void onOpenProjectFile(final OpenProjectFileEvent event)
   {
      // no-op for current project
      FileSystemItem projFile = event.getFile();
      if (projFile.getPath().equals(
                  session_.getSessionInfo().getActiveProjectFile()))
         return;
      
      // prompt to confirm
      String projectPath = projFile.getParentPathString();
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,  
         "Confirm Open Project",                             
         "Do you want to open the project " + projectPath + "?",                         
          new Operation() 
          { 
             public void execute()
             {
                 switchToProject(event.getFile().getPath());
             }
          },  
          true);   
   }
   

   @Override
   public void onSwitchToProject(final SwitchToProjectEvent event)
   {
      switchToProject(event.getProject());
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
   
   
   private void switchToProject(final String projectFilePath)
   {
      applicationQuit_.prepareForQuit("Switch Projects",
                                 new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            applicationQuit_.performQuit(saveChanges, projectFilePath);
         }}); 
   }
   
   private final Provider<ProjectMRUList> pMRUList_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations server_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fsContext_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final Provider<UIPrefs> pUIPrefs_;
   
   private static final String NONE = "none";

   
  
}
