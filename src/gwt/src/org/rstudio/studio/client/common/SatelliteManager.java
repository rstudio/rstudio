package org.rstudio.studio.client.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SatelliteManager
{
   @Inject
   public SatelliteManager()
   {
      
   }
   
   public void registerAsMainWindow()
   {
      
   }
   
   public native void registerAsSatelliteWindow() /*-{
      $wnd.isRStudioSatellite = true;
   }-*/;
   
  
   
   
   public native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;
   
}
