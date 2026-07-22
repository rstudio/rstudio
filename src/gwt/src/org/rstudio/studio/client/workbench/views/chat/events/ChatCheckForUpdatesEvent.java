/*
 * ChatCheckForUpdatesEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

/**
 * Fired when Posit Assistant asks RStudio to check for Posit Assistant updates
 * via the {@code ui/checkForUpdates} JSON-RPC request. Carries no data; it is a
 * trigger for the {@code checkForPositAssistantUpdates} command flow.
 */
public class ChatCheckForUpdatesEvent extends GwtEvent<ChatCheckForUpdatesEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChatCheckForUpdates(ChatCheckForUpdatesEvent event);
   }

   public ChatCheckForUpdatesEvent()
   {
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChatCheckForUpdates(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
