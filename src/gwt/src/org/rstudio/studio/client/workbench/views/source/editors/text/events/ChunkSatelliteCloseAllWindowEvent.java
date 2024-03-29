/*
 * ChunkSatelliteCloseAllWindowEvent.java
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
public class ChunkSatelliteCloseAllWindowEvent
             extends CrossWindowEvent<ChunkSatelliteCloseAllWindowEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteCloseAllWindow(ChunkSatelliteCloseAllWindowEvent event);
   }

   public ChunkSatelliteCloseAllWindowEvent()
   {
   }

   public ChunkSatelliteCloseAllWindowEvent(
      String docId)
   {
      docId_ = docId;
   }

   public String getDocId()
   {
      return docId_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteCloseAllWindow(this);
   }

   @Override
   public boolean forward()
   {
      return true;
   }

   private String docId_;

   public static final Type<Handler> TYPE = new Type<>();
}
