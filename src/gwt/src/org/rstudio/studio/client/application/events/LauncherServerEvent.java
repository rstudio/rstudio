/*
 * LauncherServerEvent.java
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

import com.google.gwt.event.shared.GwtEvent;

public class LauncherServerEvent extends GwtEvent<LauncherServerHandler>
{
   public enum EventType
   {
      ServerConnectSucceeded,
      ServerConnectFailed,
      JobStatusStreamFailure,
      JobOutputStreamFailure,
      JobControlSuccess,
      JobControlFailure,
      JobsEnabled,
      JobsDisabled,
      JobSubmitSuccess,
      JobSubmitFailure,
      GetContainerUserSuccess,
      GetContainerUserFailure,
      LauncherServerSettingsChanged
   }

   public LauncherServerEvent(EventType eventType, String details)
   {
      eventType_ = eventType;
      details_ = details;
   }

   public static final GwtEvent.Type<LauncherServerHandler> TYPE = new GwtEvent.Type<>();
   
   @Override
   protected void dispatch(LauncherServerHandler handler)
   {
      handler.onLauncherServerEvent(this);
   }

   @Override
   public GwtEvent.Type<LauncherServerHandler> getAssociatedType()
   {
      return TYPE;
   }

   public EventType eventType()
   {
      return eventType_;
   }
   public String details() { return details_; }

   private EventType eventType_;
   private String details_;
}
