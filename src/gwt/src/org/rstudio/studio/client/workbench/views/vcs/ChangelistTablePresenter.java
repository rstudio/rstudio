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

import com.google.inject.Inject;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.vcs.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.StageUnstageHandler;

import java.util.ArrayList;

public class ChangelistTablePresenter
{
   @Inject
   public ChangelistTablePresenter(VCSServerOperations server,
                                   ChangelistTable view)
   {
      server_ = server;
      view_ = view;

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
               server_.vcsUnstage(paths,
                                  new SimpleRequestCallback<Void>());
            }
            else
            {
               server_.vcsStage(paths,
                                new SimpleRequestCallback<Void>());
            }
         }
      });
   }

   public ChangelistTable getView()
   {
      return view_;
   }

   private final VCSServerOperations server_;
   private final ChangelistTable view_;
}
