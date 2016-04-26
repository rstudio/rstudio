/*
 * ShinyApplication.java
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowCloseMonitor;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.ApplicationInterrupt.InterruptHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.satellite.events.WindowClosedEvent;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.LaunchShinyApplicationEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerClearedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ShinyApplication implements ShinyApplicationStatusEvent.Handler,
                                         ConsoleBusyEvent.Handler,
                                         DebugModeChangedEvent.Handler,
                                         RestartStatusEvent.Handler, 
                                         WindowClosedEvent.Handler,
                                         LaunchShinyApplicationEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, ShinyApplication> {}

   @Inject
   public ShinyApplication(EventBus eventBus, 
                           Commands commands,
                           Binder binder,
                           Provider<UIPrefs> pPrefs,
                           final SatelliteManager satelliteManager, 
                           ShinyServerOperations server,
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
      currentViewType_ = ShinyViewerType.SHINY_VIEWER_NONE;
      dependencyManager_ = dependencyManager;
      interrupt_ = interrupt;
      
      eventBus_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      eventBus_.addHandler(LaunchShinyApplicationEvent.TYPE, this);
      eventBus_.addHandler(ConsoleBusyEvent.TYPE, this);
      eventBus_.addHandler(DebugModeChangedEvent.TYPE, this);
      eventBus_.addHandler(RestartStatusEvent.TYPE, this);
      eventBus_.addHandler(WindowClosedEvent.TYPE, this);

      binder.bind(commands, this);
      exportShinyAppClosedCallback();
   }
   
   // Event handlers ----------------------------------------------------------
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      if (event.getParams().getState() == ShinyApplicationParams.STATE_STARTED)
      {
         currentViewType_ = event.getParams().getViewerType();

         // open the window to view the application if needed
         if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW)
         {
            activateWindow(event.getParams());
         }
         else if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_BROWSER)
         {
            display_.openWindow(event.getParams().getUrl());
         }
         params_ = event.getParams();
         
         // if the app was started from the same path as a pending satellite
         // closure, don't shut down the app when the close finishes
         if (event.getParams().getPath() == satelliteClosePath_)
         {
            stopOnNextClose_ = false;
         }
      }
      else if (event.getParams().getState() == ShinyApplicationParams.STATE_STOPPED)
      {
         params_ = null;
      }
   }

   @Override
   public void onLaunchShinyApplication(LaunchShinyApplicationEvent event)
   {
      launchShinyApplication(event.getPath(), event.getExtendedType());
   }

   @Override
   public void onConsoleBusy(ConsoleBusyEvent event)
   {
      isBusy_ = event.isBusy();
      
      // if the browser is up and R stops being busy, presume it's because the
      // app has stopped
      if (!isBusy_ && params_ != null && 
          params_.getViewerType() == ShinyViewerType.SHINY_VIEWER_BROWSER)
      {
         params_.setState(ShinyApplicationParams.STATE_STOPPED);
         eventBus_.fireEvent(new ShinyApplicationStatusEvent(params_));
      }
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // When leaving debug mode while the Shiny application is open in a 
      // browser, automatically return to the app by activating the window.
      if (!event.debugging() && 
          params_ != null &&
          currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW) 
      {
         satelliteManager_.activateSatelliteWindow(
               ShinyApplicationSatellite.NAME);
      }
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // Close the satellite window when R restarts, since this leads to the
      // Shiny server being terminated. Closing the window triggers a 
      // ShinyApplicationStatusEvent that allows the rest of the UI a chance
      // to react to the app's termination.
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED &&
          currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW)
      {
            satelliteManager_.closeSatelliteWindow(
                  ShinyApplicationSatellite.NAME);
      }
   }

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      // we get this event on the desktop (currently only Cocoa); it lets us
      // know that the satellite has been shut down even in the case where the
      // script window that ordinarily would let us know has been disconnected.
      if (!event.getName().equals(ShinyApplicationSatellite.NAME))
         return;

      // stop the app if this event wasn't generated by a disconnect
      if (params_ != null && disconnectingUrl_ == null && stopOnNextClose_)
      {
         params_.setState(ShinyApplicationParams.STATE_STOPPING);
         notifyShinyAppClosed(params_);
      }
   }

   // Command handlers --------------------------------------------------------

   @Handler
   public void onShinyRunInPane()
   {
      setShinyViewerType(ShinyViewerType.SHINY_VIEWER_PANE);
   }
   
   @Handler
   public void onShinyRunInViewer()
   {
      setShinyViewerType(ShinyViewerType.SHINY_VIEWER_WINDOW);
   }

   @Handler
   public void onShinyRunInBrowser()
   {
      setShinyViewerType(ShinyViewerType.SHINY_VIEWER_BROWSER);
   }
   
   // Public methods ----------------------------------------------------------

   // Private methods ---------------------------------------------------------
   
   private void launchShinyApplication(final String filePath, 
         final String extendedType)
   {
      String fileDir = filePath.substring(0, filePath.lastIndexOf("/"));
      if (fileDir.equals(currentAppPath()))
      {
         // The app being launched is the one already running; open and
         // reload the app.
         if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW)
         {
            satelliteManager_.dispatchCommand(commands_.reloadShinyApp(), 
                  ShinyApplicationSatellite.NAME);
            activateWindow();
         } 
         else if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_PANE &&
                  commands_.viewerRefresh().isEnabled())
         {
            commands_.viewerRefresh().execute();
         }
         else if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_BROWSER)
         {
            eventBus_.fireEvent(new ShinyApplicationStatusEvent(params_));
         }
         return;
      }
      else if (params_ != null && isBusy_)
      {
         // There's another app running. Interrupt it and then start this one.
         interrupt_.interruptR(new InterruptHandler() {
            @Override
            public void onInterruptFinished()
            {
               launchShinyFile(filePath, extendedType);
            }
         });
      }
      else
      {
         // Nothing else running, start this app.
         dependencyManager_.withShiny("Running Shiny applications",
               new Command() {
                  @Override
                  public void execute()
                  {
                     launchShinyFile(filePath, extendedType);
                  }
         });
      }
   }

   private String currentAppPath()
   {
      if (params_ != null)
         return params_.getPath();
      return null;
   }
   
   private void notifyShinyAppDisconnected(JavaScriptObject params)
   {
      ShinyApplicationParams appState = params.cast();
      if (params_ == null)
         return;
      
      // remember that this URL is disconnecting (so we don't interrupt R when
      // the window is torn down)
      disconnectingUrl_ = appState.getUrl();
   }

   private void notifyShinyAppClosed(final JavaScriptObject params)
   {
      // if we don't know that an app is running, ignore this event
      if (params_ == null)
         return;

      satelliteClosePath_ = params_.getPath();
      
      // wait for confirmation of window closure (could be a reload)
      WindowCloseMonitor.monitorSatelliteClosure(
            ShinyApplicationSatellite.NAME, new Command()
      {
         @Override
         public void execute()
         {
            // satellite closed for real; shut down the app
            satelliteClosePath_ = null;
            onShinyApplicationClosed(params);
         }
      }, 
      new Command() 
      {
         @Override
         public void execute()
         {
            // satellite didn't actually close (it was a reload)
            satelliteClosePath_ = null;
         }
      });
   }
   
   private void onShinyApplicationClosed(JavaScriptObject params)
   {
      ShinyApplicationParams appState = params.cast();

      // this completes any pending disconnection
      disconnectingUrl_ = null;
      
      // if we were asked not to stop when the window closes (i.e. when 
      // changing viewer types), bail out
      if (!stopOnNextClose_)
      {
         stopOnNextClose_ = true;
         return;
      }
      
      // If the application is stopping, then the user initiated the stop by
      // closing the app window. Interrupt R to stop the Shiny app.
      if (appState.getState().equals(ShinyApplicationParams.STATE_STOPPING))
      {
         if (commands_.interruptR().isEnabled()) 
            commands_.interruptR().execute();
         appState.setState(ShinyApplicationParams.STATE_STOPPED);
      }
      eventBus_.fireEvent(new ShinyApplicationStatusEvent(
            (ShinyApplicationParams) params.cast()));
   }
   
   private final native void exportShinyAppClosedCallback()/*-{
      var registry = this;     
      $wnd.notifyShinyAppClosed = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.shiny.ShinyApplication::notifyShinyAppClosed(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
      $wnd.notifyShinyAppDisconnected = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.shiny.ShinyApplication::notifyShinyAppDisconnected(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
   }-*/;

   private void setShinyViewerType(int viewerType)
   {
      UIPrefs prefs = pPrefs_.get();
      prefs.shinyViewerType().setGlobalValue(viewerType);
      prefs.writeUIPrefs();

      // if we have a running Shiny app and the viewer type has changed, 
      // snap the app into the new location
      if (currentViewType_ != viewerType &&
          params_ != null)
      {
         // if transitioning away from the pane or the window, close down
         // the old instance
         if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_PANE ||
             currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW) 
         {
            if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW)
            {
               stopOnNextClose_ = false;
               satelliteManager_.closeSatelliteWindow(
                  ShinyApplicationSatellite.NAME);
            }
            else
            {
               eventBus_.fireEvent(new ViewerClearedEvent(false));
            }
         }
         
         // assign new viewer type
         currentViewType_ = viewerType;
         params_.setViewerType(viewerType);
         
         if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_PANE ||
             currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW ||
             currentViewType_ == ShinyViewerType.SHINY_VIEWER_BROWSER)
         {
            eventBus_.fireEvent(new ShinyApplicationStatusEvent(params_));
         }
      }
   }
   
   private void launchShinyFile(String file, String extendedType)
   {
      server_.getShinyRunCmd(file, 
            extendedType,
            new ServerRequestCallback<ShinyRunCmd>()
            {
               @Override
               public void onResponseReceived(ShinyRunCmd cmd)
               {
                  eventBus_.fireEvent(
                        new SendToConsoleEvent(cmd.getRunCmd(), true));
               }
   
               @Override
               public void onError(ServerError error)
               {
                  display_.showErrorMessage("Shiny App Launch Failed", 
                                            error.getMessage());
               }
            });
   }
   
   private void activateWindow()
   {
      activateWindow(null);
   }
   
   private void activateWindow(ShinyApplicationParams params)
   {
      WindowEx win = satelliteManager_.getSatelliteWindowObject(
            ShinyApplicationSatellite.NAME);
      boolean isRefresh = win != null && 
            (params == null || (params_ != null &&
                                params.getPath().equals(params_.getPath())));
      boolean isChrome = !Desktop.isDesktop() && BrowseCap.isChrome();
      if (params != null)
         params_ = params;
      if (win == null || (!isRefresh && !isChrome))
      {
         // If there's no window yet, or we're switching apps in a browser
         // other than Chrome, do a normal open
         satelliteManager_.openSatellite(ShinyApplicationSatellite.NAME,     
                                         params_, new Size(960,1100));   
      } 
      else if (isChrome)
      {
         // we have a window and we're Chrome, so force a close and reopen
         satelliteManager_.forceReopenSatellite(ShinyApplicationSatellite.NAME, 
                                                params_,
                                                true);
      }
      else
      {
         satelliteManager_.activateSatelliteWindow(
               ShinyApplicationSatellite.NAME);
      }
   }
   
   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;
   private final DependencyManager dependencyManager_;
   private final Commands commands_;
   private final Provider<UIPrefs> pPrefs_;
   private final ShinyServerOperations server_;
   private final GlobalDisplay display_;
   private final ApplicationInterrupt interrupt_;

   private ShinyApplicationParams params_;
   private String disconnectingUrl_;
   private boolean isBusy_;
   private boolean stopOnNextClose_ = true;
   private String satelliteClosePath_ = null;
   private int currentViewType_;
}
