/*
 * CrossWindowEvent.java
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// A CrossWindowEvent can be fired from the main window to a satellite, or vice
// versa. Note that CrossWindowEvents should be annotated with 
// @JavaScriptSerializable so that they can be appropriately marshaled across
// the window boundary.
public abstract class CrossWindowEvent<T extends EventHandler> extends GwtEvent<T>
{
   public CrossWindowEvent()
   {
      setOriginWindowName(RStudioGinjector.INSTANCE.getSatellite().getSatelliteName());
   }

   // Whether the event should be forwarded to the main window by default 
   public boolean forward()
   {
      return true;
   }

   // The focus behavior for the event
   public int focusMode()
   {
      return MODE_BACKGROUND;
   }

   public boolean isFromMainWindow()
   {
      return StringUtil.isNullOrEmpty(originWindowName_);
   }

   public String originWindowName()
   {
      return originWindowName_;
   }

   public void setOriginWindowName(String originWindow)
   {
      originWindowName_ = originWindow;
   }

   private String originWindowName_;

   // this event is processed in the background; don't raise the window
   public static final int MODE_BACKGROUND = 0;

   // this event does auxiliary processing; make the window visible if possible
   // but don't give it focus (affects desktop only)
   public static final int MODE_AUXILIARY = 1;

   // this event makes the window interactive; give it focus
   public static final int MODE_FOCUS = 2;
}
