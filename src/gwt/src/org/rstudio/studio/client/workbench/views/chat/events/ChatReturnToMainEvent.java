/*
 * ChatReturnToMainEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ChatReturnToMainEvent
   extends CrossWindowEvent<ChatReturnToMainEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChatReturnToMain(ChatReturnToMainEvent event);
   }

   public ChatReturnToMainEvent()
   {
   }

   @Override
   public boolean forward()
   {
      return true;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChatReturnToMain(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
