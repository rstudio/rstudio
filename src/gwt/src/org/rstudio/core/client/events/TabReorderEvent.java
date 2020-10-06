/*
 * TabReorderEvent.java
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class TabReorderEvent extends GwtEvent<TabReorderEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public TabReorderEvent(int oldPos, int newPos)
   {
      oldPos_ = oldPos;
      newPos_ = newPos;
   }

   public int getNewPos()
   {
      return newPos_;
   }

   public int getOldPos()
   {
      return oldPos_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTabReorder(this);
   }

   public interface Handler extends EventHandler
   {
      void onTabReorder(TabReorderEvent event);
   }

   private int oldPos_;
   private int newPos_;
}
