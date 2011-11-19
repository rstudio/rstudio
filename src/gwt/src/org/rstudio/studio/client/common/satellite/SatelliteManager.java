/*
 * SatelliteManager.java
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
package org.rstudio.studio.client.common.satellite;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.model.Session;

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
    
   // open a satellite window (re-activate existing if possible)
   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize)
   {
      // satellites can't launch other satellites -- this is because the 
      // delegating/forwarding of remote server calls and events doesn't
      // cascade correctly -- it wouldn't be totally out of the question
      // to make htis work but we'd rather not have this complexity
      // if we don't need to.
      if (isCurrentWindowSatellite())
      {
         Debug.log("Satellite windows can't launch other satellites");
         assert false;
         return;
      }
      
      // check for a re-activation of an existing window
      for (ActiveSatellite satellite : satellites_)
      {
         if (satellite.getName().equals(name))
         {
            WindowEx window = satellite.getWindow();
            if (!window.isClosed())
            {
               // for web mode  bring the window to the front, notify
               // it that it has been reactivated, then exit
               if (!Desktop.isDesktop())
               {
                  window.focus();
                  callNotifyReactivated(window, params);
                  return;
               }
               // for desktop mode we just notify the window it is being
               // re-activated -- the actual re-activation is handled
               // by a different codepath within DesktopWebView so we 
               // continue on with processing after callNotifyReactivated
               else
               {
                  callNotifyReactivated(window, params);
               }
            }
         }
      }
      
      // record satellite params for subsequent setting (this value is read
      // by the satellite within the call to registerAsSatellite)
      if (params != null)
         satelliteParams_.put(name, params);
 
      // open the satellite - it will call us back on registerAsSatellite
      // at which time we'll call setSessionInfo, setParams, etc.
      RStudioGinjector.INSTANCE.getGlobalDisplay().openSatelliteWindow(
                                              name,
                                              preferredSize.width,
                                              preferredSize.height);
   }
   
   public boolean satelliteWindowExists(String name)
   {
      for (ActiveSatellite satellite : satellites_)
         if (satellite.getName().equals(name))
            return true;
      
      return false;
   }
   
   // close all satellite windows
   public void closeAllSatellites()
   {
      for (ActiveSatellite satellite : satellites_)
      {
         try
         {
            satellite.getWindow().close();
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
      ArrayList<ActiveSatellite> removeWindows = null;
        
      // iterate over the satellites (make a copy to avoid races if
      // for some reason firing an event creates or destroys a satellite)
      @SuppressWarnings("unchecked")
      ArrayList<ActiveSatellite> satellites = 
                           (ArrayList<ActiveSatellite>)satellites_.clone();
      for (ActiveSatellite satellite : satellites)
      {
         try
         {
            WindowEx satelliteWnd = satellite.getWindow();
            if (satelliteWnd.isClosed())
            {
               if (removeWindows == null)
                  removeWindows = new ArrayList<ActiveSatellite>();
               removeWindows.add(satellite);
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
         for (ActiveSatellite satellite : removeWindows)
         {
            satellites_.remove(satellite);
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
   private void registerAsSatellite(String name, JavaScriptObject wnd)
   {
      // get the satellite and add it to our list
      WindowEx satelliteWnd = wnd.<WindowEx>cast();
      satellites_.add(new ActiveSatellite(name, satelliteWnd));
      
      // call setSessionInfo
      callSetSessionInfo(satelliteWnd, session_.getSessionInfo());
      
      // call setParams
      JavaScriptObject params = satelliteParams_.get(name);
      if (params != null)
         callSetParams(satelliteWnd, params);
   }
   
   // export the global function requried for satellites to register
   private native void exportSatelliteRegistrationCallback() /*-{
      var manager = this;     
      $wnd.registerAsRStudioSatellite = $entry(
         function(name, satelliteWnd) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::registerAsSatellite(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(name, satelliteWnd);
         }
      ); 
   }-*/;
   
   // call setSessionInfo on a satellite
   private native void callSetSessionInfo(JavaScriptObject satellite,
                                          JavaScriptObject sessionInfo) /*-{
      satellite.setRStudioSatelliteSessionInfo(sessionInfo);
   }-*/;
   
   // call setParams on a satellite
   private native void callSetParams(JavaScriptObject satellite,
                                     JavaScriptObject params) /*-{
      satellite.setRStudioSatelliteParams(params);
   }-*/;
   
   // call notifyReactivated on a satellite
   private native void callNotifyReactivated(JavaScriptObject satellite,
                                         JavaScriptObject params) /*-{
      satellite.notifyRStudioSatelliteReactivated(params);
   }-*/;
   
   // dispatch event to a satellite
   private native void callDispatchEvent(JavaScriptObject satellite,
                                         JavaScriptObject clientEvent) /*-{
      satellite.dispatchEventToRStudioSatellite(clientEvent);
   }-*/;
   
   // check whether the current window is a satellite (note this method
   // is also implemeted in the Satellite class -- we don't want this class
   // to depend on Satellite so we duplicate the definition)
   private native boolean isCurrentWindowSatellite() /*-{
      return !!$wnd.isRStudioSatellite;
   }-*/;
   
   private final Session session_;
   private final ArrayList<ActiveSatellite> satellites_ = 
                                          new ArrayList<ActiveSatellite>();
   
   private final HashMap<String,JavaScriptObject> satelliteParams_ = 
                                new HashMap<String,JavaScriptObject>();

   private class ActiveSatellite
   {
      public ActiveSatellite(String name, WindowEx window)
      {
         name_ = name;
         window_ = window;
      }
      
      public String getName()
      {
         return name_;
      }
      
      public WindowEx getWindow()
      {
         return window_;
      }
      
      private final String name_;
      private final WindowEx window_;
   }
   
}


