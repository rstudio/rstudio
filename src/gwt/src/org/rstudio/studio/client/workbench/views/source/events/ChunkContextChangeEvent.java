/*
 * ChunkContextChangeEvent.java
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

import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChunkContextChangeEvent 
             extends GwtEvent<ChunkContextChangeEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkContextChange(ChunkContextChangeEvent event);
   }

   public static final GwtEvent.Type<ChunkContextChangeEvent.Handler> TYPE =
      new GwtEvent.Type<ChunkContextChangeEvent.Handler>();
   
   public ChunkContextChangeEvent(String docId, String contextId,
         JsArray<ChunkDefinition> chunkDefs)
   {
      docId_ = docId;
      contextId_ = contextId;
      chunkDefs_ = chunkDefs;
   }
   
   public String getDocId()
   {
      return docId_;
   }

   public JsArray<ChunkDefinition> getChunkDefs()
   {
      return chunkDefs_;
   }
   
   public String getContextId()
   {
      return contextId_;
   }
   
   @Override
   protected void dispatch(ChunkContextChangeEvent.Handler handler)
   {
      handler.onChunkContextChange(this);
   }

   @Override
   public GwtEvent.Type<ChunkContextChangeEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String docId_;
   private final String contextId_;
   private final JsArray<ChunkDefinition> chunkDefs_;
}
