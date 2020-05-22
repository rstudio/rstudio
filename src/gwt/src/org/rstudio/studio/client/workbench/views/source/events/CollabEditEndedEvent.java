/*
 * CollabEditEndedEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class CollabEditEndedEvent 
             extends CrossWindowEvent<CollabEditEndedEvent.Handler>
{ 
   public static class Data extends JavaScriptObject
   {
      protected Data() 
      {
      }

      public final native static Data create(String path) /*-{
         return { "path": path };
      }-*/;
      
      public final native String getPath() /*-{
         return this.path;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onCollabEditEnded(CollabEditEndedEvent event);
   }

   public static final GwtEvent.Type<CollabEditEndedEvent.Handler> TYPE =
      new GwtEvent.Type<CollabEditEndedEvent.Handler>();

   public CollabEditEndedEvent()
   {
   }
   
   public CollabEditEndedEvent(Data data)
   {
      data_ = data;
   }
   
   public String getPath()
   {
      return data_.getPath();
   }
   
   @Override
   protected void dispatch(CollabEditEndedEvent.Handler handler)
   {
      handler.onCollabEditEnded(this);
   }

   @Override
   public GwtEvent.Type<CollabEditEndedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private Data data_;
}
