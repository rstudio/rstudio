/*
 * ChunkSatelliteWindowRegisteredEvent.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChunkSatelliteWindowRegisteredEvent extends GwtEvent<ChunkSatelliteWindowRegisteredEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteWindowRegistered(ChunkSatelliteWindowRegisteredEvent event);
   }

   public ChunkSatelliteWindowRegisteredEvent()
   {
   }

   public ChunkSatelliteWindowRegisteredEvent(
      String docId,
      String chunkId)
   {
      docId_ = docId;
      chunkId_ = chunkId;
   }

   public String getDocId()
   {
      return docId_;
   }

   public String getChunkId()
   {
      return chunkId_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteWindowRegistered(this);
   }

   private String docId_;
   private String chunkId_;

   public static final Type<Handler> TYPE = new Type<>();
}
