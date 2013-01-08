/*
 * SatelliteManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.layout.ScreenUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
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
   public SatelliteManager(
         Session session,
         Provider<ApplicationUncaughtExceptionHandler> pUncaughtExceptionHandler)
   {
      session_ = session;
      pUncaughtExceptionHandler_ = pUncaughtExceptionHandler;
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
               // for web mode bring the window to the front, notify
               // it that it has been reactivated, then exit. 
               if (!Desktop.isDesktop())
               {
                  // don't do this for chrome (since it doesn't allow
                  // window.focus). for chrome we'll just fall through
                  // and openSatelliteWindow will be called and the 
                  // window will be reloaded)
                  if (!BrowseCap.isChrome())
                  {
                     window.focus();
                     callNotifyReactivated(window, params);
                     return;
                  }
               }
               // desktop mode: activate and return
               else
               {
                  Desktop.getFrame().activateSatelliteWindow(
                    SatelliteUtils.getSatelliteWindowName(satellite.getName()));
                  callNotifyReactivated(window, params);
                  return;
               }
            }
         }
      }
      
      // Start buffering events sent to this satellite. That way, we won't miss
      // anything while the satellite is being loaded/reactivated
      if (!pendingEventsBySatelliteName_.containsKey(name))
      {
         pendingEventsBySatelliteName_.put(name,
                                           new ArrayList<JavaScriptObject>());
      }

      // record satellite params for subsequent setting (this value is read
      // by the satellite within the call to registerAsSatellite)
      if (params != null)
         satelliteParams_.put(name, params);
 
      // open the satellite - it will call us back on registerAsSatellite
      // at which time we'll call setSessionInfo, setParams, etc.
      Size windowSize = ScreenUtils.getAdjustedWindowSize(preferredSize);
      RStudioGinjector.INSTANCE.getGlobalDisplay().openSatelliteWindow(
                                              name,
                                              windowSize.width,
                                              windowSize.height);
   }
   
   public boolean satelliteWindowExists(String name)
   {
      return getSatelliteWindowObject(name) != null;
   }
   
   public WindowEx getSatelliteWindowObject(String name)
   {
      for (ActiveSatellite satellite : satellites_)
         if (satellite.getName().equals(name) && 
             !satellite.getWindow().isClosed())
            return satellite.getWindow();
            
      return null;
   }
   
   public void activateSatelliteWindow(String name)
   {
      if (Desktop.isDesktop())
      {
         Desktop.getFrame().activateSatelliteWindow(
               SatelliteUtils.getSatelliteWindowName(name));
      }
      else
      {
         for (ActiveSatellite satellite : satellites_)
         {
            if (satellite.getName().equals(name) && 
                !satellite.getWindow().isClosed())
            {
               satellite.getWindow().focus();
               break;
            }
         }   
      }
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
      pendingEventsBySatelliteName_.clear();
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
            // If we're buffering events for this satellite, then don't dispatch
            // them
            if (pendingEventsBySatelliteName_.containsKey(satellite.getName()))
               continue;

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

      for (Entry<String, ArrayList<JavaScriptObject>> entry :
                                       pendingEventsBySatelliteName_.entrySet())
      {
         entry.getValue().add(clientEvent);
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
   private void registerAsSatellite(final String name, JavaScriptObject wnd)
   {
      // get the satellite and add it to our list. in some cases (such as
      // the Ctrl+R reload of an existing satellite window) we actually
      // already have a reference to this satellite in our list so in that
      // case we make sure not to add a duplicate
      WindowEx satelliteWnd = wnd.<WindowEx>cast();
      ActiveSatellite satellite = new ActiveSatellite(name, satelliteWnd);
      if (!satellites_.contains(satellite))
         satellites_.add(satellite);
      
      // call setSessionInfo
      callSetSessionInfo(satelliteWnd, session_.getSessionInfo());
      
      // call setParams
      JavaScriptObject params = satelliteParams_.get(name);
      if (params != null)
         callSetParams(satelliteWnd, params);
   }

   private void flushPendingEvents(String name)
   {
      ArrayList<JavaScriptObject> events =
                                    pendingEventsBySatelliteName_.remove(name);

      if (events == null || events.size() == 0)
         return;

      for (ActiveSatellite satellite :
                                    new ArrayList<ActiveSatellite>(satellites_))
      {
         if (satellite.getName().equals(name)
             && !satellite.getWindow().isClosed())
         {
            for (JavaScriptObject evt : events)
            {
               try
               {
                  callDispatchEvent(satellite.getWindow(), evt);
               }
               catch (Exception e)
               {
                  pUncaughtExceptionHandler_.get().onUncaughtException(e);
               }
            }
         }
      }
   }
   
   // export the global function requried for satellites to register
   private native void exportSatelliteRegistrationCallback() /*-{
      var manager = this;     
      $wnd.registerAsRStudioSatellite = $entry(
         function(name, satelliteWnd) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::registerAsSatellite(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(name, satelliteWnd);
         }
      );
      $wnd.flushPendingEvents = $entry(
         function(name) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::flushPendingEvents(Ljava/lang/String;)(name);
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
   
   
   // alert callback (used for testing html preview sandbox)
   //private void showAlert(String message)
   //{
   //   RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage("Alert", 
   //                                                                 message);
   //}
   
   //private native void exportSatelliteAlertCallback() /*-{
   //   var manager = this;
   //   $wnd.rstudioSatelliteAlert = $entry(
   //      function(message) {
   //        manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::showAlert(Ljava/lang/String;)(message);
   //      }
   //   ); 
   //}-*/;
   
   private final Session session_;
   private final Provider<ApplicationUncaughtExceptionHandler> pUncaughtExceptionHandler_;
   private final ArrayList<ActiveSatellite> satellites_ = 
                                          new ArrayList<ActiveSatellite>();
   
   private final HashMap<String,JavaScriptObject> satelliteParams_ = 
                                new HashMap<String,JavaScriptObject>();

   private final HashMap<String, ArrayList<JavaScriptObject>>
         pendingEventsBySatelliteName_ = new HashMap<String, ArrayList<JavaScriptObject>>();

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
      
      @Override 
      public boolean equals(Object other)
      {
         if (other == null)
            return false;
         
         ActiveSatellite otherSatellite = (ActiveSatellite)other;
         
         return getName().equals(otherSatellite.getName()) &&
                getWindow().equals(otherSatellite.getWindow());
      }
      
      private final String name_;
      private final WindowEx window_;
   }
   
}


