/*
 * WorktreeOpener.java
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

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
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

// Opens a git worktree as an RStudio project, either in this window or in a
// new session. A worktree is just another directory, so "activating" it means
// opening the .Rproj it contains (creating one first if the repository has
// none).
@Singleton
public class WorktreeOpener
{
   @Inject
   public WorktreeOpener(EventBus events,
                         GlobalDisplay globalDisplay,
                         ProjectsServerOperations projectsServer,
                         Session session)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      projectsServer_ = projectsServer;
      session_ = session;
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
                  createProjectFile(worktree.getPath(), newSession);
               }
            },
            true);
   }

   private void createProjectFile(String worktreePath, final boolean newSession)
   {
      projectsServer_.createProjectFile(
            worktreePath,
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
   private final Session session_;

   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
