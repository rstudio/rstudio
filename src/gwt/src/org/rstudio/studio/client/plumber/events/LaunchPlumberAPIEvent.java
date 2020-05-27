/*
 * LaunchPlumberAPIEvent.java
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
package org.rstudio.studio.client.plumber.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class LaunchPlumberAPIEvent 
   extends CrossWindowEvent<LaunchPlumberAPIEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onLaunchPlumberAPI(LaunchPlumberAPIEvent event);
   }

   public static final GwtEvent.Type<LaunchPlumberAPIEvent.Handler> TYPE =
      new GwtEvent.Type<LaunchPlumberAPIEvent.Handler>();
   
   public LaunchPlumberAPIEvent()
   {
   }
   
   public LaunchPlumberAPIEvent(String path)
   {
      path_ = path;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   @Override
   protected void dispatch(LaunchPlumberAPIEvent.Handler handler)
   {
      handler.onLaunchPlumberAPI(this);
   }

   @Override
   public GwtEvent.Type<LaunchPlumberAPIEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String path_;
}
