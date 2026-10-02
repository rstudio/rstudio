/*
 * WorktreeActions.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.WorktreeInfo;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;

// Git worktree operations shared by the Git pane: opening a worktree as an
// RStudio project (in this window or a new session), creating one, and
// removing one. A worktree is just another directory, so "activating" it means
// opening the .Rproj it contains (creating one first if the repository has
// none).
@Singleton
public class WorktreeActions
{
   @Inject
   public WorktreeActions(EventBus events,
                          GlobalDisplay globalDisplay,
                          ProjectsServerOperations projectsServer,
                          GitServerOperations gitServer,
                          GitState gitState,
                          Session session,
                          Provider<UserState> pUserState)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      projectsServer_ = projectsServer;
      gitServer_ = gitServer;
      gitState_ = gitState;
      session_ = session;
      pUserState_ = pUserState;
   }

   // The directory new worktrees are created under: the last one used, else
   // alongside the main worktree, else alongside the project.
   public String defaultParentDir()
   {
      String parentDir = pUserState_.get().gitWorktreeParentDir().getGlobalValue();
      if (!StringUtil.isNullOrEmpty(parentDir))
         return parentDir;

      for (WorktreeInfo worktree : JsUtil.asIterable(gitState_.getBranchInfo().getWorktrees()))
      {
         if (worktree.isMain())
            parentDir = FileSystemItem.createDir(worktree.getPath()).getParentPathString();
      }

      if (StringUtil.isNullOrEmpty(parentDir))
         parentDir = session_.getSessionInfo().getActiveProjectDir().getParentPathString();

      return parentDir;
   }

   // Worktrees that can be removed: not the main one, and not the one we're in
   public List<WorktreeInfo> removableWorktrees()
   {
      List<WorktreeInfo> worktrees = new ArrayList<>();
      for (WorktreeInfo worktree : JsUtil.asIterable(gitState_.getBranchInfo().getWorktrees()))
      {
         if (!worktree.isMain() && !worktree.isCurrent() && !worktree.isBare())
            worktrees.add(worktree);
      }
      return worktrees;
   }

   // Runs 'git worktree add' for `path`, then offers to open the result.
   // `onCreated` (optional) runs first, once the worktree exists.
   public void create(final String path,
                      String parentDir,
                      String branch,
                      boolean createBranch,
                      final Command onCreated)
   {
      UserState userState = pUserState_.get();
      if (!StringUtil.equals(userState.gitWorktreeParentDir().getGlobalValue(), parentDir))
      {
         userState.gitWorktreeParentDir().setGlobalValue(parentDir);
         userState.writeState();
      }

      gitServer_.gitAddWorktree(
            path,
            branch,
            createBranch,
            new ServerRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess process)
               {
                  final ConsoleProgressDialog dialog = new ConsoleProgressDialog(process, gitServer_);
                  dialog.showModal();
                  process.addProcessExitHandler(new ProcessExitEvent.Handler()
                  {
                     @Override
                     public void onProcessExit(ProcessExitEvent event)
                     {
                        // leave the output up on failure so the error can be read
                        if (event.getExitCode() != 0)
                           return;

                        dialog.closeDialog();
                        if (onCreated != null)
                           onCreated.execute();
                        onWorktreeAdded(path);
                     }
                  });
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage(constants_.newWorktreeCapitalized(),
                                                  error.getUserMessage());
               }
            });
   }

   private void onWorktreeAdded(final String path)
   {
      // refresh so the branch menu picks up the new worktree, then offer to
      // open it; the opener needs the refreshed entry for its project file.
      // Git reports the resolved path, which can differ from the one the user
      // typed (symlinks), so match on the directory name as well.
      final String name = FileSystemItem.createDir(path).getName();
      gitState_.refresh(false, new Command()
      {
         @Override
         public void execute()
         {
            WorktreeInfo added = null;
            for (WorktreeInfo worktree : JsUtil.asIterable(gitState_.getBranchInfo().getWorktrees()))
            {
               if (StringUtil.equals(worktree.getPath(), path) ||
                   StringUtil.equals(FileSystemItem.createDir(worktree.getPath()).getName(), name))
               {
                  added = worktree;
               }
            }

            if (added == null)
               return;

            final WorktreeInfo worktree = added;
            globalDisplay_.showYesNoMessage(
                  MessageDialog.QUESTION,
                  constants_.newWorktreeCapitalized(),
                  constants_.openNewWorktree(worktree.getPath()),
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        open(worktree, false);
                     }
                  },
                  true);
         }
      });
   }

   // Confirms, then runs 'git worktree remove' (with --force when asked, as
   // git otherwise refuses to delete uncommitted changes)
   public void remove(final WorktreeInfo worktree, final boolean force)
   {
      globalDisplay_.showYesNoMessage(
            MessageDialog.WARNING,
            constants_.removeWorktreeCapitalized(),
            constants_.removeWorktreeConfirm(worktree.getPath()),
            new Operation()
            {
               @Override
               public void execute()
               {
                  doRemove(worktree, force);
               }
            },
            false);
   }

   private void doRemove(WorktreeInfo worktree, boolean force)
   {
      gitServer_.gitRemoveWorktree(
            worktree.getPath(),
            force,
            new ServerRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess process)
               {
                  final ConsoleProgressDialog dialog = new ConsoleProgressDialog(process, gitServer_);
                  dialog.showModal();
                  process.addProcessExitHandler(new ProcessExitEvent.Handler()
                  {
                     @Override
                     public void onProcessExit(ProcessExitEvent event)
                     {
                        if (event.getExitCode() == 0)
                           dialog.closeDialog();
                        gitState_.refresh(false);
                     }
                  });
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage(constants_.removeWorktreeCapitalized(),
                                                  error.getUserMessage());
               }
            });
   }

   public boolean canOpenInNewSession()
   {
      return Desktop.hasDesktopFrame() || session_.getSessionInfo().getMultiSession();
   }

   public void open(final WorktreeInfo worktree, final boolean newSession)
   {
      if (!StringUtil.isNullOrEmpty(worktree.getProjectFile()))
      {
         openProject(worktree.getProjectFile(), newSession);
         return;
      }

      globalDisplay_.showYesNoMessage(
            MessageDialog.QUESTION,
            constants_.openWorktreeCaption(),
            constants_.worktreeHasNoProjectFile(worktree.getPath()),
            new Operation()
            {
               @Override
               public void execute()
               {
                  createProjectFile(worktree.getProjectDir(), newSession);
               }
            },
            true);
   }

   private void createProjectFile(String projectDir, final boolean newSession)
   {
      projectsServer_.createProjectFile(
            projectDir,
            new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String projectFile)
               {
                  openProject(projectFile, newSession);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage(
                        constants_.openWorktreeCaption(),
                        error.getUserMessage());
               }
            });
   }

   private void openProject(String projectFile, boolean newSession)
   {
      if (newSession && canOpenInNewSession())
         events_.fireEvent(new OpenProjectNewWindowEvent(projectFile, null));
      else
         events_.fireEvent(new SwitchToProjectEvent(projectFile));
   }

   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final ProjectsServerOperations projectsServer_;
   private final GitServerOperations gitServer_;
   private final GitState gitState_;
   private final Session session_;
   private final Provider<UserState> pUserState_;

   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
