/*
 * VCSApplicationWindow.java
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
package org.rstudio.studio.client.vcs.ui;


import java.util.ArrayList;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.vcs.GitPresenterCore;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


@Singleton
public class VCSApplicationWindow extends SatelliteWindow
                                  implements VCSApplicationView
{
   @Inject
   public VCSApplicationWindow(Provider<GitPresenterCore> pVCSCore,
                               Provider<ReviewPresenter> pReviewPresenter,
                               Provider<HistoryPresenter> pHistoryPresenter,
                               Provider<EventBus> pEventBus,
                               Provider<FontSizeManager> pFontSizeManager)
   {
      super(pEventBus, pFontSizeManager);
      pVCSCore_ = pVCSCore;
      pReviewPresenter_ = pReviewPresenter;
      pHistoryPresenter_ = pHistoryPresenter;
   }
   
   
   @Override
   protected void onInitialize(LayoutPanel mainPanel, 
                               JavaScriptObject params)
   {
      // set our window title
      Window.setTitle("Review Changes");
      
      // make sure vcs core is initialized
      pVCSCore_.get();
      
      // show the vcs ui in our main panel
      VCSApplicationParams vcsParams = params.<VCSApplicationParams>cast();
      ReviewPresenter rpres = pReviewPresenter_.get();
      ArrayList<StatusAndPath> selected = vcsParams.getSelected();
      if (selected.size() > 0)
         rpres.setSelectedPaths(selected);
      vcsPopupController_ = VCSPopup.show(mainPanel,
                                          rpres,
                                          pHistoryPresenter_.get(), 
                                          vcsParams.getShowHistory());  
   }
   
   @Override
   public void reactivate(JavaScriptObject params)
   {
      VCSApplicationParams vcsParams = params.<VCSApplicationParams>cast();
      if (vcsParams.getShowHistory())
      {
         vcsPopupController_.switchToHistory();
      }
      else
      {
         vcsPopupController_.switchToReview(vcsParams.getSelected());
      }
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }


   private final Provider<GitPresenterCore> pVCSCore_;
   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
   private VCSPopup.Controller vcsPopupController_ = null;
 
}
