/*
 * GitChangelistTablePresenter.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.RemoteBranchInfo;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
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

      view_.addStageUnstageHandler(new StageUnstageEvent.Handler()
      {
         @Override
         public void onStageUnstage(StageUnstageEvent event)
         {
            ArrayList<String> paths = new ArrayList<>();
            for (StatusAndPath path : event.getPaths())
               paths.add(path.getPath());

            if (event.isUnstage())
            {
               server_.gitUnstage(paths,
                                  new SimpleRequestCallback<>());
            }
            else
            {
               server_.gitStage(paths,
                                new SimpleRequestCallback<>());
            }
         }
      });

      gitState_.bindRefreshHandler(view_, new VcsRefreshEvent.Handler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(gitState_.getStatus());

            RemoteBranchInfo remote = gitState_.getRemoteBranchInfo();
            if (remote != null && remote.getCommitsBehind() > 0)
            {
               String message =
                  remote.getCommitsBehind() > 1 ? constants_.branchAheadOfRemotePlural(remote.getName(),
                          remote.getCommitsBehind()) : constants_.branchAheadOfRemoteSingular(remote.getName(),
                          remote.getCommitsBehind());

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
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
