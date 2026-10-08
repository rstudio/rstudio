/*
 * WorktreeActions.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
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
                          Satellite satellite)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      projectsServer_ = projectsServer;
      gitServer_ = gitServer;
      gitState_ = gitState;
      session_ = session;
      satellite_ = satellite;

      if (!Satellite.isCurrentWindowSatellite())
         exportOpenProjectCallback();
   }

   // The directory new worktrees are created under: the one holding this
   // repository's linked worktrees (the most common one, should they be
   // spread out), else alongside the main worktree, else alongside the
   // project, else home (a checkout at "~" or directly under "/" has no
   // usable parent). The worktrees themselves are the record of where this
   // repository keeps them; a remembered directory would carry over from
   // whichever repository was used last.
   public String defaultParentDir()
   {
      String parentDir = null;
      String mainParentDir = null;
      int best = 0;
      Map<String, Integer> counts = new HashMap<>();
      for (WorktreeInfo worktree : worktrees())
      {
         String candidate = FileSystemItem.createDir(worktree.getPath()).getParentPathString();
         if (worktree.isMain())
         {
            mainParentDir = candidate;
            continue;
         }

         int count = counts.containsKey(candidate) ? counts.get(candidate) + 1 : 1;
         counts.put(candidate, count);
         if (count > best)
         {
            best = count;
            parentDir = candidate;
         }
      }

      if (StringUtil.isNullOrEmpty(parentDir))
         parentDir = mainParentDir;

      if (StringUtil.isNullOrEmpty(parentDir))
         parentDir = session_.getSessionInfo().getActiveProjectDir().getParentPathString();

      if (StringUtil.isNullOrEmpty(parentDir))
         parentDir = FileSystemItem.HOME_PATH;

      return parentDir;
   }

   // Worktrees that can be removed: not the main one, and not the one we're in.
   // A locked one is offered too; removing it takes the force option.
   public List<WorktreeInfo> removableWorktrees()
   {
      List<WorktreeInfo> worktrees = new ArrayList<>();
      for (WorktreeInfo worktree : worktrees())
      {
         if (!worktree.isMain() && !worktree.isCurrent() && !worktree.isBare())
            worktrees.add(worktree);
      }
      return worktrees;
   }

   // Runs 'git worktree add' for `path`, then offers to open the result. The
   // branch is created when `createBranch` is set -- from `startPoint` (a
   // remote branch, which it then tracks) when given, else from HEAD -- and
   // checked out as it is otherwise. `onCreated` (optional) runs first, once
   // the worktree exists, and is handed that offer to make when its own work
   // is done.
   public void create(final String path,
                      final String branch,
                      boolean createBranch,
                      String startPoint,
                      final CommandWithArg<Command> onCreated)
   {
      addWorktree(path, branch, createBranch, StringUtil.notNull(startPoint), onCreated);
   }

   // As above, settling for itself how the branch comes about: an existing
   // local branch is checked out, a remote-only one is tracked (from origin
   // when more than one remote has it -- git's own guess needs exactly one),
   // and otherwise the branch is new.
   public void create(final String path,
                      final String branch,
                      final CommandWithArg<Command> onCreated)
   {
      gitServer_.gitListBranches(new ServerRequestCallback<BranchesInfo>()
      {
         @Override
         public void onResponseReceived(BranchesInfo branchesInfo)
         {
            if (hasLocalBranch(branch, branchesInfo))
            {
               create(path, branch, false, null, onCreated);
               return;
            }

            List<String> remotes = remotesWithBranch(branch, branchesInfo);
            if (remotes.isEmpty())
            {
               create(path, branch, true, null, onCreated);
               return;
            }

            String remote = remotes.contains("origin") ? "origin" : remotes.get(0);
            create(path, branch, true, remote + "/" + branch, onCreated);
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

   private static boolean hasLocalBranch(String branch, BranchesInfo branchesInfo)
   {
      for (String candidate : JsUtil.asIterable(branchesInfo.getBranches()))
      {
         if (StringUtil.equals(candidate, branch))
            return true;
      }
      return false;
   }

   // The remotes with a branch of this name ("remotes/<remote>/<branch>")
   private static List<String> remotesWithBranch(String branch, BranchesInfo branchesInfo)
   {
      List<String> remotes = new ArrayList<>();
      for (String candidate : JsUtil.asIterable(branchesInfo.getBranches()))
      {
         if (!candidate.startsWith("remotes/"))
            continue;

         int slash = candidate.indexOf('/', "remotes/".length());
         if (slash == -1)
            continue;

         if (StringUtil.equals(candidate.substring(slash + 1), branch))
            remotes.add(candidate.substring("remotes/".length(), slash));
      }
      return remotes;
   }

   private void addWorktree(final String path,
                            String branch,
                            boolean createBranch,
                            String startPoint,
                            final CommandWithArg<Command> onCreated)
   {
      // the entry git adds is told apart from the ones known now
      final Set<String> existing = new HashSet<>();
      for (WorktreeInfo worktree : worktrees())
         existing.add(worktree.getPath());

      gitServer_.gitAddWorktree(
            path,
            branch,
            createBranch,
            startPoint,
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

                        Command offerToOpen = () -> onWorktreeAdded(path, existing);
                        if (onCreated != null)
                           onCreated.execute(offerToOpen);
                        else
                           offerToOpen.execute();
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

   private void onWorktreeAdded(final String path, final Set<String> existing)
   {
      // refresh so the branch menu picks up the new worktree, then offer to
      // open it; the opener needs the refreshed entry for its project file.
      // Should the refresh fail, the error is all the user gets to see of
      // why no offer follows, so it is shown.
      gitState_.refresh(true, new Command()
      {
         @Override
         public void execute()
         {
            final WorktreeInfo worktree = findAddedWorktree(path, existing);
            if (worktree == null)
               return;

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

   // The refreshed entry for the worktree just created at `path`. Git reports
   // the resolved path, which can differ from the one the user typed ("~",
   // symlinks), so when no entry matches the path itself, take the one entry
   // that wasn't there before -- or, should several have appeared at once,
   // the one sharing its directory name, provided that is unambiguous.
   private WorktreeInfo findAddedWorktree(String path, Set<String> existing)
   {
      List<WorktreeInfo> added = new ArrayList<>();
      for (WorktreeInfo worktree : worktrees())
      {
         if (StringUtil.equals(worktree.getPath(), path))
            return worktree;
         if (!existing.contains(worktree.getPath()))
            added.add(worktree);
      }

      if (added.size() == 1)
         return added.get(0);

      String name = FileSystemItem.createDir(path).getName();
      WorktreeInfo sameName = null;
      for (WorktreeInfo worktree : added)
      {
         if (!StringUtil.equals(FileSystemItem.createDir(worktree.getPath()).getName(), name))
            continue;
         if (sameName != null)
            return null;
         sameName = worktree;
      }
      return sameName;
   }

   // Confirms, then runs 'git worktree remove' (forced when asked, as git
   // otherwise refuses to delete uncommitted changes or a locked worktree)
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
      // git keeps the branch of a worktree whose directory is gone; there is
      // nothing to open until the stale entry has been removed
      if (worktree.isPrunable())
      {
         globalDisplay_.showMessage(
               MessageDialog.WARNING,
               constants_.openWorktreeCaption(),
               constants_.worktreeDirectoryMissing(worktree.getDisplayName(), worktree.getPath()));
         return;
      }

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

   // The project events are handled in the main window only, so a satellite
   // (Review Changes has the branch menu too) hands the request over to it
   private void openProject(String projectFile, boolean newSession)
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         // the main window is reached through the opener; without one (or
         // before it has created this class) there is nothing to hand over to
         if (!callMainWindowOpenProject(projectFile, newSession))
         {
            Debug.log("Unable to open worktree from satellite: main window unavailable");
            globalDisplay_.showErrorMessage(constants_.openWorktreeCaption(),
                                            constants_.openWorktreeMainWindowUnavailable());
            return;
         }

         satellite_.focusMainWindow();
         return;
      }

      if (newSession && canOpenInNewSession())
         events_.fireEvent(new OpenProjectNewWindowEvent(projectFile, null));
      else
         events_.fireEvent(new SwitchToProjectEvent(projectFile));
   }

   private final native void exportOpenProjectCallback() /*-{
      var self = this;
      $wnd.worktreeOpenProjectFromRStudioSatellite = $entry(
         function(projectFile, newSession) {
            self.@org.rstudio.studio.client.workbench.views.vcs.WorktreeActions::openProject(Ljava/lang/String;Z)(projectFile, newSession);
         }
      );
   }-*/;

   private final native boolean callMainWindowOpenProject(String projectFile, boolean newSession) /*-{
      var opener = $wnd.opener;
      if (!opener || !opener.worktreeOpenProjectFromRStudioSatellite)
         return false;

      opener.worktreeOpenProjectFromRStudioSatellite(projectFile, newSession);
      return true;
   }-*/;

   // The known worktrees; empty until the first status refresh has completed
   private Iterable<WorktreeInfo> worktrees()
   {
      BranchesInfo branchInfo = gitState_.getBranchInfo();
      if (branchInfo == null)
         return new ArrayList<>();

      return JsUtil.asIterable(branchInfo.getWorktrees());
   }

   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final ProjectsServerOperations projectsServer_;
   private final GitServerOperations gitServer_;
   private final GitState gitState_;
   private final Session session_;
   private final Satellite satellite_;

   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
