/*
 * ChunkChangeEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class ChunkChangeEvent 
             extends CrossWindowEvent<ChunkChangeEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkChange(ChunkChangeEvent event);
   }

   public static final GwtEvent.Type<ChunkChangeEvent.Handler> TYPE =
      new GwtEvent.Type<ChunkChangeEvent.Handler>();
   
   public ChunkChangeEvent()
   {
      docId_ = null;
      chunkId_ = null;
      requestId_ = null;
      row_ = 0;
      type_ = 0;
   }

   public ChunkChangeEvent(String docId, String chunkId, String requestId,
         int row, int type)
   {
      docId_ = docId;
      chunkId_ = chunkId;
      requestId_ = requestId;
      row_ = row;
      type_ = type;
   }
   
   public String getDocId()
   {
      return docId_;
   }

   public String getChunkId()
   {
      return chunkId_;
   }
   
   public int getRow()
   {
      return row_;
   }
   
   public int getChangeType()
   {
      return type_;
   }
   
   public String getRequestId()
   {
      return requestId_;
   }
   
   @Override
   protected void dispatch(ChunkChangeEvent.Handler handler)
   {
      handler.onChunkChange(this);
   }

   @Override
   public GwtEvent.Type<ChunkChangeEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   @Override
   public boolean forward()
   {
      return false;
   }
   
   private String docId_;
   private String chunkId_;
   private String requestId_;
   private int row_;
   private int type_;
   
   public final static int CHANGE_CREATE = 0;
   public final static int CHANGE_REMOVE = 1;
}
