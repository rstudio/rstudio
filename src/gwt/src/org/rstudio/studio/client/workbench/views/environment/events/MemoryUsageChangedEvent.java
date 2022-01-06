/*
 * MemoryUsageChangedEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;

public class MemoryUsageChangedEvent extends GwtEvent<MemoryUsageChangedEvent.Handler>
{
   public MemoryUsageChangedEvent(MemoryUsage data)
   {
      usage_ = data;
   }

   public MemoryUsage getMemoryUsage()
   {
      return usage_;
   }

   private final MemoryUsage usage_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onMemoryUsageChanged(MemoryUsageChangedEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onMemoryUsageChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}

