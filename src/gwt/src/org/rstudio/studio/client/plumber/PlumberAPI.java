/*
 * PlumberAPI.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.plumber;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowCloseMonitor;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.plumber.model.PlumberServerOperations;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.plumber.events.LaunchPlumberAPIEvent;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.plumber.model.PlumberRunCmd;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerClearedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PlumberAPI implements PlumberAPIStatusEvent.Handler,
                                   ConsoleBusyEvent.Handler,
                                   DebugModeChangedEvent.Handler,
                                   RestartStatusEvent.Handler, 
                                   WindowClosedEvent.Handler,
                                   LaunchPlumberAPIEvent.Handler,
                                   InterruptStatusEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, PlumberAPI> {}

   @Inject
   public PlumberAPI(EventBus eventBus, 
                     Commands commands,
                     Binder binder,
                     Provider<UserPrefs> pPrefs,
                     final SatelliteManager satelliteManager, 
                     PlumberServerOperations server,
                     GlobalDisplay display,
                     DependencyManager dependencyManager,
                     ApplicationInterrupt interrupt)
   {
      eventBus_ = eventBus;
      satelliteManager_ = satelliteManager;
      commands_ = commands;
      pPrefs_ = pPrefs;
      server_ = server;
      display_ = display;
      isBusy_ = false;
      currentViewType_ = UserPrefs.PLUMBER_VIEWER_TYPE_NONE;
      dependencyManager_ = dependencyManager;
      interrupt_ = interrupt;
      
      eventBus_.addHandler(PlumberAPIStatusEvent.TYPE, this);
      eventBus_.addHandler(LaunchPlumberAPIEvent.TYPE, this);
      eventBus_.addHandler(ConsoleBusyEvent.TYPE, this);
      eventBus_.addHandler(DebugModeChangedEvent.TYPE, this);
      eventBus_.addHandler(RestartStatusEvent.TYPE, this);
      eventBus_.addHandler(WindowClosedEvent.TYPE, this);
      eventBus_.addHandler(InterruptStatusEvent.TYPE, this);

      binder.bind(commands, this);
      exportPlumberAPIClosedCallback();
   }
   
   // Event handlers ----------------------------------------------------------
   
   @Override
   public void onPlumberAPIStatus(PlumberAPIStatusEvent event)
   {
      if (StringUtil.equals(event.getParams().getState(), PlumberAPIParams.STATE_STARTED))
      {
         currentViewType_ = event.getParams().getViewerType();

         // open the window to view the API if needed
         if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
         {
            activateWindow(event.getParams());
         }
         else if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER)
         {
            display_.openWindow(event.getParams().getUrl());
         }
         params_ = event.getParams();
         
         // if the API was started from the same path as a pending satellite
         // closure, don't shut down the API when the close finishes
         if (StringUtil.equals(event.getParams().getPath(), satelliteClosePath_))
         {
            stopOnNextClose_ = false;
         }
      }
      else if (StringUtil.equals(event.getParams().getState(), PlumberAPIParams.STATE_STOPPED))
      {
         params_ = null;
      }
   }

   @Override
   public void onLaunchPlumberAPI(LaunchPlumberAPIEvent event)
   {
      launchPlumberAPI(event.getPath());
   }

   @Override
   public void onConsoleBusy(ConsoleBusyEvent event)
   {
      isBusy_ = event.isBusy();
      
      // if the browser is up and R stops being busy, presume it's because the
      // API has stopped
      if (!isBusy_ && params_ != null && 
          params_.getViewerType() == UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER)
      {
         params_.setState(PlumberAPIParams.STATE_STOPPED);
         eventBus_.fireEvent(new PlumberAPIStatusEvent(params_));
      }
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // When leaving debug mode while the Plumber API is open in a 
      // browser, automatically return to the API by activating the window.
      if (!event.debugging() && 
          params_ != null &&
          currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW) 
      {
         satelliteManager_.activateSatelliteWindow(PlumberAPISatellite.NAME);
      }
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // Close the satellite window when R restarts, since this leads to the
      // Plumber API being terminated. Closing the window triggers a 
      // PlumberAPIStatusEvent that allows the rest of the UI a chance
      // to react to the API's termination.
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED &&
          currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
      {
            satelliteManager_.closeSatelliteWindow(PlumberAPISatellite.NAME);
      }
   }

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      // we get this event on the desktop (currently only Cocoa); it lets us
      // know that the satellite has been shut down even in the case where the
      // script window that ordinarily would let us know has been disconnected.
      if (!StringUtil.equals(event.getName(), PlumberAPISatellite.NAME))
         return;

      // stop the API if this event wasn't generated by a disconnect
      if (params_ != null && disconnectingUrl_ == null && stopOnNextClose_)
      {
         params_.setState(PlumberAPIParams.STATE_STOPPING);
         notifyPlumberAPIClosed(params_);
      }
   }

   @Handler
   public void onPlumberRunInPane()
   {
      setPlumberViewerType(UserPrefs.PLUMBER_VIEWER_TYPE_PANE);
   }
   
   @Handler
   public void onPlumberRunInViewer()
   {
      setPlumberViewerType(UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW);
   }

   @Handler
   public void onPlumberRunInBrowser()
   {
      setPlumberViewerType(UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER);
   }
   
   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      // If API is stopped via Console, ensure Satellite is closed
      if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
      {
            satelliteManager_.closeSatelliteWindow(PlumberAPISatellite.NAME);
      }
   }
    
   private void launchPlumberAPI(final String filePath)
   {
      String fileDir = filePath.substring(0, filePath.lastIndexOf("/"));
      if (fileDir.equals(currentAppPath()))
      {
         // The API being launched is the one already running; open and reload the API.
         if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
         {
            satelliteManager_.dispatchCommand(commands_.reloadPlumberAPI(), PlumberAPISatellite.NAME);
            activateWindow();
         } 
         else if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_PANE &&
                  commands_.viewerRefresh().isEnabled())
         {
            commands_.viewerRefresh().execute();
         }
         else if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER)
         {
            eventBus_.fireEvent(new PlumberAPIStatusEvent(params_));
         }
         return;
      }
      else if (params_ != null && isBusy_)
      {
         // There's another API running. Interrupt it and then start this one.
         interrupt_.interruptR(() -> launchPlumberFile(filePath));
      }
      else
      {
         // Nothing else running, start this API.
         dependencyManager_.withRPlumber("Running Plumber API", () -> launchPlumberFile(filePath));
      }
   }

   private String currentAppPath()
   {
      if (params_ != null)
         return params_.getPath();
      return null;
   }
   
   private void notifyPlumberAPIDisconnected(JavaScriptObject params)
   {
      PlumberAPIParams apiState = params.cast();
      if (params_ == null)
         return;
      
      // remember that this URL is disconnecting (so we don't interrupt R when
      // the window is torn down)
      disconnectingUrl_ = apiState.getUrl();
   }

   private void notifyPlumberAPIClosed(final JavaScriptObject params)
   {
      // if we don't know that an API is running, ignore this event
      if (params_ == null)
         return;

      satelliteClosePath_ = params_.getPath();
      
      // wait for confirmation of window closure (could be a reload)
      WindowCloseMonitor.monitorSatelliteClosure(PlumberAPISatellite.NAME, () -> {
         // satellite closed for real; shut down the API 
         satelliteClosePath_ = null;
         onPlumberAPIClosed(params);
      }, () -> {
         // satellite didn't actually close (it was a reload)
         satelliteClosePath_ = null;
      });
   }
   
   private void onPlumberAPIClosed(JavaScriptObject params)
   {
      PlumberAPIParams apiState = params.cast();

      // this completes any pending disconnection
      disconnectingUrl_ = null;
      
      // if we were asked not to stop when the window closes (i.e. when 
      // changing viewer types), bail out
      if (!stopOnNextClose_)
      {
         stopOnNextClose_ = true;
         return;
      }
      
      // If the API is stopping, then the user initiated the stop by
      // closing the API window. Interrupt R to stop the Plumber API.
      if (StringUtil.equals(apiState.getState(), PlumberAPIParams.STATE_STOPPING))
      {
         if (commands_.interruptR().isEnabled()) 
            commands_.interruptR().execute();
         apiState.setState(PlumberAPIParams.STATE_STOPPED);
      }
      eventBus_.fireEvent(new PlumberAPIStatusEvent((PlumberAPIParams) params.cast()));
   }
   
   private final native void exportPlumberAPIClosedCallback()/*-{
      var registry = this;     
      $wnd.notifyPlumberAPIClosed = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.plumber.PlumberAPI::notifyPlumberAPIClosed(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
      $wnd.notifyPlumberAPIDisconnected = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.plumber.PlumberAPI::notifyPlumberAPIDisconnected(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
   }-*/;

   private void setPlumberViewerType(String viewerType)
   {
      UserPrefs prefs = pPrefs_.get();
      prefs.plumberViewerType().setGlobalValue(viewerType);
      prefs.writeUserPrefs();

      // if we have a running Plumber API and the viewer type has changed, 
      // snap the API into the new location
      if (currentViewType_ != viewerType && params_ != null)
      {
         // if transitioning away from the pane or the window, close down
         // the old instance
         if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_PANE ||
             currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW) 
         {
            if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW)
            {
               stopOnNextClose_ = false;
               satelliteManager_.closeSatelliteWindow(PlumberAPISatellite.NAME);
            }
            else
            {
               eventBus_.fireEvent(new ViewerClearedEvent(false));
            }
         }
         
         // assign new viewer type
         currentViewType_ = viewerType;
         params_.setViewerType(viewerType);
         
         if (currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_PANE ||
             currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_WINDOW ||
             currentViewType_ == UserPrefs.PLUMBER_VIEWER_TYPE_BROWSER)
         {
            eventBus_.fireEvent(new PlumberAPIStatusEvent(params_));
         }
      }
   }
   
   private void launchPlumberFile(String file)
   {
      server_.getPlumberRunCmd(file, 
            new ServerRequestCallback<PlumberRunCmd>()
            {
               @Override
               public void onResponseReceived(PlumberRunCmd cmd)
               {
                  eventBus_.fireEvent(new SendToConsoleEvent(cmd.getRunCmd(), true));
               }
   
               @Override
               public void onError(ServerError error)
               {
                  display_.showErrorMessage("Plumber API Launch Failed", error.getMessage());
               }
            });
   }
   
   private void activateWindow()
   {
      activateWindow(null);
   }
   
   private void activateWindow(PlumberAPIParams params)
   {
      WindowEx win = satelliteManager_.getSatelliteWindowObject(PlumberAPISatellite.NAME);
      boolean isRefresh = win != null && 
            (params == null || (params_ != null &&
                                StringUtil.equals(params.getPath(), params_.getPath())));
      boolean isChrome = !Desktop.isDesktop() && BrowseCap.isChrome();
      if (params != null)
         params_ = params;
      if (win == null || (!isRefresh && !isChrome))
      {
         int width = 910;
         
         // If there's no window yet, or we're switching APIs in a browser
         // other than Chrome, do a normal open
         satelliteManager_.openSatellite(PlumberAPISatellite.NAME,     
                                         params_, new Size(width, 1100));   
      } 
      else if (isChrome)
      {
         // we have a window and we're Chrome, so force a close and reopen
         satelliteManager_.forceReopenSatellite(PlumberAPISatellite.NAME, 
                                                params_,
                                                true);
      }
      else
      {
         satelliteManager_.activateSatelliteWindow(PlumberAPISatellite.NAME);
      }
   }
   
   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;
   private final DependencyManager dependencyManager_;
   private final Commands commands_;
   private final Provider<UserPrefs> pPrefs_;
   private final PlumberServerOperations server_;
   private final GlobalDisplay display_;
   private final ApplicationInterrupt interrupt_;

   private PlumberAPIParams params_;
   private String disconnectingUrl_;
   private boolean isBusy_;
   private boolean stopOnNextClose_ = true;
   private String satelliteClosePath_ = null;
   private String currentViewType_;
}
