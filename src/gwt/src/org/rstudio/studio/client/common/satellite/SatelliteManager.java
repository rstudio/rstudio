package org.rstudio.studio.client.common.satellite;

import java.util.ArrayList;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO: Implement for Desktop

// TODO: make sure gwt url is passed to the popup window

// TODO: re-activation of existing satellites (launch manager)


@Singleton
public class SatelliteManager implements CloseHandler<Window>
{
   @Inject
   public SatelliteManager(Session session)
   {
      session_ = session;
   }
   
   // the main window should call this method during startup to set itself
   // up to manage and communicate with the satellite windows
   public void initialize()
   {
      // export the registration hook used by satellites
      exportSatelliteRegistrationCallback();
      
      // handle onClosed to automatically close all satellites
      Window.addCloseHandler(this);
   }
    
   
   // close all satellite windows
   public void closeAllSatellites()
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
      satellites_.clear();  
   }
   
   // dispatch an event to all satellites
   public void dispatchEvent(JavaScriptObject clientEvent)
   {
      // list of windows to remove (because they were closed)
      ArrayList<WindowEx> removeWindows = null;
        
      // iterate over the satellites (make a copy to avoid races if
      // for some reason firing an event creates or destroys a satellite)
      @SuppressWarnings("unchecked")
      ArrayList<WindowEx> satellites = (ArrayList<WindowEx>)satellites_.clone();
      for (WindowEx satelliteWnd : satellites)
      {
         try
         {
            if (satelliteWnd.isClosed())
            {
               if (removeWindows == null)
                  removeWindows = new ArrayList<WindowEx>();
               removeWindows.add(satelliteWnd);
            }
            else
            {
               callDispatchEvent(satelliteWnd, clientEvent);
            }
         }
         catch(Throwable e)
         {
         }
      } 
      
      // remove windows if necessary
      if (removeWindows != null)
      {
         for (WindowEx satelliteWnd : removeWindows)
         {
            satellites_.remove(satelliteWnd);
         }
      }
   }
   
   // close all satellites when we are closed
   @Override
   public void onClose(CloseEvent<Window> event)
   {
      closeAllSatellites();
   }
   
   // called by satellites to connect themselves with the main window
   private void registerAsSatellite(JavaScriptObject wnd)
   {
      // get the satellite and add it to our list
      WindowEx satelliteWnd = wnd.<WindowEx>cast();
      satellites_.add(satelliteWnd);
      
      // call setSessionInfo
      callSetSessionInfo(satelliteWnd, session_.getSessionInfo());
   }
   
   // export the global function requried for satellites to register
   private native void exportSatelliteRegistrationCallback() /*-{
      var manager = this;     
      $wnd.registerAsRStudioSatellite = $entry(
         function(satelliteWnd) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::registerAsSatellite(Lcom/google/gwt/core/client/JavaScriptObject;)(satelliteWnd);
         }
      ); 
   }-*/;
   
   // call setSessionInfo on a satellite
   private native void callSetSessionInfo(JavaScriptObject satellite,
                                          JavaScriptObject sessionInfo) /*-{
      satellite.setRStudioSatelliteSessionInfo(sessionInfo);
   }-*/;
   
   // dispatch event to a satellite
   private native void callDispatchEvent(JavaScriptObject satellite,
                                         JavaScriptObject clientEvent) /*-{
      satellite.dispatchEventToRStudioSatellite(clientEvent);
   }-*/;
   
   private final Session session_;
   private final ArrayList<WindowEx> satellites_ = new ArrayList<WindowEx>();

   
}


