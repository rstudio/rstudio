/*
 * GitChangelistTablePresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.inject.Inject;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.RemoteBranchInfo;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import java.util.ArrayList;

public class GitChangelistTablePresenter
{
   @Inject
   public GitChangelistTablePresenter(GitServerOperations server,
                                      GitChangelistTable view,
                                      GitState gitState,
                                      UserPrefs prefs)
   {
      server_ = server;
      view_ = view;
      gitState_ = gitState;
      prefs_ = prefs;

      view_.addStageUnstageHandler(new StageUnstageHandler()
      {
         @Override
         public void onStageUnstage(StageUnstageEvent event)
         {
            ArrayList<String> paths = new ArrayList<String>();
            for (StatusAndPath path : event.getPaths())
               paths.add(path.getPath());

            if (event.isUnstage())
            {
               server_.gitUnstage(paths,
                                  new SimpleRequestCallback<Void>());
            }
            else
            {
               server_.gitStage(paths,
                                new SimpleRequestCallback<Void>());
            }
         }
      });

      gitState_.bindRefreshHandler(view_, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(gitState_.getStatus());
            
            RemoteBranchInfo remote = gitState_.getRemoteBranchInfo();
            if (remote != null && remote.getCommitsBehind() > 0)
            {
               String message = 
                  "Your branch is ahead of '" + remote.getName() + "' by " +
                  remote.getCommitsBehind() + " commit" +
                  (remote.getCommitsBehind() > 1 ? "s" : "") + ".";
               
               view_.showInfoBar(message, !prefs.reducedMotion().getValue());
            }
            else
            {
               view_.hideInfoBar(!prefs_.reducedMotion().getValue());
            } 
         }
      });
   }

   public void setSelectFirstItemByDefault(boolean selectFirstItemByDefault)
   {
      view_.setSelectFirstItemByDefault(selectFirstItemByDefault);
   }

   public GitChangelistTable getView()
   {
      return view_;
   }

   private final GitServerOperations server_;
   private final GitChangelistTable view_;
   private final GitState gitState_;
   private final UserPrefs prefs_;
}
