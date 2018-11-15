/*
 * SessionRelaunchEvent.java
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

public class SessionRelaunchEvent extends GwtEvent<SessionRelaunchHandler>
{
   public static final GwtEvent.Type<SessionRelaunchHandler> TYPE =
      new GwtEvent.Type<SessionRelaunchHandler>();

   public enum Type
   {
      RELAUNCH_INITIATED,
      RELAUNCH_COMPLETE
   }

   public SessionRelaunchEvent(Type type)
   {
      type_ = type;
   }

   public Type getType()
   {
      return type_;
   }

   @Override
   protected void dispatch(SessionRelaunchHandler handler)
   {
      handler.onSessionRelaunch(this);
   }

   @Override
   public GwtEvent.Type<SessionRelaunchHandler> getAssociatedType()
   {
      return TYPE;
   }

   private final Type type_;
}
