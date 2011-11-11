package org.rstudio.studio.client.common;

import java.util.ArrayList;

import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SatelliteManager implements CloseHandler<Window>
{
   @Inject
   public SatelliteManager()
   {
      
   }
   
   public void initMainWindow()
   {
      // export the registration hook used by satellites
      exportSatelliteRegistrationCallback();
      
      // handle onClosed to automatically close all satellites
      Window.addCloseHandler(this);
      
   }
   
   
   
   public native void initSatelliteWindow() /*-{
      $wnd.isRStudioSatellite = true;
      $wnd.opener.registerAsRStudioSatellite($wnd);
   }-*/;
   
   
   public native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;

   // close all satellites when we are closed
   @Override
   public void onClose(CloseEvent<Window> event)
   {
      for (int i=0; i<satellites_.size(); i++)
      {
         try
         {
            WindowEx satelliteWnd = satellites_.get(i);
            satelliteWnd.close();
         }
         catch(Throwable e)
         {
         }
      } 
   }
   
   
   
   private void registerAsSatellite(JavaScriptObject wnd)
   {
      WindowEx satelliteWnd = wnd.<WindowEx>cast();
      satellites_.add(satelliteWnd);
   }
   
   private native void exportSatelliteRegistrationCallback() /*-{
      var manager = this;     
      $wnd.registerAsRStudioSatellite = $entry(
         function(satelliteWnd) {
            manager.@org.rstudio.studio.client.common.SatelliteManager::registerAsSatellite(Lcom/google/gwt/core/client/JavaScriptObject;)(satelliteWnd);
         }
      ); 
   }-*/;
   
   private final ArrayList<WindowEx> satellites_ = new ArrayList<WindowEx>();

   
}


