/*
 * SatelliteFocusedEvent.java
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
package org.rstudio.studio.client.common.satellite.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class SatelliteFocusedEvent
             extends CrossWindowEvent<SatelliteFocusedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSatelliteFocused(SatelliteFocusedEvent event);
   }

   public SatelliteFocusedEvent()
   {
   }

   public SatelliteFocusedEvent(String name)
   {
      name_ = name;
   }

   public String getName()
   {
      return name_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSatelliteFocused(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private String name_;
}
