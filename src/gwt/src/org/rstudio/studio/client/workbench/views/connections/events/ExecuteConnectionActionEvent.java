/*
 * ExecuteConnectionActionEvent.java
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
package org.rstudio.studio.client.workbench.views.connections.events;

import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ExecuteConnectionActionEvent 
             extends GwtEvent<ExecuteConnectionActionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onExecuteConnectionAction(ExecuteConnectionActionEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   public ExecuteConnectionActionEvent(ConnectionId id, String action)
   {
      connectionId_ = id;
      action_ = action;
   }
   
   public String getAction() 
   {
      return action_;
   }
   
   public ConnectionId getConnectionId() 
   {
      return connectionId_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onExecuteConnectionAction(this);
   }

   private final ConnectionId connectionId_;
   private final String action_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
