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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.ui.ProjectOptionsDialog;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectWizard;
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
import org.rstudio.studio.client.workbench.views.vcs.ConsoleProgressDialog;

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
            
            // disable the open project in new window command in web mode
            if (!Desktop.isDesktop())
               commands.openProjectInNewWindow().remove();
            
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
            NewProjectWizard wiz = new NewProjectWizard(
              FileSystemItem.createDir(
                       pUIPrefs_.get().defaultProjectLocation().getValue()),
              new ProgressOperationWithInput<NewProjectResult>() {

               @Override
               public void execute(final NewProjectResult newProject, 
                                   final ProgressIndicator indicator)
               {
                  createNewProject(newProject, indicator, saveChanges);
               }
   
            });
            wiz.showModal();
         }
      }); 
   }
   

   private void createNewProject(final NewProjectResult newProject,
                                 final ProgressIndicator indicator,
                                 final boolean saveChanges)
   {
      // This gets a little crazy. We have several pieces of asynchronous logic
      // that each may or may not need to be executed, depending on the type
      // of project being created and on whether the previous pieces of logic
      // succeed. Plus we have this ProgressIndicator that needs to be fed
      // properly.


      // Here's the command queue that will hold the various operations.
      final SerializedCommandQueue createProjectCmds =
                                                  new SerializedCommandQueue();

      // WARNING: When calling addCommand, BE SURE TO PASS FALSE as the second
      // argument, to delay running of the commands until they are all
      // scheduled.

      // First, attempt to update the default project location pref
      createProjectCmds.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
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
                     new VoidServerRequestCallback(indicator) {
                        @Override
                        public void onResponseReceived(Void response)
                        {
                           continuation.execute();
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           super.onError(error);
                           continuation.execute();
                        }
                     });
            }
            else
            {
               continuation.execute();
            }
         }
      }, false);

      // Next, if necessary, clone the git repo
      if (newProject.getGitRepoUrl() != null)
      {
         createProjectCmds.addCommand(new SerializedCommand()
         {
            @Override
            public void onExecute(final Command continuation)
            {
               indicator.onProgress("Cloning git repoistory...");
               
               server_.vcsClone(
                     newProject.getGitRepoUrl(),
                     newProject.getNewDefaultProjectLocation(),
                     new ServerRequestCallback<ConsoleProcess>() {
                        boolean continuationExecuted_ = false;

                        @Override
                        public void onResponseReceived(ConsoleProcess proc)
                        {
                           proc.addProcessExitHandler(new ProcessExitEvent.Handler()
                           {
                              @Override
                              public void onProcessExit(ProcessExitEvent event)
                              {
                                 if (event.getExitCode() == 0)
                                 {
                                    if (!continuationExecuted_)
                                    {
                                       continuationExecuted_ = true;
                                       continuation.execute();
                                    }
                                 }
                              }
                           });
                           ConsoleProgressDialog consoleProgressDialog = new ConsoleProgressDialog(
                                 "Clone Repository",
                                 proc);
                           consoleProgressDialog.addCloseHandler(new CloseHandler<PopupPanel>()
                           {
                              @Override
                              public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
                              {
                                 if (!continuationExecuted_)
                                 {
                                    continuationExecuted_ = true;
                                    continuation.execute();
                                 }
                              }
                           });
                           consoleProgressDialog.showModal();
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           indicator.onError(error.getUserMessage());
                        }
                     });
            }
         }, false);
      }

      // Next, create the project file
      createProjectCmds.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
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
                        continuation.execute();
                     }
                  });
         }
      }, false);

      // If we get here, dismiss the progress indicator
      createProjectCmds.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(Command continuation)
         {
            indicator.onCompleted();
            continuation.execute();
         }
      }, false);

      // Now set it all in motion!
      createProjectCmds.run();
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
            showOpenProjectDialog(
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
   public void onOpenProjectInNewWindow()
   {
      showOpenProjectDialog(
         new ProgressOperationWithInput<FileSystemItem>() 
         {
            @Override
            public void execute(final FileSystemItem input,
                                ProgressIndicator indicator)
            {
               indicator.onCompleted();
               
               if (input == null)
                  return;
               
               // call the desktop to open the project (since it is
               // a conventional foreground gui application it has
               // less chance of running afowl of desktop app creation
               // & activation restrictions)
               Desktop.getFrame().openProjectInNewWindow(input.getPath());
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
      // project options for current project
      FileSystemItem projFile = event.getFile();
      if (projFile.getPath().equals(
                  session_.getSessionInfo().getActiveProjectFile()))
      {
         onProjectOptions();
         return;
      }
      
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
   
   private void showOpenProjectDialog(
                  ProgressOperationWithInput<FileSystemItem> onCompleted)
   {
      // choose project file
      fileDialogs_.openFile(
         "Open Project", 
         fsContext_, 
         FileSystemItem.createDir(
               pUIPrefs_.get().defaultProjectLocation().getValue()),
         "R Projects (*.Rproj)",
         onCompleted);  
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
