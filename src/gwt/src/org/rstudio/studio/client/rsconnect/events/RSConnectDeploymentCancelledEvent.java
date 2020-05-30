/*
 * RSConnectDeploymentCancelledEvent.java
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

public class RSConnectDeploymentCancelledEvent extends GwtEvent<RSConnectDeploymentCancelledEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectDeploymentCancelled(RSConnectDeploymentCancelledEvent event);
   }

   public static final GwtEvent.Type<RSConnectDeploymentCancelledEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectDeploymentCancelledEvent.Handler>();
   
   public RSConnectDeploymentCancelledEvent()
   {
   }
   
   @Override
   protected void dispatch(RSConnectDeploymentCancelledEvent.Handler handler)
   {
      handler.onRSConnectDeploymentCancelled(this);
   }

   @Override
   public GwtEvent.Type<RSConnectDeploymentCancelledEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
}
