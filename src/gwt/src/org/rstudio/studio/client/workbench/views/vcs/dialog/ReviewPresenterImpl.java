/*
 * ReviewPresenterImpl.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.common.events.SwitchViewEvent.Handler;
import org.rstudio.studio.client.workbench.views.vcs.git.dialog.GitReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.dialog.SVNReviewPresenter;

import java.util.ArrayList;

public class ReviewPresenterImpl implements ReviewPresenter
{
   @Inject
   public ReviewPresenterImpl(Provider<GitReviewPresenter> pGitReviewPresenter,
                              Provider<SVNReviewPresenter> pSvnReviewPresenter,
                              Session session)
   {
      String vcsName = session.getSessionInfo().getVcsName();

      if (vcsName.equalsIgnoreCase(VCSConstants.GIT_ID))
         pres_ = pGitReviewPresenter.get();
      else if (vcsName.equalsIgnoreCase(VCSConstants.SVN_ID))
         pres_ = pSvnReviewPresenter.get();
      else
         throw new IllegalStateException("Unknown vcs name: " + vcsName);
   }

   @Override
   public void setSelectedPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      pres_.setSelectedPaths(selectedPaths);
   }

   @Override
   public void onShow()
   {
      pres_.onShow();
   }

   @Override
   public HandlerRegistration addSwitchViewHandler(Handler handler)
   {
      return pres_.addSwitchViewHandler(handler);
   }

   @Override
   public Widget asWidget()
   {
      return pres_.asWidget();
   }

   private ReviewPresenter pres_;
}
