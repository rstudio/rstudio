/*
 * ZoomLevelChangedEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.core.client.Debug;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ZoomLevelChangedEvent extends GwtEvent<ZoomLevelChangedEvent.Handler>
{
   public final static int ZOOM_IN  = 0;
   public final static int ZOOM_OUT = 1;
   
   public interface Handler extends EventHandler
   {
      void onZoomLevelChanged(ZoomLevelChangedEvent event);
   }

   public ZoomLevelChangedEvent(int direction)
   {
      Debug.devlog("zoom detect: " + direction);
      direction_ = direction;
   }
   
   public int direction()
   {
      return direction_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onZoomLevelChanged(this);
   }
   
   private final int direction_; 
   public static final Type<Handler> TYPE = new Type<Handler>();
}