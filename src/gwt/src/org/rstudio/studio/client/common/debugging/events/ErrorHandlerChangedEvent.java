/*
 * ErrorHandlerChangedEvent.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.common.debugging.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.core.client.JavaScriptObject;

public class ErrorHandlerChangedEvent
        extends GwtEvent<ErrorHandlerChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onErrorHandlerChanged(ErrorHandlerChangedEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getType() /*-{
         return this.type;
      }-*/;

   }

   public ErrorHandlerChangedEvent(Data type)
   {
      type_ = type;
   }

   public String getHandlerType()
   {
      return type_.getType();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onErrorHandlerChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private Data type_;
}
