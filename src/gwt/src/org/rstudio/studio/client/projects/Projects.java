/*
 * Projects.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
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
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowHandler;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.events.NewProjectEvent;
import org.rstudio.studio.client.projects.events.OpenProjectEvent;
import org.rstudio.studio.client.projects.model.NewProjectContext;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.OpenProjectParams;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectWizard;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.remote.RResult;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class Projects implements OpenProjectFileHandler,
                                 SwitchToProjectHandler,
                                 OpenProjectErrorHandler,
                                 OpenProjectNewWindowHandler,
                                 NewProjectEvent.Handler,
                                 OpenProjectEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, Projects> {}
   
   @Inject
   public Projects(GlobalDisplay globalDisplay,
                   final Session session,
                   Provider<ProjectMRUList> pMRUList,
                   SharedProject sharedProject,
                   RemoteFileSystemContext fsContext,
                   ApplicationQuit applicationQuit,
                   ProjectsServerOperations projServer,
                   PackratServerOperations packratServer,
                   ApplicationServerOperations appServer,
                   GitServerOperations gitServer,
                   EventBus eventBus,
                   Binder binder,
                   final Commands commands,
                   ProjectOpener opener,
                   Provider<ProjectPreferencesDialog> pPrefDialog,
                   Provider<WorkbenchContext> pWorkbenchContext,
                   Provider<UIPrefs> pUIPrefs)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      pMRUList_ = pMRUList;
      applicationQuit_ = applicationQuit;
      projServer_ = projServer;
      packratServer_ = packratServer;
      appServer_ = appServer;
      gitServer_ = gitServer;
      fsContext_ = fsContext;
      session_ = session;
      pWorkbenchContext_ = pWorkbenchContext;
      pPrefDialog_ = pPrefDialog;
      pUIPrefs_ = pUIPrefs;
      opener_ = opener;

      binder.bind(commands, this);
       
      eventBus.addHandler(OpenProjectErrorEvent.TYPE, this);
      eventBus.addHandler(SwitchToProjectEvent.TYPE, this);
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
      eventBus.addHandler(OpenProjectNewWindowEvent.TYPE, this);
      eventBus.addHandler(NewProjectEvent.TYPE, this);
      eventBus.addHandler(OpenProjectEvent.TYPE, this);
      
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
            if (!hasProject)
            {
               commands.setWorkingDirToProjectDir().remove();
               commands.showDiagnosticsProject().remove();
            }
            boolean enableProjectSharing = hasProject && 
                  sessionInfo.projectSupportsSharing();
            commands.shareProject().setEnabled(enableProjectSharing);
            commands.shareProject().setVisible(enableProjectSharing);
            
            // remove version control commands if necessary
            if (!sessionInfo.isVcsEnabled())
            {
               commands.activateVcs().remove();
               commands.layoutZoomVcs().remove();
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
               commands.layoutZoomVcs().setMenuLabel(
                                    "Zoom _" + sessionInfo.getVcsName());
               
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
            
            // disable the open project in new window if necessary
            if (!Desktop.isDesktop() || !sessionInfo.getMultiSession())
               commands.openProjectInNewWindow().remove();
            
            // maintain mru
            if (hasProject)
               mruList.add(activeProjectFile);
         }
      });
   }
   
   @Handler
   public void onClearRecentProjects()
   {  
      // Clear the contents of the most rencently used list of projects
      pMRUList_.get().clear();
   }
   
   @Handler
   public void onNewProject()
   {
      handleNewProject(false, true);
   }
   
   private void handleNewProject(boolean forceSaveAll, 
                                 final boolean allowOpenInNewWindow)
   {
      // first resolve the quit context (potentially saving edited documents   
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Save Current Workspace",
         forceSaveAll,
         new ApplicationQuit.QuitContext() {
           @Override
           public void onReadyToQuit(final boolean saveChanges)
           {
              projServer_.getNewProjectContext(
                new SimpleRequestCallback<NewProjectContext>() {
                   @Override
                   public void onResponseReceived(NewProjectContext context)
                   {
                      NewProjectWizard wiz = new NewProjectWizard(
                         session_.getSessionInfo(),
                         pUIPrefs_.get(),
                         pWorkbenchContext_.get(),
                         new NewProjectInput(
                            FileSystemItem.createDir(
                               pUIPrefs_.get().defaultProjectLocation().getValue()), 
                            context
                         ),
                         allowOpenInNewWindow,
                         
                         new ProgressOperationWithInput<NewProjectResult>() {

                          @Override
                          public void execute(NewProjectResult newProject, 
                                              ProgressIndicator indicator)
                          {
                             indicator.onCompleted();
                             createNewProject(newProject, saveChanges); 
                          }
                      });
                      wiz.showModal();  
                   }
              });
           }  
      });
      
      
   }
   

   @Override
   public void onNewProjectEvent(NewProjectEvent event)
   {
      handleNewProject(event.getForceSaveAll(), 
                       event.getAllowOpenInNewWindow());
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
               
               if (newProject.getUsePackrat() !=
                   uiPrefs.newProjUsePackrat().getValue())
               {
                  uiPrefs.newProjUsePackrat().setGlobalValue(
                                          newProject.getUsePackrat());
               }
               
               // call the server -- in all cases continue on with
               // creating the project (swallow errors updating the pref)
               projServer_.setUiPrefs(
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
                  indicator.onProgress("Cloning Git repository...");
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

      // Next, create the project itself -- depending on the type, this
      // could involve creating an R package, or Shiny application, and so on.
      createProjectCmds.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            // Validate the package name if we're creating a package
            if (newProject.getNewPackageOptions() != null)
            {
               final String packageName =
                     newProject.getNewPackageOptions().getPackageName();

               if (!PACKAGE_NAME_PATTERN.test(packageName))
               {
                  indicator.onError(
                        "Invalid package name '" + packageName + "': " +
                              "package names must start with a letter, and contain " +
                              "only letters and numbers."
                        );
                  return;
               }
            }
            
            indicator.onProgress("Creating project...");
            
            if (newProject.getNewPackageOptions() == null)
            {
               projServer_.createProject(
                     newProject.getProjectFile(),
                     newProject.getNewPackageOptions(),
                     newProject.getNewShinyAppOptions(),
                     newProject.getProjectTemplateOptions(),
                     new VoidServerRequestCallback(indicator)
                     {
                        @Override
                        public void onSuccess()
                        {
                           continuation.execute();
                        }
                     });
            }
            else
            {
               String projectFile = newProject.getProjectFile();
               String packageDirectory = projectFile.substring(0,
                     projectFile.lastIndexOf('/'));
               
               projServer_.packageSkeleton(
                     newProject.getNewPackageOptions().getPackageName(),
                     packageDirectory,
                     newProject.getNewPackageOptions().getCodeFiles(),
                     newProject.getNewPackageOptions().getUsingRcpp(),
                     new ServerRequestCallback<RResult<Void>>()
                     {
                        
                        @Override
                        public void onResponseReceived(RResult<Void> response)
                        {
                           if (response.failed())
                              indicator.onError(response.errorMessage());
                           else
                              continuation.execute();
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           indicator.onError(error.getUserMessage());
                        }
                     });
                     
            }

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
      
      // Generate a new packrat project
      if (newProject.getUsePackrat()) {
         createProjectCmds.addCommand(new SerializedCommand() 
         {
            
            @Override
            public void onExecute(final Command continuation) {
               
               indicator.onProgress("Initializing packrat project...");
               
               String projDir = FileSystemItem.createFile(
                  newProject.getProjectFile()
               ).getParentPathString();
               
               packratServer_.packratBootstrap(
                  projDir, 
                  false,
                  new VoidServerRequestCallback(indicator) {
                     @Override
                     public void onSuccess()
                     {
                        continuation.execute();
                     }
                  });
            }
         }, false);
      }
      
      if (newProject.getOpenInNewWindow())
      {
         createProjectCmds.addCommand(new SerializedCommand() {

            @Override
            public void onExecute(final Command continuation)
            {
               FileSystemItem project = FileSystemItem.createFile(
                                               newProject.getProjectFile());
               if (Desktop.isDesktop())
               {
                  Desktop.getFrame().openProjectInNewWindow(project.getPath());                   
                  continuation.execute();
               }
               else
               {
                  indicator.onProgress("Preparing to open project...");
                  serverOpenProjectInNewWindow(project, 
                                               newProject.getRVersion(),
                                               continuation); 
               } 
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
            
            if (!newProject.getOpenInNewWindow())
            {
               applicationQuit_.performQuit(
                                 saveChanges,
                                 newProject.getProjectFile(),
                                 newProject.getRVersion());
            }
            
            continuation.execute();
         }
      }, false);

      // Now set it all in motion!
      createProjectCmds.run();
   }


   @Handler
   public void onOpenProject()
   {
      showOpenProjectDialog(ProjectOpener.PROJECT_TYPE_FILE);
   }
   
   @Override
   public void onOpenProjectEvent(OpenProjectEvent event)
   {
      showOpenProjectDialog(ProjectOpener.PROJECT_TYPE_FILE,
                            event.getForceSaveAll(),
                            event.getAllowOpenInNewWindow());
   }
   
   @Handler
   public void onOpenProjectInNewWindow()
   {
      showOpenProjectDialog(ProjectOpener.PROJECT_TYPE_FILE,
         false,
         new ProgressOperationWithInput<OpenProjectParams>() 
         {
            @Override
            public void execute(final OpenProjectParams input,
                                ProgressIndicator indicator)
            {
               indicator.onCompleted();
               
               if (input == null)
                  return;
               
               eventBus_.fireEvent(
                   new OpenProjectNewWindowEvent(
                         input.getProjectFile().getPath(),
                         input.getRVersion()));
            }
         });
   }
   
   @Handler
   public void onOpenSharedProject()
   {
      showOpenProjectDialog(ProjectOpener.PROJECT_TYPE_SHARED);
   }
   
   @Override
   public void onOpenProjectNewWindow(OpenProjectNewWindowEvent event)
   {
      // call the desktop to open the project (since it is
      // a conventional foreground gui application it has
      // less chance of running afowl of desktop app creation
      // & activation restrictions)
      FileSystemItem project = FileSystemItem.createFile(event.getProject());
      if (Desktop.isDesktop())
         Desktop.getFrame().openProjectInNewWindow(project.getPath());
      else
         serverOpenProjectInNewWindow(project, event.getRVersion(), null);
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
   
   @Handler
   public void onPackratBootstrap()
   {
      showProjectOptions(ProjectPreferencesDialog.PACKRAT);
   }
   
   @Handler
   public void onPackratOptions()
   {
      showProjectOptions(ProjectPreferencesDialog.PACKRAT);
   }
   
   public void showProjectOptions(final int initialPane)
   {
      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator(
                                                      "Error Reading Options");
      indicator.onProgress("Reading options...");

      projServer_.readProjectOptions(new SimpleRequestCallback<RProjectOptions>() {

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
      switchToProject(event.getProject(), event.getForceSaveAll());
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
   
   private void switchToProject(String projectFilePath)
   {
      switchToProject(projectFilePath, false);
   }
   
   private void switchToProject(final String projectFilePath, 
                                final boolean forceSaveAll)
   {
      // validate that the switch will actually work
      projServer_.validateProjectPath(
                      projectFilePath,
                      new SimpleRequestCallback<Boolean>() {
                         
         @Override
         public void onResponseReceived(Boolean valid)
         {
            if (valid)
            {
               applicationQuit_.prepareForQuit("Switch Projects",
                  forceSaveAll,
                  new ApplicationQuit.QuitContext() {
                  public void onReadyToQuit(final boolean saveChanges)
                  {
                     applicationQuit_.performQuit(saveChanges, projectFilePath);
                  }
               });
            }
            else
            {
               // show error dialog
               String msg = "Project '" + projectFilePath + "' " +
                            "does not exist (it has been moved or deleted)";
               globalDisplay_.showErrorMessage("Error Opening Project", msg);
                
               // remove from mru list
               pMRUList_.get().remove(projectFilePath);
            }
         }
      });
   }
   
   private void showOpenProjectDialog(
                  int defaultType,
                  boolean allowOpenInNewWindow,
                  ProgressOperationWithInput<OpenProjectParams> onCompleted)
   {
      opener_.showOpenProjectDialog(fsContext_, projServer_,
            "~", 
            defaultType, allowOpenInNewWindow, onCompleted);
   }
   
   @Handler
   public void onShowDiagnosticsProject()
   {
      final ProgressIndicator indicator = globalDisplay_.getProgressIndicator("Lint");
      indicator.onProgress("Analyzing project sources...");
      projServer_.analyzeProject(new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            indicator.onCompleted();
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            indicator.onCompleted();
         }
      });
   }
   
   private void serverOpenProjectInNewWindow(FileSystemItem project,
                                             RVersionSpec rVersion,
                                             final Command onSuccess)
   {
      appServer_.getNewSessionUrl(
                    GWT.getHostPageBaseURL(),
                    true,
                    project.getParentPathString(), 
                    rVersion,
        new SimpleRequestCallback<String>() {

         @Override
         public void onResponseReceived(String url)
         {
            if (onSuccess != null)
               onSuccess.execute();
            
            globalDisplay_.openWindow(url);
         }
      });
   }
   
   private void showOpenProjectDialog(final int projectType)
   {
      showOpenProjectDialog(projectType, false, true);
   }
   
   private void showOpenProjectDialog(final int projectType, 
                                      boolean forceSaveAll,
                                      final boolean allowOpenInNewWindow)
   {
      // first resolve the quit context (potentially saving edited documents
      // and determining whether to save the R environment on exit)
      applicationQuit_.prepareForQuit("Switch Projects",
                                      forceSaveAll,
                                      new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            showOpenProjectDialog(projectType,
               allowOpenInNewWindow,
               new ProgressOperationWithInput<OpenProjectParams>() 
               {
                  @Override
                  public void execute(final OpenProjectParams input,
                                      ProgressIndicator indicator)
                  {
                     indicator.onCompleted();
                     
                     if (input == null || input.getProjectFile() == null)
                        return;
                     
                     if (input.inNewSession())
                     {
                        // open new window if requested
                        eventBus_.fireEvent(
                            new OpenProjectNewWindowEvent(
                                  input.getProjectFile().getPath(),
                                  input.getRVersion()));
                     }
                     else
                     {
                        // perform quit
                        applicationQuit_.performQuit(saveChanges,
                              input.getProjectFile().getPath());
                     }
                  }   
               });
            
         }
      }); 
   }

   private final Provider<ProjectMRUList> pMRUList_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations projServer_;
   private final PackratServerOperations packratServer_;
   private final ApplicationServerOperations appServer_;
   private final GitServerOperations gitServer_;
   private final RemoteFileSystemContext fsContext_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Provider<WorkbenchContext> pWorkbenchContext_;
   private final Provider<ProjectPreferencesDialog> pPrefDialog_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final ProjectOpener opener_;
   
   public static final String NONE = "none";
   public static final Pattern PACKAGE_NAME_PATTERN =
         Pattern.create("^[a-zA-Z][a-zA-Z0-9.]*$", "");
}
