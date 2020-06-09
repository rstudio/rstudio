/*
 * ActiveConnectionsChangedEvent.java
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

import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ActiveConnectionsChangedEvent extends GwtEvent<ActiveConnectionsChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onActiveConnectionsChanged(ActiveConnectionsChangedEvent event);
   }

   public ActiveConnectionsChangedEvent(JsArray<ConnectionId> activeConnections)
   {
      activeConnections_ = activeConnections;
   }

   public JsArray<ConnectionId> getActiveConnections()
   {
      return activeConnections_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onActiveConnectionsChanged(this);
   }

   private final JsArray<ConnectionId> activeConnections_;

   public static final Type<Handler> TYPE = new Type<>();
}
