/*
 * ChunkSatelliteCacheEditorStyleEvent.java
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
public class ChunkSatelliteCacheEditorStyleEvent 
             extends CrossWindowEvent<ChunkSatelliteCacheEditorStyleEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onChunkSatelliteCacheEditorStyle(ChunkSatelliteCacheEditorStyleEvent event);
   }

   public ChunkSatelliteCacheEditorStyleEvent()
   {
   }

   public ChunkSatelliteCacheEditorStyleEvent(
      String foregroundColor,
      String backgroundColor,
      String aceEditorColor
   )
   {
      foregroundColor_ = foregroundColor;
      backgroundColor_ = backgroundColor;
      aceEditorColor_ = aceEditorColor;
   }
   
   public String getBackgroundColor()
   {
      return backgroundColor_;
   }
   
   public String getForegroundColor()
   {
      return foregroundColor_;
   }
   
   public String getAceEditorColor()
   {
      return aceEditorColor_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChunkSatelliteCacheEditorStyle(this);
   }

   private String foregroundColor_;
   private String backgroundColor_;
   private String aceEditorColor_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}