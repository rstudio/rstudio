/*
 * ScrollYEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class ScrollYEvent extends GwtEvent<ScrollYEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onScrollY(ScrollYEvent event);
   }

   public ScrollYEvent(Position position)
   {
      position_ = position;
   }

   public Position getPosition()
   {
      return position_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onScrollY(this);
   }

   public final static Type<Handler> TYPE = new Type<>();
   private final Position position_;
}
