/*
 * RSConnectDeploymentCompletedEvent.java
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

public class RSConnectDeploymentCompletedEvent extends GwtEvent<RSConnectDeploymentCompletedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectDeploymentCompleted(RSConnectDeploymentCompletedEvent event);
   }

   public static final GwtEvent.Type<RSConnectDeploymentCompletedEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectDeploymentCompletedEvent.Handler>();
   
   public RSConnectDeploymentCompletedEvent(String url)
   {
      url_ = url;
   }
   
   public String getUrl()
   {
      return url_;
   }
   
   public boolean succeeded()
   {
      return url_ != null && url_.length() > 0;
   }
   
   @Override
   protected void dispatch(RSConnectDeploymentCompletedEvent.Handler handler)
   {
      handler.onRSConnectDeploymentCompleted(this);
   }

   @Override
   public GwtEvent.Type<RSConnectDeploymentCompletedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String url_;
}
