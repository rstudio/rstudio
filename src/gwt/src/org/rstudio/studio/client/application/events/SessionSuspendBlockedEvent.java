/*
 * SessionSerializationEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SessionSuspendBlockedEvent extends GwtEvent<SessionSuspendBlockedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native Boolean isEmpty()
      /*-{
         return Object.keys(this).length === 0;
      }-*/;

      public final native String getMsg()
      /*-{
         var msg = 'Session suspend timeout paused:';

         for (var e = 0; e < this.length; e++)
         {
             msg += '\n' + this[e];
         }

         return msg;
      }-*/;
   }
   public static final Type<Handler> TYPE = new Type<>();

   public SessionSuspendBlockedEvent(Data data)
   {
      data_ = data;
   }


   public String getMsg()
   {
      return data_.getMsg();
   }

   public Boolean isBlocked()
   {
      return !data_.isEmpty();
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSessionSuspendBlocked(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public interface Handler extends EventHandler
   {
      void onSessionSuspendBlocked(SessionSuspendBlockedEvent event);
   }
}
