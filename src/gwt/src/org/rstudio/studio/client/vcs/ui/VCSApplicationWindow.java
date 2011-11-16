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


import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.common.satellite.SatelliteWindowPrefs;
import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.vcs.GitPresenterCore;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
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
                               Provider<FontSizeManager> pFontSizeManager,
                               Provider<UIPrefs> pUIPrefs)
   {
      super(pEventBus, pFontSizeManager);
      pVCSCore_ = pVCSCore;
      pReviewPresenter_ = pReviewPresenter;
      pHistoryPresenter_ = pHistoryPresenter;
      pUIPrefs_ = pUIPrefs;
   }
   
   
   @Override
   protected void onInitialize(LayoutPanel mainPanel)
   {
      // set our window title
      Window.setTitle("Review Changes");
      
      // make sure vcs core is initialized
      pVCSCore_.get();
      
      // show the vcs ui in our main panel
      VCSPopup.show(mainPanel,
                    pReviewPresenter_.get(),
                    pHistoryPresenter_.get(), 
                    false);  
      
      // create a time-buffered command for updating our prefs. run when
      // nudged but in no case run more than once every 3 seconds
      final TimeBufferedCommand updatePrefsCommand = 
                                    new TimeBufferedCommand(-1, -1, 3000)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            pUIPrefs_.get().writeUIPrefs();
         }
      };
      
      // if the window is resized then set & write the prefs
      Window.addResizeHandler(new ResizeHandler() {
         @Override
         public void onResize(ResizeEvent event)
         {
            pUIPrefs_.get().vcsWindowPrefs().setGlobalValue(
                  SatelliteWindowPrefs.create(event.getWidth(),
                                              event.getHeight()));
            
            updatePrefsCommand.nudge();
         }
         
      });
   }
   
  
   @Override 
   public Widget getWidget()
   {
      return this;
   }


   private final Provider<GitPresenterCore> pVCSCore_;
   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
   private final Provider<UIPrefs> pUIPrefs_;
}
