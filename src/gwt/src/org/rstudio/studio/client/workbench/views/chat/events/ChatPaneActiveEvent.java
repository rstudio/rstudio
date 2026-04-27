/*
 * ChatPaneActiveEvent.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChatPaneActiveEvent extends GwtEvent<ChatPaneActiveEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChatPaneActive(ChatPaneActiveEvent event);
   }

   public ChatPaneActiveEvent(boolean active)
   {
      active_ = active;
   }

   public boolean isActive()
   {
      return active_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChatPaneActive(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final boolean active_;
}
