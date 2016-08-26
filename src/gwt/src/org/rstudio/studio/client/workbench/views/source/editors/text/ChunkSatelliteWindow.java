/*
 * ChunkSatelliteWindow.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCacheEditorStyleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCodeExecutingEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteOutputFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteShowChunkOutputEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowOpenedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;

@Singleton
public class ChunkSatelliteWindow extends SatelliteWindow
                                  implements RequiresResize,
                                             ChunkSatelliteView,
                                             ChunkSatelliteShowChunkOutputEvent.Handler,
                                             ChunkSatelliteCodeExecutingEvent.Handler,
                                             ChunkSatelliteOutputFinishedEvent.Handler,
                                             ChunkSatelliteCacheEditorStyleEvent.Handler,
                                             ChunkPlotRefreshedEvent.Handler,
                                             ChunkPlotRefreshFinishedEvent.Handler
{

   @Inject
   public ChunkSatelliteWindow(Provider<EventBus> pEventBus,
                               Provider<FontSizeManager> pFSManager,
                               RMarkdownServerOperations server)
   {
      super(pEventBus, pFSManager);
      
      pEventBus_ = pEventBus;
      server_ = server;
   }

   @Override
   protected void onInitialize(LayoutPanel mainPanel, JavaScriptObject params)
   {
      chunkWindowParams_ = params.cast();

      String title = "RStudio Chunk Window";
      Window.setTitle(title);

      ChunkOutputHost chunkOutputHost = new ChunkOutputHost()
      {
         @Override
         public void onOutputRemoved(final ChunkOutputWidget widget)
         {
         }
         
         @Override
         public void onOutputHeightChanged(ChunkOutputWidget widget,
                                           int height,
                                           boolean ensureVisible)
         {
         }
      };
      
      chunkOutputWidget_ = new ChunkOutputWidget(
         chunkWindowParams_.getDocId(),
         chunkWindowParams_.getChunkId(),
         RmdChunkOptions.create(),
         ChunkOutputWidget.EXPANDED,
         chunkOutputHost,
         ChunkOutputSize.Full);
      
      mainPanel.add(chunkOutputWidget_);
      mainPanel.setWidgetLeftRight(chunkOutputWidget_, 0, Unit.PX, 0, Unit.PX);
      mainPanel.setWidgetTopBottom(chunkOutputWidget_, 0, Unit.PX, 0, Unit.PX);

      mainPanel.addStyleName("ace_editor");

      pEventBus_.get().addHandler(ChunkSatelliteShowChunkOutputEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkSatelliteCodeExecutingEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkSatelliteOutputFinishedEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkSatelliteCacheEditorStyleEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkPlotRefreshedEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkPlotRefreshFinishedEvent.TYPE, this);

      pEventBus_.get().fireEventToMainWindow(new ChunkSatelliteWindowOpenedEvent(
         chunkWindowParams_.getDocId(),
         chunkWindowParams_.getChunkId()));
   }

   @Override
   public void onChunkSatelliteShowChunkOutput(ChunkSatelliteShowChunkOutputEvent event)
   {
      chunkOutputWidget_.showChunkOutput(
         event.getOutput(),
         event.getMode(),
         event.getScope(),
         event.getComplete(),
         false);
   }
   
   @Override
   public void onChunkSatelliteCodeExecuting(ChunkSatelliteCodeExecutingEvent event)
   {
      chunkOutputWidget_.setCodeExecuting(
         event.getMode(),
         event.getScope());
   }

   @Override
   public void onChunkSatelliteOutputFinished(ChunkSatelliteOutputFinishedEvent event)
   {
      chunkOutputWidget_.onOutputFinished(
         event.getEnsureVisible(),
         event.getScope());

      resizePlotsRemote_.schedule(1);
   }

   @Override
   public void onChunkSatelliteCacheEditorStyle(ChunkSatelliteCacheEditorStyleEvent event)
   {
      ChunkOutputWidget.cacheEditorStyle(
         event.getForegroundColor(),
         event.getBackgroundColor()
      );

      chunkOutputWidget_.applyCachedEditorStyle();
   }

   @Override
   public void reactivate(JavaScriptObject params)
   {
   }
   
   @Override 
   public Widget getWidget()
   {
      return this;
   }

   @Override
   public void onResize()
   {
      resizePlotsRemote_.schedule(250);
   }

   @Override
   public void onChunkPlotRefreshed(ChunkPlotRefreshedEvent event)
   {
      // ignore replays that are not targeting this instance
      if (currentPlotsReplayId_ != event.getData().getReplayId())
         return;

      // ignore if targeted at another document
      if (event.getData().getDocId() != chunkWindowParams_.getDocId())
         return;
      
      // ignore if targeted at another chunk
      if (event.getData().getChunkId() != chunkWindowParams_.getChunkId())
         return;
      
      chunkOutputWidget_.updatePlot(event.getData().getPlotUrl());
   }
   
   @Override
   public void onChunkPlotRefreshFinished(ChunkPlotRefreshFinishedEvent event)
   {
      // ignore replays that are not targeting this instance
      if (currentPlotsReplayId_ != event.getData().getReplayId())
         return;

      currentPlotsReplayId_ = null;
   }

   public ChunkOutputWidget getOutputWidget()
   {
      return chunkOutputWidget_;
   }

   private Timer resizePlotsRemote_ = new Timer()
   {
      @Override
      public void run()
      {
         // avoid reentrancy
         if (currentPlotsReplayId_ != null)
            return;
         
         server_.replayNotebookChunkPlots(
            chunkWindowParams_.getDocId(), 
            chunkWindowParams_.getChunkId(),
            chunkOutputWidget_.getOffsetWidth(),
            new ServerRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String replayId)
               {
                  currentPlotsReplayId_ = replayId;
               }

               @Override
               public void onError(ServerError error)
               {
                  currentPlotsReplayId_ = null;
                  Debug.logError(error);
               }
            }
         );
      }
   };

   private ChunkOutputWidget chunkOutputWidget_;
   private ChunkWindowParams chunkWindowParams_;
   
   private final Provider<EventBus> pEventBus_;
   private final RMarkdownServerOperations server_;

   private String currentPlotsReplayId_ = null;
}