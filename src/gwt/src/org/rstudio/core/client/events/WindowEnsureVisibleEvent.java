/*
 * WindowEnsureVisibleEvent.java
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
package org.rstudio.core.client.events;

import org.rstudio.core.client.theme.WindowFrame;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class WindowEnsureVisibleEvent extends GwtEvent<WindowEnsureVisibleEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onWindowEnsureVisible(WindowEnsureVisibleEvent event);
   }

   public WindowEnsureVisibleEvent(WindowFrame windowFrame)
   {
      windowFrame_ = windowFrame;
   }

   public WindowFrame getWindowFrame()
   {
      return windowFrame_;
   }

   private final WindowFrame windowFrame_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onWindowEnsureVisible(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
