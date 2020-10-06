/*
 * Projects.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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


import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationTutorialEvent;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.TutorialApiCallContext;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.common.vcs.VcsCloneOptions;
import org.rstudio.studio.client.projects.events.OpenProjectErrorEvent;
import org.rstudio.studio.client.projects.events.OpenProjectErrorHandler;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.events.OpenProjectFileHandler;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowHandler;
import org.rstudio.studio.client.projects.events.RequestOpenProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectHandler;
import org.rstudio.studio.client.projects.events.NewProjectEvent;
import org.rstudio.studio.client.projects.events.NewProjectFolderEvent;
import org.rstudio.studio.client.projects.events.NewProjectFromVcsEvent;
import org.rstudio.studio.client.projects.events.OpenProjectEvent;
import org.rstudio.studio.client.projects.model.NewProjectContext;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewProjectResult;
import org.rstudio.studio.client.projects.model.OpenProjectParams;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectWizard;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialog;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.remote.RResult;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.SessionOpener;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.function.Consumer;

@Singleton
public class Projects implements OpenProjectFileHandler,
                                 SwitchToProjectHandler,
                                 OpenProjectErrorHandler,
                                 OpenProjectNewWindowHandler,
                                 NewProjectEvent.Handler,
                                 NewProjectFromVcsEvent.Handler,
                                 NewProjectFolderEvent.Handler,
                                 OpenProjectEvent.Handler,
                                 RequestOpenProjectEvent.Handler
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
                   RenvServerOperations renvServer,
                   GitServerOperations gitServer,
                   EventBus eventBus,
                   Binder binder,
                   final Commands commands,
                   ProjectOpener opener,
                   SessionOpener sessionOpener,
                   Provider<ProjectPreferencesDialog> pPrefDialog,
                   Provider<WorkbenchContext> pWorkbenchContext,
                   Provider<UserPrefs> pUIPrefs)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      pMRUList_ = pMRUList;
      applicationQuit_ = applicationQuit;
      projServer_ = projServer;
      renvServer_ = renvServer;
      gitServer_ = gitServer;
      fsContext_ = fsContext;
      session_ = session;
      pWorkbenchContext_ = pWorkbenchContext;
      pPrefDialog_ = pPrefDialog;
      pUserPrefs_ = pUIPrefs;
      opener_ = opener;
      sessionOpener_ = sessionOpener;

      binder.bind(commands, this);

      eventBus.addHandler(OpenProjectErrorEvent.TYPE, this);
      eventBus.addHandler(SwitchToProjectEvent.TYPE, this);
      eventBus.addHandler(OpenProjectFileEvent.TYPE, this);
      eventBus.addHandler(OpenProjectNewWindowEvent.TYPE, this);
      eventBus.addHandler(NewProjectEvent.TYPE, this);
      eventBus.addHandler(NewProjectFromVcsEvent.TYPE, this);
      eventBus.addHandler(NewProjectFolderEvent.TYPE, this);
      eventBus.addHandler(OpenProjectEvent.TYPE, this);
      eventBus.addHandler(RequestOpenProjectEvent.TYPE, this);

      eventBus.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
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
            if (sessionInfo.getVcsName() == VCSConstants.SVN_ID)
            {
               commands.vcsPush().remove();
               commands.vcsPull().setButtonLabel("Update");
               commands.vcsPull().setMenuLabel("_Update");
            }

            // customize for git if necessary
            if (sessionInfo.getVcsName() == VCSConstants.GIT_ID)
            {
               commands.vcsCleanup().remove();
            }
         }

         // disable the open project in new window if necessary
         if (!Desktop.hasDesktopFrame() || !sessionInfo.getMultiSession())
            commands.openProjectInNewWindow().remove();

         // maintain mru
         if (hasProject)
            mruList.add(activeProjectFile);
      });
   }

   @Handler
   public void onClearRecentProjects()
   {
      // Clear the contents of the most recently used list of projects
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
         true /*allowCancel*/,
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
                         pUserPrefs_.get(),
                         pWorkbenchContext_.get(),
                         new NewProjectInput(
                            FileSystemItem.createDir(
                               pUserPrefs_.get().defaultProjectLocation().getValue()),
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

   @Override
   public void onNewProjectFromVcsEvent(NewProjectFromVcsEvent event)
   {
      if (event.getVcsId() != VCSConstants.GIT_ID)
      {
         Debug.logWarning("Unsupported VCS type invoked through API");
         return;
      }

      boolean saveChanges = false;
      String url = event.getRepoUrl().trim();
      String username = event.getUsername().trim();
      String checkoutDir = event.getDirName().trim();
      String dir = event.getDestDir().trim();
      if (url.length() > 0 && checkoutDir.length() > 0 && dir.length() > 0)
      {
         String projFile = projFileFromDir(
               FileSystemItem.createDir(dir).completePath(checkoutDir));

         VcsCloneOptions options = VcsCloneOptions.create(event.getVcsId(),
                                                          url,
                                                          username,
                                                          checkoutDir,
                                                          dir);
         NewProjectResult newProject = new NewProjectResult(
               projFile,
               false /*createGitRepo*/,
               false /*usePackrat*/,
               dir,
               options,
               null /*newPackageOptions*/,
               null /*newShinyAppOptions*/,
               null /*projectTemplateOptions*/,
               event.getCallContext());

         createNewProject(newProject, saveChanges);
      }
   }

   @Override
   public void onNewProjectFolderEvent(NewProjectFolderEvent event)
   {
      String name = event.getDirName().trim();
      String dir = event.getDestDir().trim();
      if (name.length() > 0 && dir.length() > 0)
      {
         String projDir = FileSystemItem.createDir(dir).completePath(name);
         String projFile = Projects.projFileFromDir(projDir);
         NewProjectResult newProject = new NewProjectResult(
               projFile,
               event.getCreateRepo(),
               false/*usePackRat*/,
               dir,
               null/*vcsCloneOptions*/,
               null/*newPackageOptions*/,
               null/*newShinyAppOptions*/,
               null/*projectTemplateOptions*/,
               event.getCallContext());

         createNewProject(newProject, false/*saveChanges*/);
      }
   }

   public static String projFileFromDir(String dir)
   {
      FileSystemItem dirItem = FileSystemItem.createDir(dir);
      return FileSystemItem.createFile(
        dirItem.completePath(dirItem.getName() + ".Rproj")).getPath();
   }

   private void notifyTutorialCreateNewResult(NewProjectResult newProject,
                                              boolean success,
                                              String resultMessage)
   {
      if (newProject.getCallContext() == null)
      {
         // not triggered by Tutorial Api, nobody to report to
         return;
      }

      eventBus_.fireEvent(new ApplicationTutorialEvent(
            success ? ApplicationTutorialEvent.API_SUCCESS : ApplicationTutorialEvent.API_ERROR,
            resultMessage,
            newProject.getCallContext()));
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
            UserPrefs userPrefs = pUserPrefs_.get();

            // update default project location pref if necessary
            if ((newProject.getNewDefaultProjectLocation() != null) ||
                (newProject.getCreateGitRepo() !=
                 userPrefs.newProjGitInit().getValue()))
            {
               indicator.onProgress("Saving defaults...");

               if (newProject.getNewDefaultProjectLocation() != null)
               {
                  userPrefs.defaultProjectLocation().setGlobalValue(
                     newProject.getNewDefaultProjectLocation());
               }

               if (newProject.getCreateGitRepo() !=
                   userPrefs.newProjGitInit().getValue())
               {
                  userPrefs.newProjGitInit().setGlobalValue(
                                          newProject.getCreateGitRepo());
               }

               // call the server -- in all cases continue on with
               // creating the project (swallow errors updating the pref)
               projServer_.setUserPrefs(
                     session_.getSessionInfo().getUserPrefs(),
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

               if (cloneOptions.getVcsName() == (VCSConstants.GIT_ID))
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
                                    notifyTutorialCreateNewResult(newProject, false, "vcsClone failed");
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
                           notifyTutorialCreateNewResult(newProject, false, error.getUserMessage());
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
                  notifyTutorialCreateNewResult(newProject, false,
                        "Invalid package name " + packageName);
                  return;
               }
            }

            indicator.onProgress("Creating project...");

            if (newProject.getNewPackageOptions() == null)
            {
               Command onReady = () -> {
                  projServer_.createProject(
                        newProject.getProjectFile(),
                        newProject.getNewPackageOptions(),
                        newProject.getNewShinyAppOptions(),
                        newProject.getProjectTemplateOptions(),
                        new ServerRequestCallback<String>()
                        {
                           @Override
                           public void onResponseReceived(String foundProjectFile)
                           {
                              if (!StringUtil.isNullOrEmpty(foundProjectFile))
                              {
                                 // found an existing project file with different name than the
                                 // parent folder; have to update here so that's what we end up
                                 // opening
                                 newProject.setProjectFile(foundProjectFile);
                              }
                              continuation.execute();
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                              indicator.onError(error.getUserMessage());
                              notifyTutorialCreateNewResult(newProject, false, error.getUserMessage());
                           }
                        });
               };

               if (newProject.getProjectTemplateOptions() != null)
               {
                  // NOTE: We provide built-in project templates for packages that may
                  // not be currently installed; in those cases, verify that the package is
                  // installed and if not attempt installation from CRAN first
                  String pkg = newProject.getProjectTemplateOptions().getDescription().getPackage();
                  ArrayList<Dependency> deps = new ArrayList<Dependency>();
                  deps.add(Dependency.cranPackage(pkg));
                  RStudioGinjector.INSTANCE.getDependencyManager().withDependencies(
                        "Creating project",
                        "Creating a project with " + pkg,
                        pkg + " Project",
                        deps,
                        false,
                        (Boolean success) -> {
                           if (!success)
                           {
                              globalDisplay_.showErrorMessage(
                                    "Error installing " + pkg,
                                    "Installation of package '" + pkg + "' failed, and so the project cannot " +
                                    "be created. Try installing the package manually with " +
                                    "'install.packages(\"" + pkg + "\")'.");
                              return;
                           }

                           onReady.execute();
                        });
               }
               else
               {
                  onReady.execute();
               }
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
                           {
                              indicator.onError(response.errorMessage());
                              notifyTutorialCreateNewResult(newProject, false, "creating project");
                           }
                           else
                           {
                              continuation.execute();
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                           indicator.onError(error.getUserMessage());
                           notifyTutorialCreateNewResult(newProject, false, error.getUserMessage());
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

      if (newProject.getUseRenv())
      {
         createProjectCmds.addCommand((final Command continuation) -> {
            indicator.onProgress("Initializing renv...");

            String projDir = FileSystemItem.createFile(
                  newProject.getProjectFile()
            ).getParentPathString();

            renvServer_.renvInit(projDir, new VoidServerRequestCallback(indicator) {

               @Override
               public void onSuccess()
               {
                  continuation.execute();
               }
            });

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
                  notifyTutorialCreateNewResult(newProject, true, "");
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
            notifyTutorialCreateNewResult(newProject, true, "");

            if (!newProject.getOpenInNewWindow())
            {
               applicationQuit_.performQuit(
                                 null,
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

   @Override
   public void onRequestOpenProjectEvent(RequestOpenProjectEvent event)
   {
      String projFile = event.getProjectFile();
      if (event.isNewSession())
      {
         eventBus_.fireEvent(new OpenProjectNewWindowEvent(projFile, null));
      }
      else
      {
         eventBus_.fireEvent(new SwitchToProjectEvent(projFile));
      }
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
      // less chance of running afoul of desktop app creation
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
      applicationQuit_.prepareForQuit("Close Project", new ApplicationQuit.QuitContext() {
         public void onReadyToQuit(final boolean saveChanges)
         {
            applicationQuit_.performQuit(null, saveChanges, NONE);
         }});
   }

   @Handler
   public void onProjectOptions()
   {
      showProjectOptions(ProjectPreferencesDialog.GENERAL, true);
   }

   @Handler
   public void onProjectSweaveOptions()
   {
      showProjectOptions(ProjectPreferencesDialog.SWEAVE, true);
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
         showProjectOptions(ProjectPreferencesDialog.BUILD, true);
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
         showProjectOptions(ProjectPreferencesDialog.VCS, true);
      }
   }

   @Handler
   public void onPackratBootstrap()
   {
      showProjectOptions(ProjectPreferencesDialog.RENV, true);
   }

   @Handler
   public void onPackratOptions()
   {
      showProjectOptions(ProjectPreferencesDialog.RENV, true);
   }

   public void showProjectOptions(final int initialPane, boolean showPaneChooser)
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
            dlg.setShowPaneChooser(showPaneChooser);
            dlg.showModal();
         }});
   }

   @Override
   public void onOpenProjectFile(final OpenProjectFileEvent event)
   {
      // project options for current project
      FileSystemItem projFile = event.getFile();
      if (projFile.getPath() ==
                  session_.getSessionInfo().getActiveProjectFile())
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
      switchToProject(event.getProject(), event.getForceSaveAll(), event.getCallContext());
   }

   @Override
   public void onOpenProjectError(OpenProjectErrorEvent event)
   {
      // show error dialog
      String msg = "Project '" + event.getProject() + "' " +
                   "could not be opened: " + event.getMessage();

      if (session_.getSessionInfo().getAllowOpenSharedProjects())
      {
         msg += "\n\nEnsure the project URL is correct; if it is, contact the project" +
               " owner to request access.";
      }

      ArrayList<String> buttons = new ArrayList<>();
      buttons.add("OK");
      ArrayList<String> elementIds = new ArrayList<>();
      elementIds.add(ElementIds.DIALOG_OK_BUTTON);
      ArrayList<Operation> ops = new ArrayList<>();
      ops.add(new Operation()
      {
         @Override
         public void execute()
         {
            // close the project by switching to the empty project
            // this puts us in a known good state instead of hanging
            // out in a session we shouldn't be using
            onCloseProject();
         }
      });

      RStudioGinjector.INSTANCE.getGlobalDisplay().showGenericDialog(
            GlobalDisplay.MSG_ERROR,
            "Error Opening Project",
            msg, buttons, elementIds, ops, 0);

      // remove from mru list
      pMRUList_.get().remove(event.getProject());
   }

   private boolean hasActiveProject()
   {
      return session_.getSessionInfo().getActiveProjectFile() != null;
   }

   private void switchToProject(String projectFilePath)
   {
      switchToProject(projectFilePath, false, null);
   }

   private void switchToProject(final String projectFilePath,
                                final boolean forceSaveAll,
                                final TutorialApiCallContext callContext)
   {
      final boolean allowCancel = (callContext == null);

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
                  allowCancel,
                  forceSaveAll,
                  new ApplicationQuit.QuitContext() {
                  public void onReadyToQuit(final boolean saveChanges)
                  {
                     applicationQuit_.performQuit(callContext, saveChanges, projectFilePath);
                  }
               });
            }
            else
            {
               // show error dialog
               showProjectOpenError(projectFilePath);

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
      Consumer<String> onSessionCreated;
      if (Desktop.isRemoteDesktop())
      {
         onSessionCreated = (String url) -> {
            if (onSuccess != null)
               onSuccess.execute();
            Desktop.getFrame().openProjectInNewWindow(url);
         };
      }
      else
      {
         onSessionCreated = (String url) -> {
            if (onSuccess != null)
               onSuccess.execute();
            globalDisplay_.openWindow(url);
         };
      }
      sessionOpener_.navigateToNewSession(
            true, /*isProject*/
            project.getParentPathString(),
            rVersion,
            onSessionCreated);
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
                                      true /*allowCancel*/,
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

                     String projectPath = input.getProjectFile().getPath();

                     // lambda to invoke to actually open the project
                     final Runnable openProject = ()->
                     {
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
                           applicationQuit_.performQuit(null, saveChanges,
                                 input.getProjectFile().getPath());
                        }
                     };

                     // validate that the open will actually work before attempting to open the project
                     projServer_.validateProjectPath(
                                     projectPath,
                                     new SimpleRequestCallback<Boolean>() {

                        @Override
                        public void onResponseReceived(Boolean valid)
                        {
                           if (valid)
                           {
                              // open the project
                              openProject.run();
                           }
                           else
                           {
                              // show error dialog
                              showProjectOpenError(projectPath);
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);

                           // attempt to open project anyway to ensure user does not get stuck
                           openProject.run();
                        }
                     });
                  }
               });
         }
      });
   }

   private void showProjectOpenError(String projectFilePath)
   {
      String msg = "Project '" + projectFilePath + "' " +
            "does not exist (it has been moved or deleted), or it " +
            "is not writeable";
      globalDisplay_.showErrorMessage("Error Opening Project", msg);
   }

   private final Provider<ProjectMRUList> pMRUList_;
   private final ApplicationQuit applicationQuit_;
   private final ProjectsServerOperations projServer_;
   private final RenvServerOperations renvServer_;
   private final GitServerOperations gitServer_;
   private final RemoteFileSystemContext fsContext_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Provider<WorkbenchContext> pWorkbenchContext_;
   private final Provider<ProjectPreferencesDialog> pPrefDialog_;
   private final Provider<UserPrefs> pUserPrefs_;
   private final ProjectOpener opener_;
   private final SessionOpener sessionOpener_;

   public static final String NONE = "none";
   public static final Pattern PACKAGE_NAME_PATTERN =
         Pattern.create("^[a-zA-Z][a-zA-Z0-9.]*$", "");
}
