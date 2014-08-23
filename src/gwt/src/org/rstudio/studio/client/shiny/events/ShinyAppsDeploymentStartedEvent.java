/*
 * ShinyAppsDeploymentStartedEvent.java
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

public class ShinyAppsDeploymentStartedEvent extends GwtEvent<ShinyAppsDeploymentStartedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onShinyAppsDeploymentStarted(ShinyAppsDeploymentStartedEvent event);
   }

   public static final GwtEvent.Type<ShinyAppsDeploymentStartedEvent.Handler> TYPE =
      new GwtEvent.Type<ShinyAppsDeploymentStartedEvent.Handler>();
   
   public ShinyAppsDeploymentStartedEvent(String path)
   {
      path_ = path;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   @Override
   protected void dispatch(ShinyAppsDeploymentStartedEvent.Handler handler)
   {
      handler.onShinyAppsDeploymentStarted(this);
   }

   @Override
   public GwtEvent.Type<ShinyAppsDeploymentStartedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String path_;
}