/*
 * ChatBackendExitEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChatBackendExitEvent extends GwtEvent<ChatBackendExitEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onChatBackendExit(ChatBackendExitEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final int getExitCode() /*-{
         return this.exit_code;
      }-*/;

      public native final boolean getCrashed() /*-{
         return this.crashed;
      }-*/;
   }

   public ChatBackendExitEvent(int exitCode, boolean crashed)
   {
      exitCode_ = exitCode;
      crashed_ = crashed;
   }

   public int getExitCode()
   {
      return exitCode_;
   }

   public boolean getCrashed()
   {
      return crashed_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChatBackendExit(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final int exitCode_;
   private final boolean crashed_;
}
