/*
 * SessionRelaunchEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SessionRelaunchEvent extends GwtEvent<SessionRelaunchEvent.Handler>
{
   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   public enum Type
   {
      RELAUNCH_INITIATED,
      RELAUNCH_COMPLETE
   }

   public SessionRelaunchEvent(Type type)
   {
      type_ = type;
      redirectUrl_ = "";
   }

   public SessionRelaunchEvent(Type type, String redirectUrl)
   {
      type_ = type;
      redirectUrl_ = redirectUrl;
   }

   public Type getType()
   {
      return type_;
   }
   public String getRedirectUrl() { return redirectUrl_; }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSessionRelaunch(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Type type_;
   private final String redirectUrl_;

   public interface Handler extends EventHandler
   {
      void onSessionRelaunch(SessionRelaunchEvent event);
   }
}
