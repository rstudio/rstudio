/*
 * ChunkWindowManager.java
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

import org.rstudio.core.client.*;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCacheEditorStyleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCloseWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCodeExecutingEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteOpenWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowOpenedEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ChunkWindowManager
   implements ChunkSatelliteOpenWindowEvent.Handler,
              ChunkSatelliteCloseWindowEvent.Handler,
              ChunkSatelliteWindowOpenedEvent.Handler,
              ChunkSatelliteCodeExecutingEvent.Handler,
              ChunkSatelliteCacheEditorStyleEvent.Handler
{
   @Inject
   public ChunkWindowManager(
      Provider<SatelliteManager> pSatelliteManager,
      EventBus events,
      SatelliteManager satelliteManager
   )
   {
      pSatelliteManager_ = pSatelliteManager;
      events_ = events;
      
      events_.addHandler(ChunkSatelliteOpenWindowEvent.TYPE, this);
      events_.addHandler(ChunkSatelliteCloseWindowEvent.TYPE, this);
      events_.addHandler(ChunkSatelliteWindowOpenedEvent.TYPE, this);
   }

   public void openChunkWindow(String docId, String chunkId, Size sourceSize)
   {
      Size size = ZoomUtils.getZoomWindowSize(sourceSize, null);
      
      events_.fireEvent(new ChunkSatelliteOpenWindowEvent(docId, chunkId, size));
   }

   @Override
   public void onChunkSatelliteOpenWindow(ChunkSatelliteOpenWindowEvent event)
   {
      pSatelliteManager_.get().openSatellite(
         getName(event.getDocId(), event.getChunkId()), 
         ChunkWindowParams.create(event.getDocId(), event.getChunkId()), 
         event.getSize());
   }
   
   @Override
   public void onChunkSatelliteCloseWindow(ChunkSatelliteCloseWindowEvent event)
   {
      String windowName = getName(event.getDocId(), event.getChunkId());
      
      if (pSatelliteManager_.get().satelliteWindowExists(windowName))
      {
         WindowEx satelliteWindow = pSatelliteManager_.get().getSatelliteWindowObject(windowName);     
         satelliteWindow.close();
      }
   }

   @Override
   public void onChunkSatelliteWindowOpened(ChunkSatelliteWindowOpenedEvent event)
   {
      events_.fireEventToAllSatellites(event);
   }

   @Override
   public void onChunkSatelliteCacheEditorStyle(ChunkSatelliteCacheEditorStyleEvent event)
   {
      events_.fireEventToAllSatellites(event);
   }

   @Override
   public void onChunkSatelliteCodeExecuting(ChunkSatelliteCodeExecutingEvent event)
   {
      events_.fireEventToAllSatellites(event);
   }
   
   public String getName(String docId, String chunkId)
   {
      return ChunkSatellite.NAME_PREFIX + docId + "_" + chunkId;
   }
   
   private final Provider<SatelliteManager> pSatelliteManager_;
   private final EventBus events_;
}
