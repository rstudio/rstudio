/*
 * ShinyApplication.java
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
package org.rstudio.studio.client.shiny;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.StringUtil;
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
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.events.LaunchShinyApplicationEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyViewerOptions;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;
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
                           Provider<UserPrefs> pPrefs,
                           final SatelliteManager satelliteManager, 
                           ShinyServerOperations server,
                           JobsServerOperations jobsServer,
                           GlobalDisplay display,
                           DependencyManager dependencyManager,
                           ApplicationInterrupt interrupt)
   {
      eventBus_ = eventBus;
      satelliteManager_ = satelliteManager;
      commands_ = commands;
      pPrefs_ = pPrefs;
      server_ = server;
      jobsServer_ = jobsServer;
      display_ = display;
      isBusy_ = false;
      currentViewType_ = UserPrefs.SHINY_VIEWER_TYPE_NONE;
      dependencyManager_ = dependencyManager;
      interrupt_ = interrupt;
      params_ = new ArrayList<ShinyApplicationParams>();
      
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
         if (currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
         {
            activateWindow(event.getParams());
         }
         else if (currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_BROWSER)
         {
            display_.openWindow(event.getParams().getUrl());
         }
         params_.add(event.getParams());
         
         // if the app was started from the same path as a pending satellite
         // closure, don't shut down the app when the close finishes
         if (event.getParams().getPath() == satelliteClosePath_)
         {
            stopOnNextClose_ = false;
         }
      }
      else if (event.getParams().getState() == ShinyApplicationParams.STATE_STOPPED)
      {
         params_.removeIf(params -> params.getId() == event.getParams().getId());
      }
   }

   @Override
   public void onLaunchShinyApplication(LaunchShinyApplicationEvent event)
   {
      launchShinyApplication(event.getPath(), event.getDestination(), event.getExtendedType());
   }

   @Override
   public void onConsoleBusy(ConsoleBusyEvent event)
   {
      isBusy_ = event.isBusy();
      
      // if the browser is up and R stops being busy, presume it's because the
      // app has stopped
      if (isBusy_)
         return;
      
      for (ShinyApplicationParams params: params_)
      {
         if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_BROWSER)
         {
            params.setState(ShinyApplicationParams.STATE_STOPPED);
            eventBus_.fireEvent(new ShinyApplicationStatusEvent(params));
         }
      }
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // When leaving debug mode while the Shiny application is open in a 
      // browser, automatically return to the app by activating the window.
      if (event.debugging())
         return;
      
      for (ShinyApplicationParams params: params_)
      {
         if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
         {
            satelliteManager_.activateSatelliteWindow(
                  ShinyApplicationSatellite.NAME_PREFIX + params.getId());
         }
      }
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // Close all the satellite windows when R restarts, since this leads to
      // the Shiny server being terminated. Closing the window triggers a 
      // ShinyApplicationStatusEvent that allows the rest of the UI a chance to
      // react to the app's termination.
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED)
      {
         for (ShinyApplicationParams params: params_)
         {
            if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
            {
               satelliteManager_.closeSatelliteWindow(
                     ShinyApplicationSatellite.getNameFromId(params.getId()));
            }
         }
      }
   }

   @Override
   public void onWindowClosed(WindowClosedEvent event)
   {
      // we get this event on the desktop (currently only Cocoa); it lets us
      // know that the satellite has been shut down even in the case where the
      // script window that ordinarily would let us know has been disconnected.
      if (event.getName().startsWith(ShinyApplicationSatellite.NAME_PREFIX))
         return;

      // stop the app if this event wasn't generated by a disconnect
      if (disconnectingUrl_ == null && stopOnNextClose_)
      {
         String id = ShinyApplicationSatellite.getIdFromName(event.getName());
         for (ShinyApplicationParams params: params_)
         {
            if (StringUtil.equals(id, params.getId()))
            {
               params.setState(ShinyApplicationParams.STATE_STOPPING);
               notifyShinyAppClosed(params);
               break;
            }
         }
      }
   }

   // Command handlers --------------------------------------------------------

   @Handler
   public void onShinyRunInPane()
   {
      setShinyViewerType(UserPrefs.SHINY_VIEWER_TYPE_PANE);
   }
   
   @Handler
   public void onShinyRunInViewer()
   {
      setShinyViewerType(UserPrefs.SHINY_VIEWER_TYPE_WINDOW);
   }

   @Handler
   public void onShinyRunInBrowser()
   {
      setShinyViewerType(UserPrefs.SHINY_VIEWER_TYPE_BROWSER);
   }
   
   // Public methods ----------------------------------------------------------

   // Private methods ---------------------------------------------------------
   
   private void launchShinyApplication(final String filePath, 
         final String destination,
         final String extendedType)
   {
      String fileDir = filePath.substring(0, filePath.lastIndexOf("/"));
      for (ShinyApplicationParams params: params_)
      {
         if (StringUtil.equals(fileDir, params.getPath()))
         {
            // The app being launched is the one already running; open and
            // reload the app.
            if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
            {
               satelliteManager_.dispatchCommand(commands_.reloadShinyApp(), 
                     ShinyApplicationSatellite.getNameFromId(params.getId()));
               activateWindow(params);
            } 
            else if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_PANE &&
                     commands_.viewerRefresh().isEnabled())
            {
               commands_.viewerRefresh().execute();
            }
            else if (params.getViewerType() == UserPrefs.SHINY_VIEWER_TYPE_BROWSER)
            {
               eventBus_.fireEvent(new ShinyApplicationStatusEvent(params));
            }
            return;
         }

         if (StringUtil.equals(destination, FOREGROUND_APP) && isBusy_ && 
               params.getId() == ShinyApplicationParams.ID_FOREGROUND)
         {
            // There's another app running in the main session. Interrupt it and
            // then start this one.
            interrupt_.interruptR(new InterruptHandler() {
               @Override
               public void onInterruptFinished()
               {
                  launchShinyFile(filePath, destination, extendedType);
               }
            });
            return;
         }
      }
      
      // Nothing else running, start this app.
      dependencyManager_.withShiny("Running Shiny applications",
            new Command() {
               @Override
               public void execute()
               {
                  launchShinyFile(filePath, destination, extendedType);
               }
         });
   }

   private void notifyShinyAppDisconnected(JavaScriptObject params)
   {
      ShinyApplicationParams appState = params.cast();
      if (params_ == null || params_.isEmpty())
         return;
      
      // remember that this URL is disconnecting (so we don't interrupt R when
      // the window is torn down)
      disconnectingUrl_ = appState.getUrl();
   }

   private void notifyShinyAppClosed(final JavaScriptObject shinyParams)
   {
      ShinyApplicationParams params = shinyParams.cast();
      
      satelliteClosePath_ = params.getPath();
      
      // wait for confirmation of window closure (could be a reload)
      WindowCloseMonitor.monitorSatelliteClosure(
            ShinyApplicationSatellite.getNameFromId(params.getId()),
      () ->
      {
         // satellite closed for real; shut down the app
         satelliteClosePath_ = null;
         onShinyApplicationClosed(params);
      }, 
      () -> 
      {
         // satellite didn't actually close (it was a reload)
         satelliteClosePath_ = null;
      });
   }
   
   /**
    * Returns the parameters of the Shiny application running in the foreground,
    * if any
    */
   private ShinyApplicationParams foregroundAppParams()
   {
      for (ShinyApplicationParams params: params_)
      {
         if (params.getId() == ShinyApplicationParams.ID_FOREGROUND)
            return params;
      }
      
      return null;
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
      if (appState.getState() == ShinyApplicationParams.STATE_STOPPING)
      {
         if (appState.getId() == ShinyApplicationParams.ID_FOREGROUND)
         {
            // Foreground apps: interrupt R itself
            if (commands_.interruptR().isEnabled()) 
               commands_.interruptR().execute();
         }
         else
         {
            // Background apps: stop the associated job
            jobsServer_.executeJobAction(appState.getId(), "stop", 
                  new VoidServerRequestCallback()
            {
               @Override
               public void onError(ServerError error)
               {
                  display_.showErrorMessage("Failed to Stop", 
                        "Could not stop the Shiny application.\n\n" + 
                        error.getMessage());
               }
            });
         }
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

   private void setShinyViewerType(String viewerType)
   {
      UserPrefs prefs = pPrefs_.get();
      prefs.shinyViewerType().setGlobalValue(viewerType);
      prefs.writeUserPrefs();
      
      ShinyApplicationParams params = foregroundAppParams();
      
      // if we have a running Shiny foreground app and the viewer type has
      // changed, snap the app into the new location
      if (currentViewType_ != viewerType &&
          params != null)
      {
         // if transitioning away from the pane or the window, close down
         // the old instance
         if (currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_PANE ||
             currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_WINDOW) 
         {
            if (currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_WINDOW)
            {
               stopOnNextClose_ = false;
               satelliteManager_.closeSatelliteWindow(
                  ShinyApplicationSatellite.getNameFromId(params.getId()));
            }
            else
            {
               eventBus_.fireEvent(new ViewerClearedEvent(false));
            }
         }
         
         // assign new viewer type
         currentViewType_ = viewerType;
         params.setViewerType(viewerType);
         
         if (currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_PANE ||
             currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_WINDOW ||
             currentViewType_ == UserPrefs.SHINY_VIEWER_TYPE_BROWSER)
         {
            eventBus_.fireEvent(new ShinyApplicationStatusEvent(params));
         }
      }
   }
   
   private void launchShinyFile(String file, String destination, String extendedType)
   {
      if (StringUtil.equals(destination, FOREGROUND_APP))
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
      else if (StringUtil.equals(destination, BACKGROUND_APP))
      {
         server_.runShinyBackgroundApp(file, extendedType, 
               new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String id)
                  {

                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     display_.showErrorMessage("Shiny App Background Launch Failed", 
                                               error.getMessage());
                  }
               });
      }
   }
   
   private void activateWindow(ShinyApplicationParams params)
   {
      WindowEx win = null;
      if (params != null)
      {
         win = satelliteManager_.getSatelliteWindowObject(
            ShinyApplicationSatellite.getNameFromId(params.getId()));
      }

      boolean isChrome = !Desktop.isDesktop() && BrowseCap.isChrome();
      
      // if we haven't seen these params before, load them
      boolean newParams = true;
      if (params != null)
      {
         for (ShinyApplicationParams p: params_)
         {
            if (p.equals(params))
            {
               newParams = false;
               break;
            }
         }
         
         if (newParams)
            params_.add(params);
      }

      boolean isRefresh = win != null && 
            (params == null || !newParams);
      
      if (win == null || (!isRefresh && !isChrome))
      {
         int width = 910;
         if ((params.getViewerOptions() & ShinyViewerOptions.SHINY_VIEWER_OPTIONS_WIDE) > 0) {
            width += 300;
         }
         // If there's no window yet, or we're switching apps in a browser
         // other than Chrome, do a normal open
         satelliteManager_.openSatellite(
               ShinyApplicationSatellite.getNameFromId(params.getId()),
               params, new Size(width, 1100));
      } 
      else if (isChrome)
      {
         // we have a window and we're Chrome, so force a close and reopen
         satelliteManager_.forceReopenSatellite(
               ShinyApplicationSatellite.getNameFromId(params.getId()), 
               params,
               true);
      }
      else
      {
         satelliteManager_.activateSatelliteWindow(
               ShinyApplicationSatellite.getNameFromId(params.getId()));
      }
   }
   
   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;
   private final DependencyManager dependencyManager_;
   private final Commands commands_;
   private final Provider<UserPrefs> pPrefs_;
   private final ShinyServerOperations server_;
   private final JobsServerOperations jobsServer_;
   private final GlobalDisplay display_;
   private final ApplicationInterrupt interrupt_;
   private final List<ShinyApplicationParams> params_;

   private String disconnectingUrl_;
   private boolean isBusy_;
   private boolean stopOnNextClose_ = true;
   private String satelliteClosePath_ = null;
   private String currentViewType_;
   
   public static final String FOREGROUND_APP = "foreground";
   public static final String BACKGROUND_APP = "background";
}
