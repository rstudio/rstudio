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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteWindow;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.ChunkPlotRefreshedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.model.NotebookQueueUnit;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCacheEditorStyleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCodeExecutingEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowOpenedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputHost;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;

@Singleton
public class ChunkSatelliteWindow extends SatelliteWindow
                                  implements RequiresResize,
                                             ChunkSatelliteView,
                                             ChunkSatelliteCodeExecutingEvent.Handler,
                                             ChunkSatelliteCacheEditorStyleEvent.Handler,
                                             ChunkPlotRefreshedEvent.Handler,
                                             ChunkPlotRefreshFinishedEvent.Handler,
                                             ChunkChangeEvent.Handler,
                                             RmdChunkOutputFinishedEvent.Handler,
                                             RmdChunkOutputEvent.Handler
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

      String title = "RStudio: Notebook Output";
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
         false,  // can close
         chunkOutputHost,
         ChunkOutputSize.Full);

      Element ele = chunkOutputWidget_.getElement();
      ele.addClassName(ThemeStyles.INSTANCE.selectableText());
      
      // Append the chunkOutputWidget as an HTML element, not as a widget.
      // Why? Chunks are widgets that are attached to the ACE editor as HTML
      // elements, not as widgets. The reason being that GWT does not support
      // triggering events for widgets that are not attached to their hierarchy.
      // Therefore, if we attach this element as a widget, GWT will remove 
      // events in some cases which will cause functionality to be lost.
      mainPanel.getElement().appendChild(chunkOutputWidget_.getElement());

      chunkOutputWidget_.getElement().getStyle().setHeight(100, Unit.PCT);

      mainPanel.addStyleName("ace_editor_theme");

      pEventBus_.get().addHandler(ChunkSatelliteCodeExecutingEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkSatelliteCacheEditorStyleEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkPlotRefreshedEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkPlotRefreshFinishedEvent.TYPE, this);
      pEventBus_.get().addHandler(ChunkChangeEvent.TYPE, this);
      pEventBus_.get().addHandler(RmdChunkOutputFinishedEvent.TYPE, this);
      pEventBus_.get().addHandler(RmdChunkOutputEvent.TYPE, this);
      
      Window.addWindowClosingHandler(new ClosingHandler()
      {
         @Override
         public void onWindowClosing(ClosingEvent arg0)
         {
            server_.cleanReplayNotebookChunkPlots(
               chunkWindowParams_.getDocId(), 
               chunkWindowParams_.getChunkId(),
                  new ServerRequestCallback<Void>()
                  {
                     @Override
                     public void onError(ServerError error)
                     {
                     }
                  }
            );
         }
      });

      pEventBus_.get().fireEventToMainWindow(new ChunkSatelliteWindowOpenedEvent(
         chunkWindowParams_.getDocId(),
         chunkWindowParams_.getChunkId()));
   }
   
   @Override
   public void onChunkSatelliteCodeExecuting(ChunkSatelliteCodeExecutingEvent event)
   {
      String docId = chunkWindowParams_.getDocId();
      
      if (event.getDocId() != docId)
         return;

      chunkOutputWidget_.setCodeExecuting(
         event.getMode(),
         event.getScope());
   }

   @Override
   public void onChunkSatelliteCacheEditorStyle(ChunkSatelliteCacheEditorStyleEvent event)
   {
      String docId = chunkWindowParams_.getDocId();
      
      if (event.getDocId() != docId)
         return;

      ChunkOutputWidget.cacheEditorStyle(
         event.getForegroundColor(),
         event.getBackgroundColor(),
         event.getAceEditorColor()
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
      chunkOutputWidget_.onResize();
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

   @Override
   public void onChunkChange(ChunkChangeEvent event)
   {
      String docId = chunkWindowParams_.getDocId();
      String chunkId = chunkWindowParams_.getChunkId();
      
      if (event.getDocId() != docId || event.getChunkId() != chunkId)
         return;
      
      switch(event.getChangeType())
      {
         case ChunkChangeEvent.CHANGE_REMOVE:
            WindowEx.get().close();  
            break;
      }
   }

   @Override
   public void onRmdChunkOutputFinished(RmdChunkOutputFinishedEvent event)
   {
      if (event.getData().getDocId() != chunkWindowParams_.getDocId())
         return;

      if (event.getData().getChunkId() != chunkWindowParams_.getChunkId())
         return;

      resizePlotsRemote_.schedule(10);
      
      RmdChunkOutputFinishedEvent.Data data = event.getData();

      chunkOutputWidget_.onOutputFinished(
         false,
         data.getScope());
   }

   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      if (event.getOutput().getDocId() != chunkWindowParams_.getDocId())
         return;

      if (event.getOutput().getChunkId() != chunkWindowParams_.getChunkId())
         return;

      chunkOutputWidget_.showChunkOutput(
         event.getOutput(),
         NotebookQueueUnit.EXEC_MODE_SINGLE,
         NotebookQueueUnit.EXEC_SCOPE_PARTIAL,
         false,
         false);
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
            chunkOutputWidget_.getOffsetHeight(),
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