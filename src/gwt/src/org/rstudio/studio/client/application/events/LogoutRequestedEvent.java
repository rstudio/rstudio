/*
 * LogoutRequestedEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.GwtEvent;

public class LogoutRequestedEvent extends GwtEvent<LogoutRequestedHandler>
{
   public static final GwtEvent.Type<LogoutRequestedHandler> TYPE =
      new GwtEvent.Type<LogoutRequestedHandler>();
   
   @Override
   protected void dispatch(LogoutRequestedHandler handler)
   {
      handler.onLogoutRequested(this);
   }

   @Override
   public GwtEvent.Type<LogoutRequestedHandler> getAssociatedType()
   {
      return TYPE;
   }
}