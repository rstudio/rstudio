/*
 * SlideIndexChangedEvent.java
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
package org.rstudio.studio.client.common.presentation.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SlideIndexChangedEvent extends GwtEvent<SlideIndexChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSlideIndexChanged(SlideIndexChangedEvent event);
   }

   public SlideIndexChangedEvent(int index)
   {
      index_ = index;
   }

   public int getIndex()
   {
      return index_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSlideIndexChanged(this);
   }

   private final int index_;

   public static final Type<Handler> TYPE = new Type<>();
}
