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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApplication implements ShinyApplicationStatusEvent.Handler
{
   @Inject
   public ShinyApplication(EventBus eventBus, 
                           final SatelliteManager satelliteManager)
   {
      eventBus_ = eventBus;
      eventBus_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      satelliteManager_ = satelliteManager;
      
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

   public void launchShinyApplication(String filePath)
   {
      String dir = filePath.substring(0, filePath.lastIndexOf("/"));
      if (dir.equals(currentAppFilePath_))
      {
         satelliteManager_.activateSatelliteWindow(ShinyApplicationSatellite.NAME);
      }
      else
      {
         eventBus_.fireEvent(new SendToConsoleEvent(
               "shiny::runApp('" + dir + "')", 
               true));
      }
   }

   private void notifyShinyAppClosed(JavaScriptObject params)
   {
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

   private final EventBus eventBus_;
   private final SatelliteManager satelliteManager_;

   private String currentAppFilePath_;
}
