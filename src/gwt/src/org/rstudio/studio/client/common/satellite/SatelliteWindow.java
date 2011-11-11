package org.rstudio.studio.client.common.satellite;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SatelliteWindow
{
   @Inject
   public SatelliteWindow()
   {
      
   }
   
   // satellite windows should call this during startup to setup a 
   // communication channel with the main window
   public native void initialize() /*-{
      $wnd.isRStudioSatellite = true;
      $wnd.opener.registerAsRStudioSatellite($wnd);
   }-*/;
   
   
   // check whether the current window is a satellite
   public native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;
   

}
