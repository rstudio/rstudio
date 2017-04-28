/*
 * SatelliteManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.layout.ScreenUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.satellite.events.AllSatellitesClosingEvent;
import org.rstudio.studio.client.common.satellite.events.SatelliteClosedEvent;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.common.satellite.events.WindowOpenedEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
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
         EventBus events,
         Provider<SourceWindowManager> pSourceWindowManager,
         Provider<ApplicationUncaughtExceptionHandler> pUncaughtExceptionHandler,
         Provider<UIPrefs> pUIPrefs)
   {
      session_ = session;
      events_ = events;
      pUIPrefs_ = pUIPrefs;
      pSourceWindowManager_ = pSourceWindowManager;
      pUncaughtExceptionHandler_ = pUncaughtExceptionHandler;
      initializeCommonCallbacks();
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
   
   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize)
   {
      openSatellite(name, params, preferredSize, true);
   }
    
   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize,
                             boolean activate)
   {
      openSatellite(name, params, preferredSize, true, null, activate);
   }
   
   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize,
                             Point position,
                             boolean activate)
   {
      openSatellite(name, params, preferredSize, true, position, activate);
   }
   

   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize, 
                             boolean adjustSize, 
                             Point position)
   {
      openSatellite(name, params, preferredSize, adjustSize, position, true);
   }
   
   // open a satellite window (optionally activate existing)
   public void openSatellite(String name,
                             JavaScriptObject params,
                             Size preferredSize, 
                             boolean adjustSize, 
                             Point position,
                             boolean activate)
   {
      // satellites can't launch other satellites -- this is because the 
      // delegating/forwarding of remote server calls and events doesn't
      // cascade correctly -- it wouldn't be totally out of the question
      // to make this work but we'd rather not have this complexity
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
                  if (!BrowseCap.isChrome() || !activate)
                  {
                     if (activate)
                        window.focus();
                     callNotifyReactivated(window, params);
                     return;
                  }
                  else
                  {
                     // for Chrome, let the window know it's about to be 
                     // closed and reopened
                     callNotifyPendingReactivate(window);
                  }
               }
               // desktop mode: activate and return
               else
               {
                  if (activate) 
                  {
                     Desktop.getFrame().activateSatelliteWindow(
                       SatelliteUtils.getSatelliteWindowName(satellite.getName()));
                  }
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
 
      // set size and position, if desired
      Size windowSize = adjustSize ? 
            ScreenUtils.getAdjustedWindowSize(preferredSize) : 
            preferredSize;
      NewWindowOptions options = new NewWindowOptions();
      options.setFocus(activate);
      if (position != null)
         options.setPosition(position);

      // open the satellite - it will call us back on registerAsSatellite
      // at which time we'll call setSessionInfo, setParams, etc.
      RStudioGinjector.INSTANCE.getGlobalDisplay().openSatelliteWindow(
                                              name,
                                              windowSize.width,
                                              windowSize.height, 
                                              options);
   }
   
   // Forcefully reopen a satellite window. This refreshes the window and
   // pushes it to the front in Chrome. It should be used as a last resort;
   // if responding to a UI event, use openSatellite instead, since Chrome
   // permits window.open to reactivate windows in that context. 
   public void forceReopenSatellite(final String name, 
                                    final JavaScriptObject params,
                                    boolean activate)
   {
      Size preferredSize = null;
      Point preferredPos = null;
      for (ActiveSatellite satellite : satellites_)
      {
         if (satellite.getName().equals(name) && 
             !satellite.getWindow().isClosed())
         {
            // save the window's geometry so we can restore it after the window 
            // is destroyed
            final WindowEx win = satellite.getWindow();
            Document doc = win.getDocument();
            preferredSize = new Size(doc.getClientWidth(), doc.getClientHeight());
            preferredPos = new Point(win.getLeft(), win.getTop());
            callNotifyPendingReactivate(win);
            satellite.close();
            break;
         }
      }   
      
      // didn't find an open window to reopen
      if (preferredSize == null)
         return;
      
      // open a new window with the same geometry as the one we just destroyed,
      // but with the newly supplied set of parameters
      final Size windowSize = preferredSize;
      final Point windowPos = preferredPos;
      openSatellite(name, params, windowSize, false, windowPos, activate);
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
      
   public boolean getSatellitesOpen()
   {
      return satellites_.size() >= 1;
   }
   
   // close all satellite windows
   public void closeAllSatellites()
   {
      // let anyone interested know that we're tearing things down; this allows 
      // listeners to distinguish between user-initiated window closure and 
      // the closes we initiate on shutdown/restart/quit/etc.
      events_.fireEvent(new AllSatellitesClosingEvent());

      for (ActiveSatellite satellite : satellites_)
      {
         satellite.close();
      } 
      satellites_.clear();
      pendingEventsBySatelliteName_.clear();
   }
   
   public String getWindowAtPoint(int x, int y)
   {
      // check to see if the point is in any of our satellites
      for (ActiveSatellite satellite : satellites_)
      {
         if (satellite.getWindow() == null || satellite.getWindow().isClosed())
            continue;
         if (isPointWithinSatellite(satellite.getWindow(), x, y))
            return satellite.getName();
      } 
      
      // check to see if the point is inside our own window
      if (DomUtils.elementFromPoint(x - WindowEx.get().getScreenX(), 
            y - WindowEx.get().getScreenY()) != null)
      {
         return "";
      }
      return null;
   }
   
   // close one satellite window 
   public void closeSatelliteWindow(String name)
   {
      for (ActiveSatellite satellite : satellites_)
      {
         if (satellite.getName().equals(name) && 
             !satellite.getWindow().isClosed())
         {
            satellite.close();
            break;
         }
      }   
   }
   
   // dispatch an event to all satellites
   public void dispatchClientEvent(JavaScriptObject clientEvent)
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
   
   // dispatch a command to the named satellite window, or to the main window
   // if no target is specified
   public void dispatchCommand(AppCommand command, String target)
   {
      if (StringUtil.isNullOrEmpty(target))
      {
         callDispatchCommandMain(command.getId());
      }
      else
      {
         for (ActiveSatellite satellite: satellites_)
         {
            if (satellite.getName() == target)
            {
               callDispatchCommandSatellite(
                     satellite.getWindow(), command.getId());
            }
         }
      }
   }
   
   // dispatch a cross-window event to all satellites
   public void dispatchCrossWindowEvent(CrossWindowEvent<?> event)
   {
      for (ActiveSatellite satellite: satellites_)
      {
         events_.fireEventToSatellite(event, satellite.getWindow());
      }
   }
   
   // close all satellites when we are closed
   @Override
   public void onClose(CloseEvent<Window> event)
   {
      closeAllSatellites();
   }
   
   // call notifyPendingReactivate on a satellite
   public native static void callNotifyPendingReactivate(JavaScriptObject satellite) /*-{
      satellite.notifyPendingReactivate();
   }-*/;

   // call notifyPendingClosure on a satellite
   public native static void callNotifyPendingClosure(JavaScriptObject satellite) /*-{
      satellite.notifyPendingClosure();
   }-*/;

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
      
      // augment the current session info with an up-to-date set of source 
      // documents
      SessionInfo sessionInfo = session_.getSessionInfo();
      sessionInfo.setSourceDocuments(
            pSourceWindowManager_.get().getSourceDocs());
      
      // clone the session info so the satellites aren't reading/writing the
      // same copy as the main window; pass the cloned copy along
      JsObject sessionInfoJs = session_.getSessionInfo().cast();
      callSetSessionInfo(satelliteWnd, sessionInfoJs.clone());
      
      // call setParams
      JavaScriptObject params = satelliteParams_.get(name);
      if (params != null)
         callSetParams(satelliteWnd, params);

      // set themes
      events_.fireEventToSatellite(new ThemeChangedEvent(
         pUIPrefs_.get().getFlatTheme().getGlobalValue()),
         satelliteWnd);
   }
   
   // called to register child windows (not necessarily full-fledged 
   // satellites). only used in desktop mode, since in server mode we have the
   // child window object as a return value from window.open.
   private void registerDesktopChildWindow (String name, JavaScriptObject window)
   {
      events_.fireEvent(new WindowOpenedEvent(name, (WindowEx) window.cast()));
   }
   
   private void unregisterDesktopChildWindow (String name)
   {
      if (SatelliteUtils.windowNameIsSatellite(name))
         name = SatelliteUtils.getWindowNameFromSatelliteName(name);
      events_.fireEvent(new WindowClosedEvent(name));
      events_.fireEvent(new SatelliteClosedEvent(name));

      // remove this satellite from the list of active satellites; ordinarily
      // we'd rely on the window object's isClosed() method, but it's possible
      // in some cases for the window to enter a zombie state in which it exists
      // and reports as open even after being closed, apparently due to 
      // exceptions occurring during teardown (see case 4436). 
      for (ActiveSatellite satellite: satellites_)
      {
         if (satellite.getName() == name)
         {
            satellites_.remove(satellite);
         }
      }
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
   
   // export the global function required for satellites to register
   private native void exportSatelliteRegistrationCallback() /*-{
      var manager = this;     
      
      $wnd.$RStudio = {};
      
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
      $wnd.registerDesktopChildWindow = $entry(
         function(name, wnd) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::registerDesktopChildWindow(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(name, wnd);
         }
      );
      $wnd.unregisterDesktopChildWindow = $entry(
         function(name, wnd) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::unregisterDesktopChildWindow(Ljava/lang/String;)(name);
         }
      );
      $wnd.notifyRStudioSatelliteClosed = $entry(
         function(name) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::notifyRStudioSatelliteClosed(Ljava/lang/String;)(name);
         }
      );
   }-*/;
   
   private final native void initializeCommonCallbacks() /*-{
      var manager = this;
      $wnd.dispatchRStudioCommandExternal = $entry(
         function(commandId) {
            manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::dispatchCommandExternal(Ljava/lang/String;)(commandId);
         }
      ); 
      
      $wnd.getWindowAtPoint = $entry(
         function(x, y) {
            return manager.@org.rstudio.studio.client.common.satellite.SatelliteManager::getWindowAtPoint(II)(x,y);
         }
      );
      
   }-*/;
   
   private void dispatchCommandExternal(String commandId)
   {  
      AppCommand cmd = RStudioGinjector.INSTANCE.getCommands()
            .getCommandById(commandId);
      if (cmd != null)
         cmd.execute();
   }
   
   private void notifyRStudioSatelliteClosed(String name)
   {
      events_.fireEvent(new SatelliteClosedEvent(name));
   }
   
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
   
   // dispatch command to a satellite
   private native void callDispatchCommandSatellite(JavaScriptObject satellite,
                                           String commandId) /*-{
      satellite.dispatchRStudioCommandExternal(commandId);
   }-*/;
   
   private native boolean isPointWithinSatellite(JavaScriptObject satellite, 
         int x, int y) /*-{
      return satellite.isPointWithinSatellite(x, y);
   }-*/;
   
   private native void callDispatchCommandMain(String commandId) /*-{
      $wnd.opener.dispatchRStudioCommandExternal(commandId);
   }-*/;
   
   // check whether the current window is a satellite (note this method
   // is also implemented in the Satellite class -- we don't want this class
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
   private final EventBus events_;
   private final Provider<SourceWindowManager> pSourceWindowManager_;
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
      
      public void close()
      {
         try
         {
            callNotifyPendingClosure(getWindow());
            getWindow().close();
         }
         catch(Throwable e)
         {
         }
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
 
   private final Provider<UIPrefs> pUIPrefs_;
}


