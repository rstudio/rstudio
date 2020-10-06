/*
 * RPubsUploadStatusEvent.java
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
package org.rstudio.studio.client.common.rpubs.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RPubsUploadStatusEvent extends GwtEvent<RPubsUploadStatusEvent.Handler>
{
   public static class Status extends JavaScriptObject
   {
      protected Status()
      {
      }

      public final native String getContextId() /*-{
         return this.contextId;
      }-*/;

      public final native String getId() /*-{
         return this.id;
      }-*/;

      public final native String getContinueUrl() /*-{
         return this.continueUrl;
      }-*/;

      public final native String getError() /*-{
         return this.error;
      }-*/;

   }

   public interface Handler extends EventHandler
   {
      void onRPubsPublishStatus(RPubsUploadStatusEvent event);
   }

   public RPubsUploadStatusEvent(Status status)
   {
      status_ = status;
   }

   public Status getStatus()
   {
      return status_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRPubsPublishStatus(this);
   }

   private final Status status_;

   public static final Type<Handler> TYPE = new Type<>();
}
