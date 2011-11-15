package org.rstudio.studio.client.vcs.ui;


import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.vcs.VCSCore;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;

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
   public VCSApplicationWindow(Provider<VCSCore> pVCSCore,
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
   }
   
  
   @Override 
   public Widget getWidget()
   {
      return this;
   }


   private final Provider<VCSCore> pVCSCore_;
   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
}
