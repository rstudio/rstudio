/*
 * AriaLiveAnnouncementEvent.java
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

public class AriaLiveAnnouncementEvent extends GwtEvent<AriaLiveAnnouncementEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAriaLiveAnnoucement(AriaLiveAnnouncementEvent event);
   }

   public AriaLiveAnnouncementEvent(boolean assertive, String message)
   {
      assertive_ = assertive;
      message_ = message;
   }

   public boolean getAssertive()
   {
      return assertive_;
   }

   public String getMessage()
   {
      return message_;
   }

   @Override
   protected void dispatch(AriaLiveAnnouncementEvent.Handler handler)
   {
      handler.onAriaLiveAnnoucement(this);
   }

   @Override
   public Type<AriaLiveAnnouncementEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final boolean assertive_;
   private final String message_;

   public static final Type<AriaLiveAnnouncementEvent.Handler> TYPE = new Type<>();
}
