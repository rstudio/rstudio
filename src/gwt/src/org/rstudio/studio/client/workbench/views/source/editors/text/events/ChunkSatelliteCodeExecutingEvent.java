/*
 * ChunkSatelliteCodeExecutingEvent.java
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
      int mode, int scope
   )
   {
      mode_ = mode;
      scope_ = scope;
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

   private int mode_;
   private int scope_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}