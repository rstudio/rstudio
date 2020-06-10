/*
 * RSConnectDeploymentStartedEvent.java
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
package org.rstudio.studio.client.rsconnect.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RSConnectDeploymentStartedEvent extends GwtEvent<RSConnectDeploymentStartedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectDeploymentStarted(RSConnectDeploymentStartedEvent event);
   }

   public static final GwtEvent.Type<RSConnectDeploymentStartedEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectDeploymentStartedEvent.Handler>();
   
   public RSConnectDeploymentStartedEvent(String path, String title)
   {
      path_ = path;
      title_ = title;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   public String getTitle()
   {
      return title_;
   }
   
   @Override
   protected void dispatch(RSConnectDeploymentStartedEvent.Handler handler)
   {
      handler.onRSConnectDeploymentStarted(this);
   }

   @Override
   public GwtEvent.Type<RSConnectDeploymentStartedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String path_;
   private final String title_;
}
