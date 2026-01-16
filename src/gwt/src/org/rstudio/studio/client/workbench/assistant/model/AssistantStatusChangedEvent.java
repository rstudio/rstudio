/*
 * AssistantStatusChangedEvent.java
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
package org.rstudio.studio.client.workbench.assistant.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AssistantStatusChangedEvent extends GwtEvent<AssistantStatusChangedEvent.Handler>
{
   // keep in sync with CopilotAgentRuntimeStatus in SessionCopilot.cpp
   public static final int UNKNOWN = 0;
   public static final int PREPARING = 1;
   public static final int STARTING = 2;
   public static final int RUNNING = 3;
   public static final int STOPPING = 4;
   public static final int STOPPED = 5;

   public interface Handler extends EventHandler
   {
      void onAssistantStatusChangedEvent(AssistantStatusChangedEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final int getStatus() /*-{
         return this.status;
      }-*/;
   }

   public AssistantStatusChangedEvent(int status)
   {
      status_ = status;
   }

   public int getStatus()
   {
      return status_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAssistantStatusChangedEvent(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final int status_;
}
