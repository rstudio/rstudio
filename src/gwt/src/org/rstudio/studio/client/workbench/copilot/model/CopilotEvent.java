/*
 * CopilotEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.copilot.model;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CopilotEvent extends GwtEvent<CopilotEvent.Handler>
{
   public static enum CopilotEventType
   {
      COPILOT_DISABLED,
      COMPLETION_REQUESTED,
      COMPLETION_RECEIVED_SOME,
      COMPLETION_RECEIVED_NONE,
      COMPLETION_CANCELLED,
      COMPLETION_ERROR,
      COMPLETIONS_ENABLED,
      COMPLETIONS_DISABLED,
   }
   
   public CopilotEvent(CopilotEventType type, Object data)
   {
      type_ = type;
      data_ = data;
   }
   
   public CopilotEvent(CopilotEventType type)
   {
      this(type, null);
   }
   
   public CopilotEventType getType()
   {
      return type_;
   }
   
   public Object getData()
   {
      return data_;
   }
   
   private final CopilotEventType type_;
   private final Object data_;

   // Boilerplate ----
   public interface Handler extends EventHandler
   {
      void onCopilot(CopilotEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCopilot(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}

