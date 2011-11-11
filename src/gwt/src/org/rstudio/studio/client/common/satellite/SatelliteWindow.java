package org.rstudio.studio.client.common.satellite;

import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SatelliteWindow
{
   // TODO: call session.setSessionInfo and pUIPrefs.get on callback
   
   @Inject
   public SatelliteWindow(Session session,
                          Provider<UIPrefs> pUIPrefs)
   {
      session_ = session;
      pUIPrefs_ = pUIPrefs;
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
   

   private final Session session_;
   private final Provider<UIPrefs> pUIPrefs_;
}
