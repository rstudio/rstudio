/*
 * MouseNavigateSourceHistoryEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// This event is primarily used to paper over issues with QtWebEngine;
// it appears that Qt 5.12.x does not propagate side mouse buttons to the
// associated WebEngine view, and so we are instead forced to synthesize
// a synthetic navigation event that gets handled specially on the GWT side.
//
// https://github.com/rstudio/rstudio/pull/7272
public class MouseNavigateEvent extends GwtEvent<MouseNavigateEvent.Handler>
{
   public MouseNavigateEvent(boolean forward, int mouseX, int mouseY)
   {
      forward_ = forward;
      mouseX_ = mouseX;
      mouseY_ = mouseY;
   }

   public interface Handler extends EventHandler
   {
      void onMouseNavigate(MouseNavigateEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onMouseNavigate(this);
   }

   public boolean getForward()
   {
      return forward_;
   }

   public int getMouseX()
   {
      return mouseX_;
   }

   public int getMouseY()
   {
      return mouseY_;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final boolean forward_;
   private final int mouseX_;
   private final int mouseY_;
}
