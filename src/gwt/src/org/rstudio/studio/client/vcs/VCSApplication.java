package org.rstudio.studio.client.vcs;


import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.satellite.Satellite;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VCSApplication  
{
   @Inject
   public VCSApplication(VCSApplicationView view,
                         Satellite satellite,
                         ApplicationUncaughtExceptionHandler uncaughtExHandler)
   {
      view_ = view;
      satellite_ = satellite;
      uncaughtExHandler_ = uncaughtExHandler;
   }
   
   public void go(RootLayoutPanel rootPanel, 
                  final Command dismissLoadingProgress)
   {
      // indicate that we are a satellite window
      satellite_.initialize();
      
      // register for uncaught exceptions (do this after calling 
      // initSatelliteWindow b/c it depends on Server)
      uncaughtExHandler_.register();
      
      // create the widget
      Widget w = view_.getWidget();
      rootPanel.add(w);
      rootPanel.setWidgetTopBottom(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
      rootPanel.setWidgetLeftRight(w, 0, Style.Unit.PX, 0, Style.Unit.PX);
      
      // show the view
      view_.show();
      
      // dismiss loading progress
      dismissLoadingProgress.execute();
   }
   
   
   private final VCSApplicationView view_;
   private final Satellite satellite_;
   private final ApplicationUncaughtExceptionHandler uncaughtExHandler_;

}
