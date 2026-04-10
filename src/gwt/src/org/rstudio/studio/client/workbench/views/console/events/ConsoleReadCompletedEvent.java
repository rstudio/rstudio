/*
 * ConsoleReadCompletedEvent.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ConsoleReadCompletedEvent extends GwtEvent<ConsoleReadCompletedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onConsoleReadCompleted(ConsoleReadCompletedEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final boolean getHistory() /*-{
         return this.history;
      }-*/;
   }

   public ConsoleReadCompletedEvent(boolean history)
   {
      history_ = history;
   }

   public boolean getHistory()
   {
      return history_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onConsoleReadCompleted(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final boolean history_;
}
