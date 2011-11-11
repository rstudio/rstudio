package org.rstudio.studio.client.vcs.ui;


import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;



@Singleton
public class VCSApplicationWindow extends Composite
                                  implements VCSApplicationView,
                                             RequiresResize,
                                             ProvidesResize
{
   @Inject
   public VCSApplicationWindow(Provider<ReviewPresenter> pReviewPresenter,
                               Provider<HistoryPresenter> pHistoryPresenter)
   {
      // save references
      pReviewPresenter_ = pReviewPresenter;
      pHistoryPresenter_ = pHistoryPresenter;
      
      // occupy full client area of the window
      Window.enableScrolling(false);
      Window.setMargin("0px");

      // create application panel
      applicationPanel_ = new LayoutPanel();
      
    
      
      // init widget
      initWidget(applicationPanel_);
   }
   
   
   @Override
   public void show()
   {
      VCSPopup.show(pReviewPresenter_.get(),
                    pHistoryPresenter_.get(), 
                    false);
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }

   @Override
   public void onResize()
   {
      applicationPanel_.onResize();
      
   }

   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
   private LayoutPanel applicationPanel_;
}
