/*
 * ChatSuspendedNoticeEvent.java
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

/**
 * Sent from the main window to the popped-out chat window when the session
 * suspends, with the notice to show until it resumes. The main window picks
 * the text because only it knows whether the suspend is part of Restart R.
 */
@JavaScriptSerializable
public class ChatSuspendedNoticeEvent
   extends CrossWindowEvent<ChatSuspendedNoticeEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChatSuspendedNotice(ChatSuspendedNoticeEvent event);
   }

   public ChatSuspendedNoticeEvent()
   {
      notice_ = "";
   }

   public ChatSuspendedNoticeEvent(String notice)
   {
      notice_ = notice;
   }

   public String getNotice()
   {
      return notice_;
   }

   @Override
   public boolean forward()
   {
      return false;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChatSuspendedNotice(this);
   }

   private String notice_;

   public static final Type<Handler> TYPE = new Type<>();
}
