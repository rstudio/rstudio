/*
 * SVNChangelistTablePresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.inject.Inject;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNChangelistTablePresenter
{
   @Inject
   public SVNChangelistTablePresenter(final SVNChangelistTable view,
                                      final SVNState svnState)
   {
      view_ = view;

      svnState.bindRefreshHandler(view, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view.setItems(svnState.getStatus());
         }
      });

   }

   public SVNChangelistTable getView()
   {
      return view_;
   }

   public ArrayList<StatusAndPath> getSelectedItems()
   {
      return view_.getSelectedItems();
   }

   private final SVNChangelistTable view_;
}
