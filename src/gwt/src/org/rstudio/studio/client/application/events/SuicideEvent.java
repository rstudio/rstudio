/*
 * SuicideEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;


public class SuicideEvent extends GwtEvent<SuicideEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public SuicideEvent(String message)
   {
      message_ = message;
   }

   public String getMessage()
   {
      return message_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSuicide(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private String message_;

   public interface Handler extends EventHandler
   {
      void onSuicide(SuicideEvent event);
   }
}
