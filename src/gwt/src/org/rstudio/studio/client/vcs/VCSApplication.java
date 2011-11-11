package org.rstudio.studio.client.vcs;


import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.SatelliteManager;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VCSApplication  
{
   @Inject
   public VCSApplication(VCSApplicationView view,
                         SatelliteManager satelliteManager,
                         ApplicationUncaughtExceptionHandler uncaughtExHandler)
   {
      view_ = view;
      satelliteManager_ = satelliteManager;
      uncaughtExHandler_ = uncaughtExHandler;
   }
   
   public void go(RootLayoutPanel rootPanel)
   {
      // indicate that we are a satellite window
      satelliteManager_.initSatelliteWindow();
      
      // register for uncaught exceptions (do this after calling 
      // initSatelliteWindow b/c it depends on Server)
      uncaughtExHandler_.register();
      
      Widget w = view_.getWidget();
      rootPanel.add(w);
      rootPanel.setWidgetTopBottom(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
      rootPanel.setWidgetLeftRight(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
   }
   
   
   private final VCSApplicationView view_;
   private final SatelliteManager satelliteManager_;
   private final ApplicationUncaughtExceptionHandler uncaughtExHandler_;

}
