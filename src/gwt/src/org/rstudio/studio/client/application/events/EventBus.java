/*
 * EventBus.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Singleton;

@Singleton
public class EventBus extends HandlerManager
{
   public EventBus()
   {
      super(null) ;
   }

   /**
    * Similar to 2-arg form of addHandler, but automatically removes handler
    * when the HasAttachHandlers object detaches.
    *
    * If the HasAttachHandlers object detaches and reattaches, the handler
    * will NOT automatically resubscribe.
    */
   public <H extends EventHandler> void addHandler(
         HasAttachHandlers removeWhenDetached, Type<H> type, H handler)
   {
      final HandlerRegistration reg = addHandler(type, handler);
      removeWhenDetached.addAttachHandler(new Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               reg.removeHandler();
         }
      });
   }
}
