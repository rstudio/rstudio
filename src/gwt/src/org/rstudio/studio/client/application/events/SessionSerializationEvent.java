/*
 * SessionSerializationEvent.java
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

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;

public class SessionSerializationEvent extends GwtEvent<SessionSerializationHandler>
{   
   public static final GwtEvent.Type<SessionSerializationHandler> TYPE =
      new GwtEvent.Type<SessionSerializationHandler>();
   
   public SessionSerializationEvent(SessionSerializationAction action)
   {
      action_ = action;
   }
   
   public SessionSerializationAction getAction()
   {
      return action_;
   }
   
   @Override
   protected void dispatch(SessionSerializationHandler handler)
   {
      handler.onSessionSerialization(this);
   }

   @Override
   public GwtEvent.Type<SessionSerializationHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   
   private SessionSerializationAction action_;
}
