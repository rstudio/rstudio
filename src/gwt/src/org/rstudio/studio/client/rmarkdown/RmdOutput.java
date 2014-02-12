/*
 * RmdOutput.java
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
package org.rstudio.studio.client.rmarkdown;

import java.util.Map;
import java.util.HashMap;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RmdOutput implements RmdRenderCompletedEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, RmdOutput> {}

   @Inject
   public RmdOutput(EventBus eventBus, 
                    Commands commands,
                    Binder binder,
                    final SatelliteManager satelliteManager)
   {
      satelliteManager_ = satelliteManager;
      
      eventBus.addHandler(RmdRenderCompletedEvent.TYPE, this);

      binder.bind(commands, this);
      
      exportRmdOutputClosedCallback();
   }
   
   @Override
   public void onRmdRenderCompleted(RmdRenderCompletedEvent event)
   {
      RmdRenderResult result = event.getResult();
      if (result.getSucceeded())
      {
         // find the last known scroll position for this file
         int scrollPosition = 0;
         if (scrollPositions_.containsKey(result.getOutputFile()))
         {
            scrollPosition = scrollPositions_.get(result.getOutputFile());
         }
         RmdPreviewParams params = RmdPreviewParams.create(
               result, scrollPosition);

         WindowEx win = satelliteManager_.getSatelliteWindowObject(
               RmdOutputSatellite.NAME);

         // we're refreshing if the window is up and we're pulling the same
         // output file as the last one
         boolean isRefresh = win != null &&
                             result_ != null && 
                             result_.getOutputFile().equals(
                                   result.getOutputFile());
         // if this isn't a refresh but there's a window up, cache the scroll
         // position of the old document before we replace it
         if (!isRefresh && result_ != null && win != null)
         {
            scrollPositions_.put(result_.getOutputFile(), 
                                 getScrollPosition(win));
         }
         if (win != null && !Desktop.isDesktop() && BrowseCap.isChrome())
         {
            // we're on Chrome, cache the scroll position unless we're switching
            // docs and do a hard close/reopen
            if (isRefresh)
            {
               params.setScrollPosition(getScrollPosition(win));
            }
            satelliteManager_.forceReopenSatellite(RmdOutputSatellite.NAME, 
                                                   params);
         }
         else
         {
            satelliteManager_.openSatellite(RmdOutputSatellite.NAME,     
                                            params,
                                            new Size(960,1100));   
         }
      }

      // save the result so we know if the next render is a re-render of the
      // same document
      result_ = result;
   }
 
   private final native void exportRmdOutputClosedCallback()/*-{
      var registry = this;     
      $wnd.notifyRmdOutputClosed = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.rmarkdown.RmdOutput::notifyRmdOutputClosed(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
   }-*/;
   
   private final native int getScrollPosition(JavaScriptObject win) /*-{
      return win.getRstudioFrameScrollPosition();
   }-*/;
   
   // when the window is closed, remember our position within it
   private void notifyRmdOutputClosed(JavaScriptObject closeParams)
   {
      RmdPreviewParams params = closeParams.cast();
      scrollPositions_.put(params.getOutputFile(), params.getScrollPosition());
   }

   private final SatelliteManager satelliteManager_;

   // stores the last scroll position of each document we know about: map
   // of path to position
   private final Map<String, Integer> scrollPositions_ = 
         new HashMap<String, Integer>();
   private RmdRenderResult result_;
}