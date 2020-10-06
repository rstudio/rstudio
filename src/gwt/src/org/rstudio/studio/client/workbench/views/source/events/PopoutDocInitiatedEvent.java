/*
 * PopoutDocInitiatedEvent.java
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

import org.rstudio.core.client.Point;
import org.rstudio.core.client.js.JavaScriptSerializable;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class PopoutDocInitiatedEvent
             extends GwtEvent<PopoutDocInitiatedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onPopoutDocInitiated(PopoutDocInitiatedEvent e);
   }

   public PopoutDocInitiatedEvent()
   {
      posX_ = 0;
      posY_ = 0;
   }

   public PopoutDocInitiatedEvent(String docId, Point position)
   {
      docId_ = docId;
      if (position != null)
      {
         posX_ = position.getX();
         posY_ = position.getY();
      }
   }

   public String getDocId()
   {
      return docId_;
   }

   public Point getPosition()
   {
      if (posX_ == 0 && posY_ == 0)
         return null;
      return Point.create(posX_, posY_);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPopoutDocInitiated(this);
   }

   private String docId_;
   private int posX_;
   private int posY_;

   public static final Type<Handler> TYPE = new Type<>();
}
