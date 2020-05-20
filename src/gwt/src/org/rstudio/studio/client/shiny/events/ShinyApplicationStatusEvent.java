/*
 * ShinyApplicationStatusEvent.java
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
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class ShinyApplicationStatusEvent 
   extends CrossWindowEvent<ShinyApplicationStatusEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onShinyApplicationStatus(ShinyApplicationStatusEvent event);
   }

   public static final GwtEvent.Type<ShinyApplicationStatusEvent.Handler> TYPE =
      new GwtEvent.Type<ShinyApplicationStatusEvent.Handler>();
   
   public ShinyApplicationStatusEvent()
   {
   }
   
   public ShinyApplicationStatusEvent(ShinyApplicationParams params)
   {
      this(params, false);
   }

   public ShinyApplicationStatusEvent(ShinyApplicationParams params,
         boolean serverInitiated)
   {
      params_ = params;
      serverInitiated_ = serverInitiated;
   }
   
   public ShinyApplicationParams getParams()
   {
      return params_;
   }
   
   public boolean isServerInitiated()
   {
      return serverInitiated_;
   }
   
   @Override
   public boolean forward()
   {
      return false;
   }
   
   @Override
   protected void dispatch(ShinyApplicationStatusEvent.Handler handler)
   {
      handler.onShinyApplicationStatus(this);
   }

   @Override
   public GwtEvent.Type<ShinyApplicationStatusEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private ShinyApplicationParams params_;
   private boolean serverInitiated_;
}
