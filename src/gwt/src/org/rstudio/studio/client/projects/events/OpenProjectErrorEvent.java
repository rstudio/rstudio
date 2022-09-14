/*
 * OpenProjectErrorEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.projects.model.OpenProjectError;

import com.google.gwt.event.shared.GwtEvent;

public class OpenProjectErrorEvent extends GwtEvent<OpenProjectErrorEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenProjectError(OpenProjectErrorEvent event);
   }

   public OpenProjectErrorEvent(OpenProjectError error)
   {
      error_ = error;
   }

   public String getProject()
   {
      return error_.getProject();
   }

   public String getMessage()
   {
      return error_.getMessage();
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenProjectError(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final OpenProjectError error_;
}
