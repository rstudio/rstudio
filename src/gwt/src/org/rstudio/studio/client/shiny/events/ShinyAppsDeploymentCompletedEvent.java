/*
 * ShinyAppsDeploymentCompletedEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShinyAppsDeploymentCompletedEvent extends GwtEvent<ShinyAppsDeploymentCompletedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onShinyAppsDeploymentCompleted(ShinyAppsDeploymentCompletedEvent event);
   }

   public static final GwtEvent.Type<ShinyAppsDeploymentCompletedEvent.Handler> TYPE =
      new GwtEvent.Type<ShinyAppsDeploymentCompletedEvent.Handler>();
   
   public ShinyAppsDeploymentCompletedEvent(String url)
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
   protected void dispatch(ShinyAppsDeploymentCompletedEvent.Handler handler)
   {
      handler.onShinyAppsDeploymentCompleted(this);
   }

   @Override
   public GwtEvent.Type<ShinyAppsDeploymentCompletedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String url_;
}