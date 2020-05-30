/*
 * RSConnectDeploymentFailedEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RSConnectDeploymentFailedEvent extends GwtEvent<RSConnectDeploymentFailedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectDeploymentFailed(RSConnectDeploymentFailedEvent event);
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getPath() /*-{
         return this.path;
      }-*/;

      public final native String getHttpStatus() /*-{
         return this.http_status;
      }-*/;
   }

   public static final GwtEvent.Type<RSConnectDeploymentFailedEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectDeploymentFailedEvent.Handler>();
   
   public RSConnectDeploymentFailedEvent(Data data)
   {
      data_ = data;
   }
   
   public Data getData()
   {
      return data_;
   }
   
   @Override
   protected void dispatch(RSConnectDeploymentFailedEvent.Handler handler)
   {
      handler.onRSConnectDeploymentFailed(this);
   }

   @Override
   public GwtEvent.Type<RSConnectDeploymentFailedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final Data data_;
}
