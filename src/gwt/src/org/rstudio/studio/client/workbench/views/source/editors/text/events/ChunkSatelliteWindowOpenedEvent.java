/*
 * ChunkSatelliteWindowOpenedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ChunkSatelliteWindowOpenedEvent
             extends CrossWindowEvent<ChunkSatelliteWindowOpenedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteWindowOpened(ChunkSatelliteWindowOpenedEvent event);
   }

   public ChunkSatelliteWindowOpenedEvent()
   {
   }

   public ChunkSatelliteWindowOpenedEvent(
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
      handler.onChunkSatelliteWindowOpened(this);
   }

   @Override
   public boolean forward()
   {
      return true;
   }

   private String docId_;
   private String chunkId_;

   public static final Type<Handler> TYPE = new Type<>();
}
