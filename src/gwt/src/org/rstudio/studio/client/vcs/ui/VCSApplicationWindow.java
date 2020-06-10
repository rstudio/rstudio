/*
 * VCSApplicationWindow.java
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
package org.rstudio.studio.client.vcs.ui;


import java.util.ArrayList;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.StyleUtils;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
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
   public VCSApplicationWindow(Provider<ReviewPresenter> pReviewPresenter,
                               Provider<HistoryPresenter> pHistoryPresenter,
                               Provider<Commands> pCommands,
                               Provider<EventBus> pEventBus,
                               Provider<FontSizeManager> pFontSizeManager)
   {
      super(pEventBus, pFontSizeManager);
      pReviewPresenter_ = pReviewPresenter;
      pHistoryPresenter_ = pHistoryPresenter;
      pCommands_ = pCommands;
   }
   
   
   @Override
   protected void onInitialize(LayoutPanel mainPanel, 
                               JavaScriptObject params)
   {
      // set our window title
      Window.setTitle("RStudio: Review Changes");
      
      // always show scrollbars on the mac
      StyleUtils.forceMacScrollbars(mainPanel);
            
      // show the vcs ui in our main panel
      VCSApplicationParams vcsParams = params.<VCSApplicationParams>cast();
      ReviewPresenter rpres = pReviewPresenter_.get();
      ArrayList<StatusAndPath> selected = vcsParams.getSelected();
      if (selected.size() > 0)
         rpres.setSelectedPaths(selected);
      HistoryPresenter hpres = pHistoryPresenter_.get();
      if (vcsParams.getHistoryFileFilter() != null)
         hpres.setFileFilter(vcsParams.getHistoryFileFilter());
           
      vcsPopupController_ = VCSPopup.show(mainPanel,
                                          rpres,
                                          hpres, 
                                          vcsParams.getShowHistory());  
   }
   
   @Override
   public void reactivate(JavaScriptObject params)
   {
      // respect parameters if they were passed
      if (params != null)
      { 
         VCSApplicationParams vcsParams = params.<VCSApplicationParams>cast();
         if (vcsParams.getShowHistory())
         {
            vcsPopupController_.switchToHistory(
                                       vcsParams.getHistoryFileFilter());
         }
         else
         {
            vcsPopupController_.switchToReview(vcsParams.getSelected());
         }
      }
      // for no parameters passed we still want to do a refresh
      else
      {
         pCommands_.get().vcsRefreshNoError().execute();
      }
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }

   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
   private final Provider<Commands> pCommands_;
   private VCSPopup.Controller vcsPopupController_ = null;
 
}
