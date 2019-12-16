/*
 * AriaLiveStatusEvent.java
 *
 * Copyright (C) 2019 by RStudio, Inc.
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AriaLiveStatusEvent extends GwtEvent<AriaLiveStatusEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAriaLiveStatus(AriaLiveStatusEvent event);
   }

   public AriaLiveStatusEvent(String message)
   {
      message_ = message;
   }

   public String getMessage()
   {
      return message_;
   }

   @Override
   protected void dispatch(AriaLiveStatusEvent.Handler handler)
   {
      handler.onAriaLiveStatus(this);
   }

   @Override
   public Type<AriaLiveStatusEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final String message_;

   public static final Type<AriaLiveStatusEvent.Handler> TYPE = new Type<>();
}
