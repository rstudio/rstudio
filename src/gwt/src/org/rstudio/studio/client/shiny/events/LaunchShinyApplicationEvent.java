/*
 * LaunchShinyApplicationEvent.java
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
package org.rstudio.studio.client.shiny.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class LaunchShinyApplicationEvent 
   extends CrossWindowEvent<LaunchShinyApplicationEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onLaunchShinyApplication(LaunchShinyApplicationEvent event);
   }

   public static final GwtEvent.Type<LaunchShinyApplicationEvent.Handler> TYPE =
      new GwtEvent.Type<LaunchShinyApplicationEvent.Handler>();
   
   public LaunchShinyApplicationEvent()
   {
   }
   
   public LaunchShinyApplicationEvent(String path, String destination, String extendedType)
   {
      path_ = path;
      destination_ = destination;
      extendedType_ = extendedType;
   }

   
   public String getPath()
   {
      return path_;
   }
   
   public String getExtendedType()
   {
      return extendedType_;
   }
   
   public String getDestination()
   {
      return destination_;
   }
   
   @Override
   protected void dispatch(LaunchShinyApplicationEvent.Handler handler)
   {
      handler.onLaunchShinyApplication(this);
   }

   @Override
   public GwtEvent.Type<LaunchShinyApplicationEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String path_;
   private String destination_;
   private String extendedType_;
}
