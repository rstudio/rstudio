/*
 * ShinyApplication.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;

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
                                         WindowClosedEvent.Handler
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
         currentAppFilePath_ = event.getParams().getPath();
      }
      else if (event.getParams().getState() == ShinyApplicationParams.STATE_STOPPED)
      {
         currentAppFilePath_ = null;
      }
   }

   @Override
   public void onConsoleBusy(ConsoleBusyEvent event)
   {
      isBusy_ = event.isBusy();
   }
   
   @Override
   public void onDebugModeChanged(DebugModeChangedEvent event)
   {
      // When leaving debug mode while the Shiny application is open in a 
      // browser, automatically return to the app by activating the window.
      if (!event.debugging() && 
          currentAppFilePath_ != null &&
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
      if (params_ != null)
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

   public void launchShinyApplication(String filePath)
   {
      final String dir = filePath.substring(0, filePath.lastIndexOf("/"));
      if (dir.equals(currentAppFilePath_))
      {
         // The app being launched is the one already running; open and
         // reload the app.
         if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_WINDOW)
         {
            satelliteManager_.dispatchCommand(commands_.reloadShinyApp());
            activateWindow();
         } 
         else if (currentViewType_ == ShinyViewerType.SHINY_VIEWER_PANE &&
                  commands_.viewerRefresh().isEnabled())
         {
            commands_.viewerRefresh().execute();
         }
         return;
      }
      else if (currentAppFilePath_ != null && isBusy_)
      {
         // There's another app running. Interrupt it and then start this one.
         interrupt_.interruptR(new InterruptHandler() {
            @Override
            public void onInterruptFinished()
            {
               launchShinyAppDir(dir);
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
                     launchShinyAppDir(dir);
                  }
         });
      }
   }

   // Private methods ---------------------------------------------------------

   private void notifyShinyAppClosed(JavaScriptObject params)
   {
      ShinyApplicationParams appState = params.cast();
      
      // if we don't know that an app is running, ignore this event
      if (currentAppFilePath_ == null)
         return;
      
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
   }-*/;

   private void setShinyViewerType(int viewerType)
   {
      UIPrefs prefs = pPrefs_.get();
      prefs.shinyViewerType().setGlobalValue(viewerType);
      prefs.writeUIPrefs();
      server_.setShinyViewerType(viewerType, new VoidServerRequestCallback());
   }
   
   private void launchShinyAppDir(String dir)
   {
      server_.getShinyRunCmd(dir, 
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
                                                params_);
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
   private String currentAppFilePath_;
   private boolean isBusy_;
   private int currentViewType_;
}
