/*
 * NativeKeyPressEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.events;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class NativeKeyPressEvent extends GwtEvent<NativeKeyPressHandler>
{
   public static final GwtEvent.Type<NativeKeyPressHandler> TYPE =
      new GwtEvent.Type<NativeKeyPressHandler>();

   protected NativeKeyPressEvent(NativeEvent event)
   {
      event_ = event;
   }

   public NativeEvent getEvent()
   {
      return event_;
   }

   public boolean isCanceled()
   {
      return handled_;
   }

   public void cancel()
   {
      handled_ = true;
   }

   public char getCharCode()
   {
      return getCharCode(event_);
   }

   public static boolean fire(NativeEvent event, HasHandlers target)
   {
      NativeKeyPressEvent evt = new NativeKeyPressEvent(event);
      target.fireEvent(evt);
      if (evt.isCanceled())
      {
         event.preventDefault();
         return true;
      }
      return false;
   }

   @Override
   public Type<NativeKeyPressHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(NativeKeyPressHandler handler)
   {
      handler.onKeyPress(this);
   }

  private native char getCharCode(NativeEvent e)/*-{
    return e.charCode || e.keyCode;
  }-*/;

   private final NativeEvent event_;
   private boolean handled_;
}
