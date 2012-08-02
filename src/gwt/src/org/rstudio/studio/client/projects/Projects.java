/*
 * Projects.java
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
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
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectWizard;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialog;
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
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

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
                   GitServerOperations gitServer,
                   EventBus eventBus,
                   Binder binder,
                   final Commands commands,
                   Provider<ProjectPreferencesDialog> pPrefDialog,
                   Provider<UIPrefs> pUIPrefs)
   {
      globalDisplay_ = globalDisplay;
      pMRUList_ = pMRUList;
      applicationQuit_ = applicationQuit;
      server_ = server;
      gitServer_ = gitServer;
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      session_ = session;
      pPrefDialog_ = pPrefDialog;
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
            
            // remove version control commands if necessary
            if (!sessionInfo.isVcsEnabled())
            {
               commands.activateVcs().remove();
               commands.vcsCommit().remove();
               commands.vcsShowHistory().remove();
               commands.vcsPull().remove();
               commands.vcsPush().remove();
               commands.vcsCleanup().remove();
            }
            else
            {
               commands.activateVcs().setMenuLabel(
                                    "Show _" + sessionInfo.getVcsName());
               
               // customize for svn if necessary
               if (sessionInfo.getVcsName().equals(VCSConstants.SVN_ID))
               {
                  commands.vcsPush().remove();
                  commands.vcsPull().setButtonLabel("Update");
                  commands.vcsPull().setMenuLabel("_Update");
               }    
               
               // customize for git if necessary
               if (sessionInfo.getVcsName().equals(VCSConstants.GIT_ID))
               {
                  commands.vcsCleanup().remove();
               }
            }
            
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
                 session_.getSessionInfo(),
                 pUIPrefs_.get(),
                 FileSystemItem.createDir(
                    pUIPrefs_.get().defaultProjectLocation().getValue()),
                 new ProgressOperationWithInput<NewProjectResult>() {

                  @Override
                  public void execute(final NewProjectResult newProject, 
                                      final ProgressIndicator indicator)
                  {
                     indicator.onCompleted();
                     createNewProject(newProject, saveChanges); 
                  }
              });
              wiz.showModal();  
           }  
      });
      
      
   }
   

   private void createNewProject(final NewProjectResult newProject,
                                 final boolean saveChanges)
   {
    
      
      // This gets a little crazy. We have several pieces of asynchronous logic
      // that each may or may not need to be executed, depending on the type
      // of project being created and on whether the previous pieces of logic
      // succeed. Plus we have this ProgressIndicator that needs to be fed
      // properly.

      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator(
                                                      "Error Creating Project");
      
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
            UIPrefs uiPrefs = pUIPrefs_.get();
            
            // update default project location pref if necessary
            if ((newProject.getNewDefaultProjectLocation() != null) ||
                (newProject.getCreateGitRepo() != 
                 uiPrefs.newProjGitInit().getValue()))
            {
               indicator.onProgress("Saving defaults...");

               if (newProject.getNewDefaultProjectLocation() != null)
               {
                  uiPrefs.defaultProjectLocation().setGlobalValue(
                     newProject.getNewDefaultProjectLocation());
               }
               
               if (newProject.getCreateGitRepo() != 
                   uiPrefs.newProjGitInit().getValue())
               {
                  uiPrefs.newProjGitInit().setGlobalValue(
                                          newProject.getCreateGitRepo());
               }
               
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

      // Next, if necessary, clone a repo
      if (newProject.getVcsCloneOptions() != null)
      {
         createProjectCmds.addCommand(new SerializedCommand()
         {
            @Override
            public void onExecute(final Command continuation)
            {
               VcsCloneOptions cloneOptions = newProject.getVcsCloneOptions();
               
               if (cloneOptions.getVcsName().equals((VCSConstants.GIT_ID)))
                  indicator.onProgress("Cloning Git repoistory...");
               else
                  indicator.onProgress("Checking out SVN repository...");
               
               gitServer_.vcsClone(
                     cloneOptions,
                     new ServerRequestCallback<ConsoleProcess>() {
                        @Override
                        public void onResponseReceived(ConsoleProcess proc)
                        {
                           final ConsoleProgressDialog consoleProgressDialog = 
                                 new ConsoleProgressDialog(proc, gitServer_);
                           consoleProgressDialog.showModal();
           
                           proc.addProcessExitHandler(new ProcessExitEvent.Handler()
                           {
                              @Override
                              public void onProcessExit(ProcessExitEvent event)
                              {
                                 if (event.getExitCode() == 0)
                                 {
                                    consoleProgressDialog.hide();
                                    continuation.execute();
                                 }
                                 else
                                 {
                                    indicator.onCompleted();
                                 }
                              }
                           }); 
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
                  newProject.getNewPackageOptions(),
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
      
      // Next, initialize a git repo if requested
      if (newProject.getCreateGitRepo())
      {
         createProjectCmds.addCommand(new SerializedCommand()
         {
            @Override
            public void onExecute(final Command continuation)
            {
               indicator.onProgress("Initializing git repository...");

               String projDir = FileSystemItem.createFile(
                     newProject.getProjectFile()).getParentPathString();
               
               gitServer_.gitInitRepo(
                     projDir,
                     new VoidServerRequestCallback(indicator)
                     {
                        @Override
                        public void onSuccess()
                        {
                           continuation.execute();
                        }
                        
                        @Override
                        public void onFailure()
                        {
                           continuation.execute();
                        }
                     });
            }
         }, false);
      }

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
      showProjectOptions(ProjectPreferencesDialog.GENERAL);
   }

   @Handler
   public void onProjectSweaveOptions()
   {
      showProjectOptions(ProjectPreferencesDialog.SWEAVE);
   }
   
   @Handler
   public void onBuildToolsProjectSetup()
   {   
      // check whether there is a project active
      if (!hasActiveProject())
      { 
         globalDisplay_.showMessage(
               MessageDialog.INFO, 
               "No Active Project", 
               "Build tools can only be configured from within an " +
               "RStudio project.");
        
      }
      else
      {
         showProjectOptions(ProjectPreferencesDialog.BUILD);
      }
   }
   
   
   @Handler
   public void onVersionControlProjectSetup()
   {
      // check whether there is a project active
      if (!hasActiveProject())
      { 
         globalDisplay_.showMessage(
               MessageDialog.INFO, 
               "No Active Project", 
               "Version control features can only be accessed from within an " +
               "RStudio project. Note that if you have an existing directory " +
               "under version control you can associate an RStudio project " +
               "with that directory using the New Project dialog.");
        
      }
      else
      {
         showProjectOptions(ProjectPreferencesDialog.VCS);
      }
   }
   
   private void showProjectOptions(final int initialPane)
   {
      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator(
                                                      "Error Reading Options");
      indicator.onProgress("Reading options...");

      server_.readProjectOptions(new SimpleRequestCallback<RProjectOptions>() {

         @Override
         public void onResponseReceived(RProjectOptions options)
         {
            indicator.onCompleted();

            ProjectPreferencesDialog dlg = pPrefDialog_.get();
            dlg.initialize(options);
            dlg.activatePane(initialPane);
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
   
   private boolean hasActiveProject()
   {
      return session_.getSessionInfo().getActiveProjectFile() != null;
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
   private final GitServerOperations gitServer_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fsContext_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final Provider<ProjectPreferencesDialog> pPrefDialog_;
   private final Provider<UIPrefs> pUIPrefs_;
   
   public static final String NONE = "none";

   
  
}
