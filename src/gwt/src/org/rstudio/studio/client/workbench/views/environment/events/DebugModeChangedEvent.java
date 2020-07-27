/*
 * DebugModeChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DebugModeChangedEvent
        extends GwtEvent<DebugModeChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onDebugModeChanged(DebugModeChangedEvent event);
   }

   public DebugModeChangedEvent(boolean debugging)
   {
      debugging_ = debugging;
   }

   public boolean debugging()
   {
      return debugging_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDebugModeChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
   private final boolean debugging_;
}
