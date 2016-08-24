/*
 * ChunkSatelliteWindowOpenedEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChunkSatelliteWindowOpenedEvent 
             extends GwtEvent<ChunkSatelliteWindowOpenedEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteWindowOpened(ChunkSatelliteWindowOpenedEvent event);
   }
   
   public ChunkSatelliteWindowOpenedEvent(
      String docId,
      String chunkId,
      ChunkOutputWidget chunkOutputWidget)
   {
      docId_ = docId;
      chunkId_ = chunkId;
      chunkOutputWidget_ = chunkOutputWidget;
   }

   public String getDocId()
   {
      return docId_;
   }

   public String getChunkId()
   {
      return chunkId_;
   }

   public ChunkOutputWidget getChunkOutputWidget()
   {
      return chunkOutputWidget_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteWindowOpened(this);
   }
   
   private String docId_;
   private String chunkId_;
   private ChunkOutputWidget chunkOutputWidget_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}