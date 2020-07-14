/*
 * ConnectionListChangedEvent.java
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

import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ConnectionListChangedEvent extends GwtEvent<ConnectionListChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onConnectionListChanged(ConnectionListChangedEvent event);
   }

   public ConnectionListChangedEvent(JsArray<Connection> connectionList)
   {
      connectionList_ = connectionList;
   }

   public JsArray<Connection> getConnectionList()
   {
      return connectionList_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConnectionListChanged(this);
   }

   private final JsArray<Connection> connectionList_;

   public static final Type<Handler> TYPE = new Type<>();
}
