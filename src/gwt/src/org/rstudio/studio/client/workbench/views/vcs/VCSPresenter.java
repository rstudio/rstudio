/*
 * VCSPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsDiffEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRevertFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsViewOnGitHubEvent;

public class VCSPresenter implements IsWidget, 
                                     ShowVcsHistoryEvent.Handler,
                                     ShowVcsDiffEvent.Handler,
                                     VcsRevertFileEvent.Handler,
                                     VcsViewOnGitHubEvent.Handler
{
   @Inject
   public VCSPresenter(Session session,
                       Provider<GitPresenter> pGitPresenter,
                       Provider<SVNPresenter> pSVNPresenter)
   {
      session_ = session;
      pGitPresenter_ = pGitPresenter;
      pSVNPresenter_ = pSVNPresenter;
   }

   @Override
   public Widget asWidget()
   {
      ensurePresenterCreated();

      return presenter_.asWidget();
   }

   private void ensurePresenterCreated()
   {
      if (presenter_ == null)
      {
         String vcsName = session_.getSessionInfo().getVcsName();
         if (VCSConstants.GIT_ID.equalsIgnoreCase(vcsName))
            presenter_ = pGitPresenter_.get();
         else if (VCSConstants.SVN_ID.equalsIgnoreCase(vcsName))
            presenter_ = pSVNPresenter_.get();
      }
   }

   public void onBeforeUnselected()
   {
      presenter_.onBeforeUnselected();
   }

   public void onBeforeSelected()
   {
      presenter_.onBeforeSelected();
   }

   public void onSelected()
   {
      presenter_.onSelected();
   }

   public void setFocus()
   {
      presenter_.setFocus();
   }

   void onVcsCommit()
   {
      presenter_.onVcsCommit();
   }

   void onVcsShowHistory()
   {
      presenter_.onVcsShowHistory();
   }

   void onVcsPull()
   {
      presenter_.onVcsPull();
   }
   
   void onVcsPullRebase()
   {
      presenter_.onVcsPullRebase();
   }
  
   void onVcsPush()
   {
      presenter_.onVcsPush();
   }
   
   void onVcsCleanup()
   {
      presenter_.onVcsCleanup();
   }
   
   void onVcsIgnore()
   {
      presenter_.onVcsIgnore();
   }
   
   @Override
   public void onShowVcsHistory(ShowVcsHistoryEvent event)
   {
      presenter_.showHistory(event.getFileFilter());
   }

   @Override
   public void onShowVcsDiff(ShowVcsDiffEvent event)
   {
      presenter_.showDiff(event.getFile());
   }

   @Override
   public void onVcsRevertFile(VcsRevertFileEvent event)
   {
      presenter_.revertFile(event.getFile());
   }
   
   @Override
   public void onVcsViewOnGitHub(VcsViewOnGitHubEvent event)
   {
      presenter_.viewOnGitHub(event.getViewRequest());
   }
   
   private final Session session_;
   private final Provider<GitPresenter> pGitPresenter_;
   private final Provider<SVNPresenter> pSVNPresenter_;
   private BaseVcsPresenter presenter_;
   
}
