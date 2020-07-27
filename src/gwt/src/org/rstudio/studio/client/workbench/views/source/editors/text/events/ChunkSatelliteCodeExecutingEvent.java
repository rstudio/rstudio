/*
 * ChunkSatelliteCodeExecutingEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ChunkSatelliteCodeExecutingEvent
             extends CrossWindowEvent<ChunkSatelliteCodeExecutingEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteCodeExecuting(ChunkSatelliteCodeExecutingEvent event);
   }

   public ChunkSatelliteCodeExecutingEvent()
   {
   }

   public ChunkSatelliteCodeExecutingEvent(
      String docId, String chunkId, int mode, int scope
   )
   {
      docId_ = docId;
      chunkId_ = chunkId;
      mode_ = mode;
      scope_ = scope;
   }

   public String getDocId()
   {
      return docId_;
   }

   public String getChunkId()
   {
      return chunkId_;
   }

   public int getMode()
   {
      return mode_;
   }

   public int getScope()
   {
      return scope_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteCodeExecuting(this);
   }

   @Override
   public boolean forward()
   {
      return true;
   }

   private String docId_;
   private String chunkId_;
   private int mode_;
   private int scope_;

   public static final Type<Handler> TYPE = new Type<>();
}
