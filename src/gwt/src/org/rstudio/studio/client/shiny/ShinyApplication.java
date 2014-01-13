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

import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.ApplicationInterrupt.InterruptHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyRunCmd;
import org.rstudio.studio.client.shiny.model.ShinyViewerType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ShinyApplication implements ShinyApplicationStatusEvent.Handler,
                                         BusyHandler
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
                           ApplicationInterrupt interrupt)
   {
      eventBus_ = eventBus;
      satelliteManager_ = satelliteManager;
      commands_ = commands;
      pPrefs_ = pPrefs;
      server_ = server;
      display_ = display;
      isBusy_ = false;
      interrupt_ = interrupt;
      
      eventBus_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      eventBus_.addHandler(BusyEvent.TYPE, this);

      binder.bind(commands, this);
      exportShinyAppClosedCallback();
   }
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      if (event.getParams().getState() == ShinyApplicationParams.STATE_STARTED)
      {
         // open the window 
         satelliteManager_.openSatellite(ShinyApplicationSatellite.NAME,     
                                        event.getParams(),
                                        new Size(960,1100));   
         currentAppFilePath_ = event.getParams().getPath();
      }
      else if (event.getParams().getState() == ShinyApplicationParams.STATE_STOPPED)
      {
         currentAppFilePath_ = null;
      }
   }

   @Override
   public void onBusy(BusyEvent event)
   {
      isBusy_ = event.isBusy();
   }

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
   
   public void launchShinyApplication(String filePath)
   {
      final String dir = filePath.substring(0, filePath.lastIndexOf("/"));
      if (dir.equals(currentAppFilePath_))
      {
         // The app being launched is the one already running; open and
         // reload the app.
         satelliteManager_.dispatchCommand(commands_.reloadShinyApp());
         satelliteManager_.activateSatelliteWindow(ShinyApplicationSatellite.NAME);
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
         launchShinyAppDir(dir);
      }
   }

   private void notifyShinyAppClosed(JavaScriptObject params)
   {
      ShinyApplicationParams appState = params.cast();
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
   
   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;
   private final Commands commands_;
   private final Provider<UIPrefs> pPrefs_;
   private final ShinyServerOperations server_;
   private final GlobalDisplay display_;
   private final ApplicationInterrupt interrupt_;

   private String currentAppFilePath_;
   private boolean isBusy_;
}
