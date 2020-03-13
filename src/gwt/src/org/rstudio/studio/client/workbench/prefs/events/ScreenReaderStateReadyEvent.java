/*
 * ScreenReaderStateReadyEvent.java
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
package org.rstudio.studio.client.workbench.prefs.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Loading the Screen reader availability flag happens in a deferred fashion (due to it
 * requiring an async call on desktop IDE). This event is fired when that state has been
 * loaded and is ready to be queried via UserPrefs.getScreenReaderEnabled()
 */
public class ScreenReaderStateReadyEvent extends GwtEvent<ScreenReaderStateReadyEvent.Handler>
{
   public static final Type<ScreenReaderStateReadyEvent.Handler> TYPE = new Type<>();

   public ScreenReaderStateReadyEvent()
   {
   }

   public interface Handler extends EventHandler
   {
      void onScreenReaderStateReady(ScreenReaderStateReadyEvent e);
   }

   @Override
   public Type<ScreenReaderStateReadyEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onScreenReaderStateReady(this);
   }
}
