/*
 * ChangelistTablePresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.VcsState;

import java.util.ArrayList;

public class ChangelistTablePresenter
{
   @Inject
   public ChangelistTablePresenter(GitServerOperations server,
                                   ChangelistTable view,
                                   VcsState vcsState)
   {
      server_ = server;
      view_ = view;
      vcsState_ = vcsState;

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

      vcsState_.bindRefreshHandler(view_, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(vcsState_.getStatus());
         }
      });
   }

   public void setSelectFirstItemByDefault(boolean selectFirstItemByDefault)
   {
      view_.setSelectFirstItemByDefault(selectFirstItemByDefault);
   }

   public ChangelistTable getView()
   {
      return view_;
   }

   private final GitServerOperations server_;
   private final ChangelistTable view_;
   private final VcsState vcsState_;
}
