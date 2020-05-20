/*
 * ChunkWindowManager.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.*;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.zoom.ZoomUtils;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCacheEditorStyleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCloseAllWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteCodeExecutingEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteOpenWindowEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowOpenedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ChunkSatelliteWindowRegisteredEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ChunkWindowManager
   implements ChunkSatelliteOpenWindowEvent.Handler,
              ChunkSatelliteCloseAllWindowEvent.Handler,
              ChunkSatelliteWindowOpenedEvent.Handler,
              ChunkSatelliteCodeExecutingEvent.Handler,
              ChunkSatelliteCacheEditorStyleEvent.Handler,
              ChunkChangeEvent.Handler
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

      satelliteChunks_ = new ArrayList<Pair<String, String>>();
      
      if (!Satellite.isCurrentWindowSatellite())
      {
         events_.addHandler(ChunkSatelliteOpenWindowEvent.TYPE, this);
         events_.addHandler(ChunkSatelliteCloseAllWindowEvent.TYPE, this);
         events_.addHandler(ChunkSatelliteWindowOpenedEvent.TYPE, this);
         events_.addHandler(ChunkSatelliteCodeExecutingEvent.TYPE, this);
         events_.addHandler(ChunkSatelliteCacheEditorStyleEvent.TYPE, this);
      }

      events_.addHandler(ChunkChangeEvent.TYPE, this);
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
   public void onChunkSatelliteCloseAllWindow(ChunkSatelliteCloseAllWindowEvent event)
   {
      ArrayList<Pair<String,String>> newSatelliteChunks = new ArrayList<Pair<String,String>>();

      for (Pair<String, String> chunkPair : satelliteChunks_)
      {
         String docId = chunkPair.first;
         String chunkId = chunkPair.second;
         
         if (docId != event.getDocId()) {
            newSatelliteChunks.add(chunkPair);
            continue;
         }

         String windowName = getName(docId, chunkId);
      
         if (pSatelliteManager_.get().satelliteWindowExists(windowName))
         {
            WindowEx satelliteWindow = pSatelliteManager_.get().getSatelliteWindowObject(windowName);     
            satelliteWindow.close();
         }
      }

      satelliteChunks_ = newSatelliteChunks;
   }

   @Override
   public void onChunkSatelliteWindowOpened(ChunkSatelliteWindowOpenedEvent event)
   {
      String docId = event.getDocId();
      String chunkId = event.getChunkId();

      satelliteChunks_.add(new Pair<String, String>(docId, chunkId));
      events_.fireEventToAllSatellites(event);

      ChunkSatelliteWindowRegisteredEvent registeredEvent = new ChunkSatelliteWindowRegisteredEvent(docId, chunkId);
      events_.fireEvent(registeredEvent);
   }

   @Override
   public void onChunkSatelliteCacheEditorStyle(ChunkSatelliteCacheEditorStyleEvent event)
   {
      String docId = event.getDocId();

      forwardEventToSatelliteAllChunks(docId, event);
   }

   @Override
   public void onChunkSatelliteCodeExecuting(ChunkSatelliteCodeExecutingEvent event)
   {
      String docId = event.getDocId();
      String chunkId = event.getChunkId();

      forwardEventToSatelliteChunk(docId, chunkId, event);
   }
   
   @Override
   public void onChunkChange(ChunkChangeEvent event)
   {
      if (Satellite.isCurrentWindowSatellite())
      {
         events_.fireEventToMainWindow(event);
      }
      else
      {
         String docId = event.getDocId();
         String chunkId = event.getChunkId();

         forwardEventToSatelliteChunk(docId, chunkId, event);
      }
   }
   
   public String getName(String docId, String chunkId)
   {
      return ChunkSatellite.NAME_PREFIX + docId + "_" + chunkId;
   }

   private boolean satelliteChunkExists(String docId, String chunkId)
   {
      for (Pair<String,String> chunkPair : satelliteChunks_)
      {
         if (chunkPair.first == docId && chunkPair.second == chunkId)
            return true;
      }

      return false;
   }

   private void forwardEventToSatelliteChunk(
      String docId,
      String chunkId,
      CrossWindowEvent<?> event
   )
   {
      if (satelliteChunkExists(docId, chunkId))
      {
         String windowName = getName(docId, chunkId);
         
         if (pSatelliteManager_.get().satelliteWindowExists(windowName))
         {
            WindowEx satelliteWindow = pSatelliteManager_.get().getSatelliteWindowObject(windowName);     
            events_.fireEventToSatellite(event, satelliteWindow);
         }
      }
   }

   private void forwardEventToSatelliteAllChunks(
      String docId,
      CrossWindowEvent<?> event
   )
   {
      for (Pair<String,String> chunkPair : satelliteChunks_)
      {
         if (chunkPair.first != docId)
            continue;
         
         String chunkId = chunkPair.second;

         String windowName = getName(docId, chunkId);
         
         if (pSatelliteManager_.get().satelliteWindowExists(windowName))
         {
            WindowEx satelliteWindow = pSatelliteManager_.get().getSatelliteWindowObject(windowName);     
            events_.fireEventToSatellite(event, satelliteWindow);
         }
      }
   }
   
   private final Provider<SatelliteManager> pSatelliteManager_;
   private final EventBus events_;
   private ArrayList<Pair<String,String>> satelliteChunks_;
}
