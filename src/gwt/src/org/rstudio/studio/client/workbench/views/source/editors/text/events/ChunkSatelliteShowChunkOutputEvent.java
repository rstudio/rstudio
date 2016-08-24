/*
 * ChunkSatelliteShowChunkOutputEvent.java
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
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ChunkSatelliteShowChunkOutputEvent 
             extends CrossWindowEvent<ChunkSatelliteShowChunkOutputEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteShowChunkOutput(ChunkSatelliteShowChunkOutputEvent event);
   }

   public ChunkSatelliteShowChunkOutputEvent()
   {
   }
   
   public ChunkSatelliteShowChunkOutputEvent(
      RmdChunkOutput rmdChunkOutput,
      int mode,
      int scope,
      boolean complete
   )
   {
      rmdChunkOutput_ = rmdChunkOutput;
      mode_ = mode;
      scope_ = scope;
      complete_ = complete;
   }
   
   public RmdChunkOutput getOutput()
   {
      return rmdChunkOutput_;
   }
   
   public int getMode()
   {
      return mode_;
   }
   
   public int getScope()
   {
      return scope_;
   }
   
   public Boolean getComplete()
   {
      return complete_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteShowChunkOutput(this);
   }
   
   private RmdChunkOutput rmdChunkOutput_;
   private int mode_;
   private int scope_;
   private boolean complete_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}