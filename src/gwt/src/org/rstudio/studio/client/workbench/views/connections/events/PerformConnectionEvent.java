/*
 * PerformConnectionEvent.java
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
package org.rstudio.studio.client.workbench.views.connections.events;


import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PerformConnectionEvent extends GwtEvent<PerformConnectionEvent.Handler>
{
   public static class Data
   {
      public Data(String connectVia, String connectCode)
      {
         connectVia_ = connectVia;
         connectCode_ = connectCode;
      }

      public String getConnectVia()
      {
         return connectVia_;
      }

      public String getConnectCode()
      {
         return connectCode_;
      }

      private final String connectVia_;
      private final String connectCode_;
   }

   public interface Handler extends EventHandler
   {
      void onPerformConnection(PerformConnectionEvent event);
   }

   public PerformConnectionEvent(Data data)
   {
      data_ = data;
   }

   public PerformConnectionEvent(String connectVia, String connectCode)
   {
      this(new Data(connectVia, connectCode));
   }

   public String getConnectVia()
   {
      return data_.getConnectVia();
   }

   public String getConnectCode()
   {
      return data_.getConnectCode();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPerformConnection(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
